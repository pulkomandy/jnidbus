package fr.viveris.jnidbus.serialization.serializers.primitives;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.serializers.Serializer;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.types.ObjectPath;

public class ObjectPathSerializer extends Serializer {

    public ObjectPathSerializer(Class<?> expectedType, SignatureElement signatureElement, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signatureElement,managedClass,managedFieldName );
        if(!ObjectPath.class.isAssignableFrom(expectedType)){
            throw new MessageCheckException("The field type should be ObjectPath",this.managedClass,this.managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value) {
        return ((ObjectPath)value).getPath();
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        return new ObjectPath((String) value);
    }
}
