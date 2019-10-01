/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.cache;

import fr.viveris.jnidbus.exception.MessageCheckException;
import fr.viveris.jnidbus.serialization.DBusType;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.serialization.serializers.*;
import fr.viveris.jnidbus.serialization.signature.Signature;
import fr.viveris.jnidbus.serialization.signature.SignatureElement;
import fr.viveris.jnidbus.serialization.signature.SupportedTypes;

import java.lang.reflect.*;
import java.util.HashMap;

/**
 * A MessageMetadata contains the list of fields managed by JNIDBus, their setters and getters. In order lower the cost of
 * the heavy use of Reflection, the fields are mapped to a pair of Serializer/deserializer which will analyze the signature
 * and type of the field, save those information and reuse them when doing the actual serialization. This mechanism speeds
 * up a lot the serialization process.
 */
public class MessageMetadata {
    /**
     * Dbus signature string
     */
    private String signature;

    /**
     * Fields name bound to the Dbus signature
     */
    private String[] fields;

    /**
     * Public constructor
     */
    private Constructor<? extends Serializable> constructor;

    /**
     * Setters and getters, mapped by field name
     */
    private HashMap<String, Method> setters = new HashMap<>();
    private HashMap<String, Method> getters = new HashMap<>();

    /**
     * Serializer and deserializers by Methods
     */
    private HashMap<String, Serializer> fieldSerializers = new HashMap<>();

    /**
     * Message class object
     */
    private Class<? extends Serializable> clazz;

    /**
     * Create a new cache entry for the given message class. The class will be checked and an exception will be thrown if
     * an error was found. If the class contains other serializable classes, it will recursively check those as well and
     * cache them.
     *
     * @param clazz class to analyze
     * @throws MessageCheckException thrown when the class is not a valid serializable class
     */
    public MessageMetadata(Class<? extends Serializable> clazz) throws MessageCheckException {
        this.clazz = clazz;

        //check annotation
        DBusType type = clazz.getAnnotation(DBusType.class);
        if(type == null) throw new MessageCheckException("no DBusType annotation found",this.clazz);

        //check constructor
        Constructor<? extends Serializable> constructor;
        try {
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MessageCheckException("No public empty constructor found",this.clazz);
        }

        //create a new cache entity
        this.signature = type.signature();
        this.fields = type.fields();
        this.constructor = constructor;

        //generate getter and setter list
        int i = 0;
        for(SignatureElement element : new Signature(type.signature())){
            String fieldName = type.fields()[i];
            //generate setter and getter name
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            Field field;
            try{
                field = clazz.getDeclaredField(fieldName);
            }catch (Exception e){
                throw new MessageCheckException("Could not find field",this.clazz,fieldName);
            }

            Type fieldType = field.getGenericType();

            try {
                //get the getter and create its serializer, the serializer will check signature and type validity
                Method getter = clazz.getDeclaredMethod(getterName);
                this.getters.put(fieldName,getter);
                if(!fieldType.equals(getter.getGenericReturnType())){
                    throw new MessageCheckException("The getter return type is not the same as the field",this.clazz,fieldName);
                }
            } catch (NoSuchMethodException e) {
                throw new MessageCheckException("Could not find getter",this.clazz,fieldName);
            }

            try {
                //get the setter and create its deserializer, the deserializer will check signature and type validity
                Method setter = clazz.getDeclaredMethod(setterName,field.getType());
                this.setters.put(fieldName,setter);
                if(setter.getGenericParameterTypes().length != 1){
                    throw new MessageCheckException("The setter must only have one parameter",this.clazz,fieldName);
                }
                if(!fieldType.equals(setter.getGenericParameterTypes()[0])){
                    throw new MessageCheckException("The setter return type is not the same as the field",this.clazz,fieldName);
                }
            } catch (NoSuchMethodException e) {
                throw new MessageCheckException("Could not find setter",this.clazz,fieldName);
            }

            this.fieldSerializers.put(fieldName,this.generateSerializerForSignature(fieldType,element,fieldName));
            i++;
        }

        if(i != this.fields.length){
            throw new MessageCheckException("Incomplete signature, there is too much fields",this.clazz);
        }
    }

    public Method getGetter(String fieldName){
        return this.getters.get(fieldName);
    }

    public Serializer getFieldSerializer(String fieldName){
        return this.fieldSerializers.get(fieldName);
    }
    public Method getSetter(String fieldName){
        return this.setters.get(fieldName);
    }

    public String getSignature() {
        return signature;
    }

    public String[] getFields() {
        return fields;
    }

    public Class<? extends Serializable> getMessageClass(){ return this.clazz; }

    /**
     * Create a new empty instances of the Message object (for deserialization).
     *
     * @return new empty instance of the message
     */
    public Serializable newInstance(){
        try {
            return this.constructor.newInstance();
        } catch (Exception e) {
           throw new IllegalStateException("Could not create instance: "+e.toString(),e);
        }
    }

    /**
     * This method will try to find a serializer matching a generic type and a signature, the created serializer will
     * perform all the necessary checks and try to find incoherence.
     * @param genericType the Type of the method, could be a parametrized type or standard one
     * @param element
     * @return
     * @throws MessageCheckException
     */
    private Serializer generateSerializerForSignature(Type genericType, SignatureElement element, String fieldName) throws MessageCheckException {
        //TypeVariables are not usable and should not be allowed
        if(genericType instanceof TypeVariable) throw new MessageCheckException("Unspecified generic type found",this.clazz,fieldName);

        //if the method returns a generic type, extract it, this allows us to support generic messages and nested lists
        Class<?> clazz;
        if(genericType instanceof ParameterizedType){
            clazz = (Class<?>) ((ParameterizedType)genericType).getRawType();
        }else{
            clazz = (Class<?>) genericType;
        }

        //if we have an array container type and a primitive type, it means we have a primitive array
        if(element.getContainerType() == SupportedTypes.ARRAY && element.getPrimitive() != null){
            return new PrimitiveArraySerializer(genericType,element,this.clazz,fieldName);

        // if there is a primitive type but no container, we have a primitive type
        }else if(element.getContainerType() == null && element.getPrimitive() != null){
            return new PrimitiveSerializer(clazz,element,this.clazz,fieldName);

        //as we checked for primitive arrays before, if there is an array container type it means we have a complex array or a map
        }else if(element.getContainerType() == SupportedTypes.ARRAY){
            //check first element type to se if we have a Map
            if(element.getSignature().getFirst().getContainerType() == SupportedTypes.DICT_ENTRY_BEGIN){
                return new MapSerializer(genericType,element,this.clazz,fieldName);
            }else{
                return new ComplexArraySerializer(genericType,element,this.clazz,fieldName);
            }

        //if there is no primitive type and an object container type, we have an object
        }else if(element.getContainerType() == SupportedTypes.OBJECT_BEGIN) {
            if (!Serializable.class.isAssignableFrom(clazz)) {
                throw new MessageCheckException("Field not Serializable", this.clazz, fieldName);
            }
            return new ObjectSerializer(clazz.asSubclass(Serializable.class), element, this.clazz, fieldName);
        }else if( element.getContainerType() == SupportedTypes.DICT_ENTRY_BEGIN){
            throw new MessageCheckException("A dict_entry can not be placed outside of an array", this.clazz, fieldName);
        }else{
            throw new MessageCheckException("The given type could not be mapped to any serializer",this.clazz,fieldName);
        }
    }
}
