package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.serialization.signature.Signature;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class MapSerializer extends Serializer {
    private Serializer keySerializer;
    private Serializer valueSerializer;
    private Signature entrySignature;


    public MapSerializer(Type genericType, SignatureElement signature, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signature, managedClass, managedFieldName);
        if(!(genericType instanceof ParameterizedType)) throw new MessageCheckException("The given map is not generic", managedClass,managedFieldName);

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        if(!Map.class.isAssignableFrom((Class) parameterizedType.getRawType())){
            throw new MessageCheckException("The value of an array of dict_entries should eb a Map", managedClass,managedFieldName);
        }

        this.entrySignature = signature.getSignature().getFirst().getSignature();

        //setup key serializer
        if(!(parameterizedType.getActualTypeArguments()[0] instanceof Class)){
            throw new MessageCheckException("The given map keys are generic", managedClass,managedFieldName);
        }
        Class<?> keyClass = (Class) parameterizedType.getActualTypeArguments()[0];
        this.keySerializer = new PrimitiveSerializer(keyClass,this.entrySignature.getFirst(),managedClass,managedFieldName);

        //setup value serializer
        Type valueType = parameterizedType.getActualTypeArguments()[1];

        Class<?> valueClass;
        if(valueType instanceof ParameterizedType){
            valueClass = (Class) ((ParameterizedType) valueType).getRawType();
        }else{
            valueClass = (Class) valueType;
        }

        SignatureElement valueSignature = this.entrySignature.getSecond();
        if(valueSignature.getContainerType() == null){
            if(!(parameterizedType.getActualTypeArguments()[0] instanceof Class)){
                throw new MessageCheckException("The given map values are generic but should be primitive", managedClass,managedFieldName);
            }
            this.valueSerializer = new PrimitiveSerializer((Class) valueType,valueSignature,managedClass,managedFieldName);
        }else if(valueSignature.getContainerType() == SupportedTypes.ARRAY){
            if(valueSignature.getPrimitive() != null){
                this.valueSerializer = new PrimitiveArraySerializer(valueClass,valueSignature,managedClass,managedFieldName);
            }else{
                this.valueSerializer = new ComplexArraySerializer(valueType,valueSignature,managedClass,managedFieldName);
            }
        }else{
            if(!Serializable.class.isAssignableFrom(valueClass)){
                throw new MessageCheckException("The given map values are not serializable objects", managedClass,managedFieldName);
            }
            this.valueSerializer = new ObjectSerializer(valueClass.asSubclass(Serializable.class),valueSignature,managedClass,managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value) {
        Map<?,?> map = (Map) value;
        DBusObject[] entries = new DBusObject[map.size()];
        int i = 0;
        for(Map.Entry entry : map.entrySet()){
            Object[] pair = new Object[2];
            pair[0] = this.keySerializer.serialize(entry.getKey());
            pair[1] = this.valueSerializer.serialize(entry.getValue());
            entries[i++] = new DBusObject(this.signature.getSignature().getSignature(),pair);
        }
        return entries;
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        Object[] entries = (Object[]) value;
        Map map = new HashMap<>();
        for(Object obj : entries){
            Object[] pair = ((DBusObject)obj).getValues();
            map.put(this.keySerializer.deserialize(pair[0]),this.valueSerializer.deserialize(pair[1]));
        }

        return map;
    }
}
