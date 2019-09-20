package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.SerializationException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrimitiveArraySerializer extends Serializer {
    private boolean isNativeArray;
    private Class expectedArrayType;
    private Class expectedValueType;

    public PrimitiveArraySerializer(Class<?> expectedType, SignatureElement signature, Class managedClass, String managedFieldName) {
        super(signature, managedClass, managedFieldName);
        this.expectedArrayType = expectedType;
        this.expectedValueType = expectedType.getComponentType();
        this.isNativeArray = expectedType.isAssignableFrom(signature.getPrimitive().getBoxedArrayType()) ||
                expectedType.isAssignableFrom(signature.getPrimitive().getPrimitiveArrayType());

        if(!this.isNativeArray && !List.class.equals(expectedType)) throw new SerializationException("An array must be a Java primitive array or a List");
    }

    @Override
    public Object serialize(Object value){
        if(this.isNativeArray) return value;
        else return ((List)value).toArray();
    }

    @Override
    public Object deserialize(Object value) {
        if(!(value instanceof Object[])) throw new IllegalStateException("Unexpected object class, expected Object array");
        Object[] values = (Object[]) value;

        if(this.isNativeArray) {
            if(values.length == 0) return Array.newInstance(this.expectedValueType,0);
            else if(Object.class.isAssignableFrom(this.expectedValueType)){
                return Arrays.copyOf(values,values.length,this.expectedArrayType);
            }else{
                Object array = Array.newInstance(this.expectedValueType,values.length);
                int i = 0;
                for(Object v : values){
                    Array.set(array,i++,v);
                }
                return array;
            }
        }
        else{
            //workaround of Array.toList() which returns a list with an empty array inside of the array is empty or null
            if(values == null || values.length == 0) return Collections.emptyList();
            else return Arrays.asList(values);
        }
    }
}
