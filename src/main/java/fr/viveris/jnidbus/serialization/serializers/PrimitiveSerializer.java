package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;

public class PrimitiveSerializer extends Serializer {
    public PrimitiveSerializer(Class<?> expectedType, SignatureElement signatureElement, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signatureElement,managedClass,managedFieldName );
        if(!expectedType.isAssignableFrom(signature.getPrimitive().getPrimitiveType()) &&
                !expectedType.isAssignableFrom(signature.getPrimitive().getBoxedType())){
            throw new MessageCheckException("The field type is not compatible with the dbus type",this.managedClass,this.managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value){
        return value;
    }

    @Override
    public Object unserialize(Object value) {
        return value;
    }
}
