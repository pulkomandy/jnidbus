/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.cache.Cache;
import fr.viveris.jnidbus.cache.MessageMetadata;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.DBusType;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.serialization.serializers.Serializer;

import java.lang.reflect.Method;

/**
 * Represent anything that can be sent to dbus. This class implements serialization and deserialization methods that
 * will use reflection and the DBusType annotation to know how to perform the serialization. As the reflection is slow
 * we use a cache to store information that will not change between serializations.
 *
 * An extending class serializable fields should only be:
 *      -A string
 *      -A primitive (or boxed type)
 *      -A List, Nested lists and objects are supported
 *      -Another Message class
 */
public abstract class Message implements Serializable {
    /**
     * Special message type which has an empty signature. As message of this type can often happen, we can use this
     * static instance instead of creating your own
     */
    public static final EmptyMessage EMPTY = new EmptyMessage();

    /**
     * Cache containing the reflection data. The map use a ClassLoader as a key in order to support same name classes loaded
     * by different class loaders. In addition this map is weak so an unused class loader can be freed without issues
     * (hot reload of classes for example)
     */
    private static final Cache<Class<? extends Serializable>, MessageMetadata> CACHE = new Cache<>();


    @Override
    public DBusObject serialize() {
        //retrieve reflection data from the cache
        Class<? extends Message> clazz = this.getClass();
        MessageMetadata messageMetadata = Message.retrieveFromCache(clazz);

        //set the array length at the number of field and iterate on the signature
        Object[] values = new Object[messageMetadata.getFields().length];
        int i = 0;
        for(String fieldName : messageMetadata.getFields()){
            try{
                //retrieve the getter from the cache
                Method getter = messageMetadata.getGetter(fieldName);
                Object returnValue = getter.invoke(this);
                if(returnValue == null) throw new NullPointerException("A DBus value can not be nullable");

                values[i++] = messageMetadata.getFieldSerializer(fieldName).serialize(returnValue);
            }catch (Exception e){
                throw new IllegalStateException("An exception was raised during serialization "+e.toString(),e);
            }
        }

        return new DBusObject(messageMetadata.getSignature(),values);
    }

    @Override
    public void deserialize(DBusObject obj) throws MessageSignatureMismatchException {
        //retrieve reflection data from the cache
        Class<? extends Message> clazz = this.getClass();
        MessageMetadata messageMetadata = Message.retrieveFromCache(clazz);

        //check if the given pre-deserialized object have the same signature as this class
        if(!messageMetadata.getSignature().equals(obj.getSignature())) throw new MessageSignatureMismatchException("Signature mismatch, expected "+ messageMetadata.getSignature()+" but got "+obj.getSignature());

        int i = 0;
        for(Object value : obj.getValues()){
            //get the field name for the current signature element
            String fieldName = messageMetadata.getFields()[i];

            try{
                //retrieve the setter from the cache
                Method setter = messageMetadata.getSetter(fieldName);
                Serializer serializer = messageMetadata.getFieldSerializer(fieldName);
                Object deserialized = serializer.deserialize(value);
                setter.invoke(this,deserialized);
            }catch (Exception e){
                throw new IllegalStateException("An exception was raised during deserialization",e);
            }
            //go to the next value
            i++;
        }
    }

    /**
     * Try to retrieve the cached metadata for the given class, if the cache entity does not exists, check the class,
     * try to create one and return it. If the class is invalid, it will throw
     *
     * @param clazz class to retrieve
     * @return the cached entity
     */
    public static MessageMetadata retrieveFromCache(Class<? extends Serializable> clazz){
        //if the cache is null, make the entity null, the cache will be created when the clazz is processed
        MessageMetadata meta = CACHE.getCachedEntity(clazz);
        if(meta != null){
            return meta;
        }

        try {
            meta = new MessageMetadata(clazz);
            Message.addToCache(clazz,meta);
            return meta;
        } catch (Exception e) {
            throw new IllegalStateException("Message validity check failed: " + e, e);
        }
    }

    public static void addToCache(Class<? extends Serializable> clazz, MessageMetadata meta){
        CACHE.addCachedEntity(clazz, meta);
    }

    public static void clearCache(){
        CACHE.clear();
    }

    /**
     * Special type of message that do not need serialization or deserialization.
     */
    @DBusType(
            signature = "",
            fields = {}
    )
    public static class EmptyMessage extends Message{
        public EmptyMessage(){}
    }
}
