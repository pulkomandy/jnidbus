package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.cache.MessageMetadata;
import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.exception.SerializationException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * The ComplexArraySerializer takes care of nested arrays, arrays that contain objects and arrays that contains map.
 * Its role is to find through the field generic type which serializer to use.
 */
public class ComplexArraySerializer extends Serializer{
    //signature of the value (used to set the signature on child DBusObjects)
    private SignatureElement valueSignature;

    //are we dealing with a primitive array (ex: int[][])
    private boolean isPrimitiveArray;

    //delegate serializer used to serialize the array values
    private Serializer nestedSerializer;

    //expected class of the array values
    private Class expectedValueType;

    /**
     *
     * @param genericType generic type of the field
     * @param signature signature of the element
     * @param managedClass class managed by this serializer
     * @param managedFieldName field managed by this serializer
     * @throws MessageCheckException
     */
    public ComplexArraySerializer(Type genericType, SignatureElement signature, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signature, managedClass, managedFieldName);
        //extract the value signature
        SignatureElement valueType = signature.getSignature().getFirst();
        this.valueSignature = valueType;

        //get the array class, if it is generic
        Class<?> rawType;
        if(genericType instanceof ParameterizedType){
            rawType = (Class<?>) ((ParameterizedType)genericType).getRawType();
        }else{
            rawType = (Class<?>) genericType;
        }
        this.isPrimitiveArray = rawType.isArray();

        //get the value class
        Class<?> arrayValueClass;
        if(this.isPrimitiveArray){
            //primitive arrays have a method for that
            arrayValueClass = rawType.getComponentType();
        }else{
            //get the generic type, if the resulting type is also generic, get its raw type
            Type listType = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if(listType instanceof ParameterizedType){
                arrayValueClass = (Class<?>) ((ParameterizedType)listType).getRawType();
            }else{
                arrayValueClass = (Class<?>) listType;
            }
        }
        this.expectedValueType = arrayValueClass;

        //check that the array is primitive or a List
        if(!this.isPrimitiveArray && !List.class.isAssignableFrom(rawType)){
            throw new MessageCheckException("An array must be a Java primitive array or a List",this.managedClass,this.managedFieldName);
        }

        if(valueType.getPrimitive() == null){
            //we have another container type
            if(valueType.getContainerType() == SupportedTypes.OBJECT_BEGIN){
                //if we have objects, check they are Serializable
                if(!Serializable.class.isAssignableFrom(arrayValueClass)){
                    throw new MessageCheckException("The array contain non-serializable values",managedClass,managedFieldName);
                }
                Class<? extends Serializable> nestedObjectClass = arrayValueClass.asSubclass(Serializable.class);
                this.nestedSerializer = new ObjectSerializer(nestedObjectClass,valueSignature, this.managedClass, this.managedFieldName);
            }else if (valueType.getContainerType() == SupportedTypes.ARRAY){
                //for primitive arrays, use the value class, else we have to get the generic type class
                if(this.isPrimitiveArray){
                    //if this array contains another arrays containing dict_entries, use the Map Serialiazer
                    if(valueType.getSignature().getFirst().isDictEntry()){
                        this.nestedSerializer = new MapSerializer(arrayValueClass,valueSignature, this.managedClass, this.managedFieldName);
                    }else{
                        this.nestedSerializer = new ComplexArraySerializer(arrayValueClass,valueSignature, this.managedClass, this.managedFieldName);
                    }
                }else{
                    if(valueType.getSignature().getFirst().isDictEntry()){
                        this.nestedSerializer = new MapSerializer(((ParameterizedType)genericType).getActualTypeArguments()[0],valueSignature,this.managedClass,this.managedFieldName);
                    }else{
                        this.nestedSerializer = new ComplexArraySerializer(((ParameterizedType)genericType).getActualTypeArguments()[0],valueSignature,this.managedClass,this.managedFieldName);
                    }
                }
            }else{
                //if another container is found, this serializer should not be the one processing it
                throw new MessageCheckException("The ComplexArraySerializer can not process the item value"+valueType,managedClass,managedFieldName);
            }
        }else{
            //if we have a primitive, it means this array contains arrays of primitive type
            if(this.isPrimitiveArray){
                this.nestedSerializer = new PrimitiveArraySerializer(arrayValueClass,valueSignature,this.managedClass,this.managedFieldName);
            }else{
                this.nestedSerializer = new PrimitiveArraySerializer(((ParameterizedType)genericType).getActualTypeArguments()[0],valueSignature,this.managedClass,this.managedFieldName);
            }
        }
    }

    @Override
    public Object serialize(Object value){
        Object returned;
        //if the expected value is a primitive array, try to keep its value unboxed
        if(isPrimitiveArray){
            returned = Array.newInstance(this.expectedValueType,Array.getLength(value));
        }else{
            returned = new Object[((List) value).size()];
        }

        //if dealing with a primitive array, we might have created an unboxed array, which do not have proper types so we
        //use Array.set to set values on unboxed arrays
        if(this.isPrimitiveArray){
            int i = 0;
            for(Object s : (Object[]) value) {
                Array.set(returned,i++,this.nestedSerializer.serialize(s));
            }
        }else{
            int i = 0;
            for(Object s : (List) value) {
                ((Object[])returned)[i++] = this.nestedSerializer.serialize(s);
            }
        }

        return returned;
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        boolean isDBusObject = value instanceof DBusObject[];
        Object[] values = (Object[]) value;

        if(this.isPrimitiveArray){
            int i = 0;
            Object returned = Array.newInstance(this.expectedValueType,values.length);
            for(Object s : values){
                //if this complex array is an array of dbusobject, we should set the signature of each value as the JNI code
                //do not set it
                if(isDBusObject) s = new DBusObject(this.valueSignature.getSignatureString(), ((DBusObject)s).getValues());
                Array.set(returned,i++,this.nestedSerializer.deserialize(s));
            }
            return returned;
        }else{
            List returned = new ArrayList();
            for(Object s : values){
                //if this complex array is an array of dbusobject, we should set the signature of each value as the JNI code
                //do not set it
                if(isDBusObject) s = new DBusObject(this.valueSignature.getSignatureString(), ((DBusObject)s).getValues());
                returned.add(this.nestedSerializer.deserialize(s));
            }
            return returned;
        }
    }
}
