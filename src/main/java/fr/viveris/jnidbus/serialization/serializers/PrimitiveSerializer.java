package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.serializers.primitives.BasicTypesSerializer;
import fr.viveris.jnidbus.serialization.serializers.primitives.EnumSerializer;
import fr.viveris.jnidbus.serialization.serializers.primitives.ObjectPathSerializer;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;
import fr.viveris.jnidbus.types.ObjectPath;

import java.lang.annotation.ElementType;

/**
 * The PrimitiveSerializer doesn't do much beside checking the expected type of value
 */
public class PrimitiveSerializer extends Serializer {
    private static final Class<?>[] NON_BASIC_TYPES = {Enum.class, ObjectPath.class};

    private Serializer nestedSerializer;

    public PrimitiveSerializer(Class<?> expectedType, SignatureElement signatureElement, Class managedClass, String managedFieldName) throws MessageCheckException {
        super(signatureElement,managedClass,managedFieldName );
        if(expectedType.isEnum()){
            this.nestedSerializer = new EnumSerializer(expectedType,signatureElement,managedClass,managedFieldName);
        }else if(signatureElement.getPrimitive() == SupportedTypes.OBJ_PATH){
            this.nestedSerializer = new ObjectPathSerializer(expectedType,signatureElement,managedClass,managedFieldName);
        }else {
            this.nestedSerializer = new BasicTypesSerializer(expectedType,signatureElement,managedClass,managedFieldName);
        }
    }

    @Override
    public Object serialize(Object value){
        return this.nestedSerializer.serialize(value);
    }

    @Override
    public Object deserialize(Object value) throws MessageSignatureMismatchException {
        return this.nestedSerializer.deserialize(value);
    }

    public static boolean isNonBasicType(Class<?> clazz){
        for(Class c : PrimitiveSerializer.NON_BASIC_TYPES){
            if(c.isAssignableFrom(clazz)){
                return true;
            }
        }
        return false;
    }
}
