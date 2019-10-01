package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrimitiveArraySerializer extends Serializer {
    private boolean isPrimitiveArray;
    private boolean isBoxedArray;
    private boolean isEnumArray;
    private SupportedTypes expectedValueSignature;
    private Class expectedArrayType;
    private Class expectedValueType;

    //used for enum arrays
    private Serializer primitiveSerializer;

    public PrimitiveArraySerializer(Type genericType, SignatureElement signature, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signature, managedClass, managedFieldName);

        Class<?> rawType;
        if(genericType instanceof ParameterizedType){
            rawType = (Class<?>) ((ParameterizedType)genericType).getRawType();
        }else{
            rawType = (Class<?>) genericType;
        }

        this.isPrimitiveArray = !List.class.isAssignableFrom(rawType);
        this.expectedArrayType = rawType;

        if(this.isPrimitiveArray && !rawType.isArray()){
            throw new MessageCheckException("An array must be a Java primitive array or a List",managedClass,managedFieldName);
        }


        if(this.isPrimitiveArray){
            //primitive arrays have a method for that
            this.expectedValueType = rawType.getComponentType();
        }else{
            //get the generic type, if the resulting type is also generic, get its raw type
            Type listType = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if(listType instanceof ParameterizedType){
                this.expectedValueType = (Class<?>) ((ParameterizedType)listType).getRawType();
            }else{
                this.expectedValueType = (Class<?>) listType;
            }
        }

        this.isEnumArray = this.expectedValueType.isEnum();
        this.expectedValueSignature = signature.getPrimitive();
        this.isBoxedArray = this.expectedValueType != null && Object.class.isAssignableFrom(this.expectedValueType);
        this.primitiveSerializer = new PrimitiveSerializer(this.expectedValueType,signature.getSignature().getFirst(),managedClass,managedFieldName);
    }

    @Override
    public Object serialize(Object value){
        //primitive arrays do not need any processing as they contain only primitive DBus values
        if(this.isEnumArray){
            Object[] returned;
            if(this.isPrimitiveArray){
                Object[] values = (Object[])value;
                returned = new Object[values.length];
                int i = 0;
                for(Object o : values){
                    returned[i++] = this.primitiveSerializer.serialize(o);
                }
            } else{
                List values = (List)value;
                returned = new Object[values.size()];
                int i = 0;
                for(Object o : values){
                    returned[i++] = this.primitiveSerializer.serialize(o);
                }
            }

            return returned;
        }
        else if(this.isPrimitiveArray) return value;
        else return ((List)value).toArray();
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        Object[] values = (Object[]) value;

        if(this.isPrimitiveArray) {
            if(values.length == 0) return Array.newInstance(this.expectedValueType,0);
            else if(this.isBoxedArray && !this.isEnumArray){
                return Arrays.copyOf(values,values.length,this.expectedArrayType);
            }else{
                Object array = Array.newInstance(this.expectedValueType,values.length);
                int i = 0;
                for(Object v : values){
                    Array.set(array,i++,this.primitiveSerializer.deserialize(v));
                }
                return array;
            }
        }
        else{
            //workaround of Array.toList() which returns a list with an empty array inside of the array is empty or null
            if(values == null || values.length == 0) return Collections.emptyList();
            else if(this.isEnumArray){
                List returned = new ArrayList();
                for(Object v : values){
                    returned.add(this.primitiveSerializer.deserialize(v));
                }
                return returned;
            }
            else return Arrays.asList(values);
        }
    }
}
