package fr.viveris.jnidbus.serialization.serializers.primitives;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.serialization.serializers.Serializer;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

public class EnumSerializer extends Serializer {
    Class<? extends Enum> clazz;
    SupportedTypes type;

    public EnumSerializer(Class<?> expectedType, SignatureElement signatureElement, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signatureElement, managedClass, managedFieldName);

        if(!Enum.class.isAssignableFrom(expectedType)){
            throw new MessageCheckException("The expected type should be an enum but isn't",managedClass,managedFieldName);
        }

        this.type = signatureElement.getPrimitive();
        if(this.type != SupportedTypes.STRING && this.type != SupportedTypes.INTEGER){
            throw new MessageCheckException("The passed primitive is an enum but the signature is not a String or Integer",managedClass,managedFieldName);
        }
        this.clazz = expectedType.asSubclass(Enum.class);
    }

    @Override
    public Object serialize(Object value){
        if(this.type == SupportedTypes.STRING){
            return ((Enum)value).name();
        }else{
            return ((Enum)value).ordinal();
        }
    }

    @Override
    public Object deserialize(Object value) {
        if(this.type == SupportedTypes.STRING){
            return Enum.valueOf(this.clazz,(String) value);
        }else{
            return this.clazz.getEnumConstants()[(Integer) value];
        }
    }
}
