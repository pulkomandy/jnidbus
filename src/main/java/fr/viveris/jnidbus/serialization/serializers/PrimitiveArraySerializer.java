package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.SerializationException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrimitiveArraySerializer extends Serializer {
    private boolean isNativeArray;

    public PrimitiveArraySerializer(Class<?> expectedType, SignatureElement signature, Class managedClass, String managedFieldName) {
        super(signature, managedClass, managedFieldName);
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
    public Object unserialize(Object value) {
        if(!(value instanceof Object[])) throw new IllegalStateException("Unexpected object class, expected Object array");
        if(this.isNativeArray) return value;
        else{
            Object[] values = (Object[]) value;
            //workaround of Array.toList() which returns a list with an empty array inside of the array is empty or null
            if(values == null || values.length == 0) return Collections.emptyList();
            else return Arrays.asList(values);
        }
    }
}
