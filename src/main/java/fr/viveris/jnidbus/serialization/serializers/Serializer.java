package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;

public abstract class Serializer {
    protected SignatureElement signature;
    protected Class managedClass;
    protected String managedFieldName;

    public Serializer(SignatureElement signature, Class managedClass, String managedFieldName) {
        this.signature = signature;
        this.managedClass = managedClass;
        this.managedFieldName = managedFieldName;
    }

    public abstract Object serialize(Object value);

    public abstract Object deserialize(Object value) throws MessageSignatureMismatchException;
}
