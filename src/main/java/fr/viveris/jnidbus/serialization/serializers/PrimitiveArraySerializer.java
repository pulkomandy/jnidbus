package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrimitiveArraySerializer extends Serializer {
    private boolean isPrimitiveArray;
    private boolean isBoxedArray;
    private Class expectedArrayType;
    private Class expectedValueType;

    public PrimitiveArraySerializer(Class<?> expectedType, SignatureElement signature, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signature, managedClass, managedFieldName);
        this.expectedArrayType = expectedType;
        this.expectedValueType = expectedType.getComponentType();
        this.isBoxedArray = this.expectedValueType != null && Object.class.isAssignableFrom(this.expectedValueType);
        this.isPrimitiveArray = expectedType.isAssignableFrom(signature.getPrimitive().getBoxedArrayType()) ||
                expectedType.isAssignableFrom(signature.getPrimitive().getPrimitiveArrayType());

        if(!this.isPrimitiveArray && !List.class.equals(expectedType)){
            throw new MessageCheckException("An array must be a Java primitive array or a List",managedClass,managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value){
        //primitive arrays do not need any processing as they contain only primitive DBus values
        if(this.isPrimitiveArray) return value;
        else return ((List)value).toArray();
    }

    @Override
    public Object deserialize(Object value) {
        Object[] values = (Object[]) value;

        if(this.isPrimitiveArray) {
            if(values.length == 0) return Array.newInstance(this.expectedValueType,0);
            else if(this.isBoxedArray){
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
