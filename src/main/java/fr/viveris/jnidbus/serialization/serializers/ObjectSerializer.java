package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.cache.MessageMetadata;
import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;

public class ObjectSerializer extends Serializer {
    private MessageMetadata metadata;

    public ObjectSerializer(Class<? extends Serializable> clazz,SignatureElement signature, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signature, managedClass, managedFieldName);
        MessageMetadata testedEntity = new MessageMetadata(clazz);
        Message.addToCache(clazz,testedEntity);
        this.metadata = testedEntity;
    }

    @Override
    public Object serialize(Object value) {
        return ((Serializable)value).serialize();
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        if(!(value instanceof DBusObject)) throw new IllegalStateException("Unexpected object class, expected DBusObject");
        Serializable deserialized = this.metadata.newInstance();
        deserialized.deserialize(new DBusObject(this.signature.getSignatureString(),((DBusObject)value).getValues()));
        return deserialized;
    }

}
