/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.cache.Cache;
import fr.viveris.jnidbus.cache.MessageMetadata;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.exception.SerializationException;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.DBusType;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.serialization.serializers.Serializer;

import java.lang.reflect.Method;

/**
 * Represent anything that can be sent to dbus. The serialization process uses a lot of Reflection, which would slow
 * JNIDBus down. In order to prevent that, Messages metadata are cached when first used, those metadata contain a list
 * of Serializer objects bound to each fields. Those Serializer use Reflection only at instantiation time for better
 * performances.
 *
 * An extending class serializable fields should only be:
 *      -A string
 *      -A primitive (or boxed type)
 *      -A List/Array, Nested lists/arrays/serializable are supported
 *      -A Map with a primitive key (int,long,short,byte,bool,double,String)
 *      -Another Serializable class
 */
public abstract class Message implements Serializable {
    /**
     * Special message type which has an empty signature. As message of this type can often happen, we can use this
     * static instance instead of creating your own, which speeds up the sending quite a bit
     */
    public static final EmptyMessage EMPTY = new EmptyMessage();

    /**
     * Cache containing the Message metadata
     */
    private static final Cache<Class<? extends Serializable>, MessageMetadata> CACHE = new Cache<>();


    @Override
    public DBusObject serialize() {
        //retrieve reflection data from the cache
        Class<? extends Message> clazz = this.getClass();
        MessageMetadata messageMetadata = Message.retrieveFromCache(clazz);

        //set the array length at the number of field and iterate on them
        Object[] values = new Object[messageMetadata.getFields().length];
        int i = 0;
        for(String fieldName : messageMetadata.getFields()){
            try{
                //retrieve the getter from the cache, execute it and serialize its output
                Method getter = messageMetadata.getGetter(fieldName);
                Object returnValue = getter.invoke(this);
                if(returnValue == null) throw new SerializationException("A DBus value can not be nullable");

                values[i++] = messageMetadata.getFieldSerializer(fieldName).serialize(returnValue);
            }catch (Exception e){
                throw new SerializationException("An exception was raised during serialization "+e.toString(),e);
            }
        }

        return new DBusObject(messageMetadata.getSignature(),values);
    }

    @Override
    public void deserialize(DBusObject obj) throws MessageSignatureMismatchException {
        //retrieve reflection data from the cache
        Class<? extends Message> clazz = this.getClass();
        MessageMetadata messageMetadata = Message.retrieveFromCache(clazz);

        //check if the given object have the same signature as this class
        if(!messageMetadata.getSignature().equals(obj.getSignature())){
            throw new MessageSignatureMismatchException("Signature mismatch, expected "+ messageMetadata.getSignature()+" but got "+obj.getSignature());
        }

        //iterate on the values/fields
        int i = 0;
        for(Object value : obj.getValues()){
            //get the field name for the current signature element
            String fieldName = messageMetadata.getFields()[i++];

            try{
                //retrieve the setter from the cache and execute it with the deserialized output
                Method setter = messageMetadata.getSetter(fieldName);
                Serializer serializer = messageMetadata.getFieldSerializer(fieldName);
                Object deserialized = serializer.deserialize(value);
                setter.invoke(this,deserialized);
            }catch (Exception e){
                throw new SerializationException("An exception was raised during deserialization",e);
            }
        }
    }

    /**
     * Try to retrieve the cached metadata for the given class, if the cache entity does not exists,
     * try to create one and return it. If the class is invalid, this method will throw
     *
     * @param clazz class to retrieve
     * @return the cached entity
     */
    public static MessageMetadata retrieveFromCache(Class<? extends Serializable> clazz){
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
