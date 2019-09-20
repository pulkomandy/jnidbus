/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.serialization.signature;

/**
 * Data class containing all the information about a signature element. An element can either be primitive (int, string,
 * etc...) or be a container (array, object).
 *
 * If the container is an array, the getPrimitive() method might return a type if the array contains a primitive types,
 * if the array contains an object, getPrimitive() will return null. If the element is a container, we can get the signature
 * of its content by calling getSignature().
 */
public class SignatureElement {
    /**
     * Signature of the element. If the element is a container, its signature will container the signature of its content
     */
    private String signature;

    /**
     * Container type, can be null
     */
    private SupportedTypes containerType;

    /**
     * Primitive type, also contains type of primitive arrays
     */
    private SupportedTypes primitiveType;

    public SignatureElement(String signature, SupportedTypes primitiveType, SupportedTypes containerType) {
        this.signature = signature;
        this.primitiveType = primitiveType;
        this.containerType = containerType;
    }

    public boolean isArray(){
        return this.containerType == SupportedTypes.ARRAY;
    }

    public boolean isPrimitive(){
        return this.primitiveType != null && !this.isArray();
    }

    public SupportedTypes getPrimitive(){
        return this.primitiveType;
    }

    public boolean isObject(){
        return this.containerType == SupportedTypes.OBJECT_BEGIN;
    }

    public boolean isDictEntry(){
        return this.containerType == SupportedTypes.OBJECT_BEGIN;
    }

    public Signature getSignature(){
        return new Signature(this.signature);
    }

    public String getSignatureString(){
        return this.signature;
    }

    public SupportedTypes getContainerType() { return containerType; }

    @Override
    public String toString() {
        return "SignatureElement{" +
                "signature='" + signature + '\'' +
                ", containerType=" + containerType +
                ", primitiveType=" + primitiveType +
                '}';
    }
}
