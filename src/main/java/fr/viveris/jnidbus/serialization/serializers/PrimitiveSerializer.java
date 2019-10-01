package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

import java.lang.annotation.ElementType;

/**
 * The PrimitiveSerializer doesn't do much beside checking the expected type of value
 */
public class PrimitiveSerializer extends Serializer {
    private boolean isEnum = false;
    private Class<? extends Enum> enumType;
    private SupportedTypes type;

    public PrimitiveSerializer(Class<?> expectedType, SignatureElement signatureElement, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signatureElement,managedClass,managedFieldName );
        this.type = signatureElement.getPrimitive();
        if(expectedType.isEnum()){
            if(this.type != SupportedTypes.STRING && this.type != SupportedTypes.INTEGER){
                throw new MessageCheckException("The passed primitive is an enum but the signature is not a String or Integer",managedClass,managedFieldName);
            }
            this.isEnum = true;
            this.enumType = expectedType.asSubclass(Enum.class);
        } else if(!expectedType.isAssignableFrom(signature.getPrimitive().getPrimitiveType()) &&
                !expectedType.isAssignableFrom(signature.getPrimitive().getBoxedType())){
            throw new MessageCheckException("The field type is not compatible with the dbus type",this.managedClass,this.managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value){
        if(this.isEnum){
            if(this.type == SupportedTypes.STRING){
                return ((Enum)value).name();
            }else{
                return ((Enum)value).ordinal();
            }
        }
        return value;
    }

    @Override
    public Object deserialize(Object value) {
        if(this.isEnum){
            if(this.type == SupportedTypes.STRING){
                return Enum.valueOf(this.enumType,(String) value);
            }else{
                return this.enumType.getEnumConstants()[(Integer) value];
            }
        }
        return value;
    }
}
