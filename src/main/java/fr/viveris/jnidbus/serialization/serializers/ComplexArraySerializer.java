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

public class ComplexArraySerializer extends Serializer{
    private SignatureElement valueSignature;
    private boolean isPrimitiveArray;
    private Serializer nestedSerializer;

    //nullable, used only if the array contains objects
    private Class<? extends Serializable> objectClass;

    public ComplexArraySerializer(Type genericType, SignatureElement signature, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signature, managedClass, managedFieldName);
        SignatureElement valueType = signature.getSignature().getFirst();
        this.valueSignature = valueType;

        Class<?> rawType;
        if(genericType instanceof ParameterizedType){
            rawType = (Class<?>) ((ParameterizedType)genericType).getRawType();
        }else{
            rawType = (Class<?>) genericType;
        }

        this.isPrimitiveArray = rawType.isArray();

        Class<?> arrayValueClass;
        if(this.isPrimitiveArray){
            arrayValueClass = rawType.getComponentType();
        }else{
            Type listType = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if(listType instanceof ParameterizedType){
                arrayValueClass = (Class<?>) ((ParameterizedType)listType).getRawType();
            }else{
                arrayValueClass = (Class<?>) listType;
            }
        }

        if(!this.isPrimitiveArray && !List.class.isAssignableFrom(rawType)){
            throw new MessageCheckException("An array must be a Java primitive array or a List",this.managedClass,this.managedFieldName);
        }

        if(valueType.getPrimitive() == null){
            //we have another nested array, use the complex serializer, If the nested value is an object, try to
            //add it to cache
            if(valueType.getContainerType() == SupportedTypes.OBJECT_BEGIN){
                Class<? extends Serializable> nestedObjectClass;
                if(this.isPrimitiveArray){
                    nestedObjectClass = ((Class)genericType).asSubclass(Serializable.class);
                }else{
                    nestedObjectClass = ((Class) ((ParameterizedType)genericType).getActualTypeArguments()[0]).asSubclass(Serializable.class);
                }
                MessageMetadata testedEntity = new MessageMetadata(nestedObjectClass);
                Message.addToCache(nestedObjectClass,testedEntity);
                this.nestedSerializer = new ObjectSerializer(nestedObjectClass,valueSignature, this.managedClass, this.managedFieldName);
            }else{
                if(this.isPrimitiveArray){
                    this.nestedSerializer = new ComplexArraySerializer(arrayValueClass,valueSignature, this.managedClass, this.managedFieldName);
                }else{
                    this.nestedSerializer = new ComplexArraySerializer(((ParameterizedType)genericType).getActualTypeArguments()[0],valueSignature,this.managedClass,this.managedFieldName);
                }
            }

        }else{
            this.nestedSerializer = new PrimitiveArraySerializer(arrayValueClass,valueSignature,this.managedClass,this.managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value){
        Object[] returned;
        if(isPrimitiveArray){
            returned = new Object[((Serializable[]) value).length];
        }else{
            returned = new Object[((List) value).size()];
        }

        if(this.isPrimitiveArray){
            int i = 0;
            for(Object s : (Object[]) value) {
                returned[i++] = this.nestedSerializer.serialize(s);
            }
        }else{
            int i = 0;
            for(Object s : (List) value) {
                returned[i++] = this.nestedSerializer.serialize(s);
            }
        }

        return returned;
    }

    @Override
    public Object unserialize(Object value) throws MessageSignatureMismatchException {
        if(!(value instanceof Object[])) throw new IllegalStateException("Unexpected object class, expected DBUSObject[]");
        boolean isDBusObject = value instanceof DBusObject[];
        Object[] values = (Object[]) value;

        if(this.isPrimitiveArray){
            int i = 0;
            Object[] returned = (Object[]) Array.newInstance(this.objectClass,((Object[]) value).length);
            for(Object s : values){
                if(isDBusObject) s = new DBusObject(this.valueSignature.getSignatureString(), ((DBusObject)s).getValues());
                returned[i++] = this.nestedSerializer.unserialize(s);
            }
            return returned;
        }else{
            List returned = new ArrayList();
            for(Object s : values){
                if(isDBusObject) s = new DBusObject(this.valueSignature.getSignatureString(), ((DBusObject)s).getValues());
                returned.add(this.nestedSerializer.unserialize(s));
            }
            return returned;
        }
    }
}
