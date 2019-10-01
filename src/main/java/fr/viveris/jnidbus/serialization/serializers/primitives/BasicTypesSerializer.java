package fr.viveris.jnidbus.serialization.serializers.primitives;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.serializers.Serializer;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

public class BasicTypesSerializer extends Serializer {

    public BasicTypesSerializer(Class<?> expectedType, SignatureElement signatureElement, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signatureElement,managedClass,managedFieldName );
        if(!expectedType.isAssignableFrom(signatureElement.getPrimitive().getPrimitiveType()) &&
                !expectedType.isAssignableFrom(signatureElement.getPrimitive().getBoxedType())){
            throw new MessageCheckException("The field type is not compatible with the dbus type",this.managedClass,this.managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value) {
        return value;
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        return value;
    }
}
