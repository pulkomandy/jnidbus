package fr.viveris.jnidbus.serialization.serializers;

import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializers are objects bound the the field of a Message, that will process the serialization and deserialization of
 * the field value. A Serializer MUST not use reflection in its methods and should instead use it in its constructor to
 * retrieve all the information needed once.
 *
 * Serializers can use other serializers when dealing with nested values in order to maximize code reuse
 */
public abstract class Serializer {
    private final static Logger LOG = LoggerFactory.getLogger(Serializer.class);
    /**
     * Signature element of the managed field
     */
    protected SignatureElement signature;

    /**
     * Class managed by this serializer (for logging purposes)
     */
    protected Class managedClass;

    /**
     * Field managed by this serializer (for logging purposes)
     */
    protected String managedFieldName;

    public Serializer(SignatureElement signature, Class managedClass, String managedFieldName) {
        LOG.debug("Creating serializer for the class {} and field {}",managedClass.getSimpleName(),managedFieldName);
        this.signature = signature;
        this.managedClass = managedClass;
        this.managedFieldName = managedFieldName;
    }

    public abstract Object serialize(Object value);

    public abstract Object deserialize(Object value) throws MessageSignatureMismatchException;
}
