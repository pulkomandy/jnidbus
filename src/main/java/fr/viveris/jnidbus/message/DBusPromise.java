package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;

/**
 * DBus promises are used by JNI code to notify the return af a call. Such promises must be instantiated with the expected
 * value class for performance purposes (reflection cal to determine promise type at runtime are expensive)
 * @param <T>
 */
public class DBusPromise<T extends Serializable> extends Promise<T> {
    private Class<T> clazz;

    /**
     * Create a new promise expecting the given serializable type
     * @param clazz
     */
    public DBusPromise(Class<T> clazz){
        this.clazz = clazz;
    }

    /**
     * Called by JNI code. The promise will try to deserialize the DBusObject using the expected type and fail if it is
     * not possible
     *
     * @param object
     */
    private void resolve(DBusObject object){
        try {
            T value;
            //if the message is an EmptyMessage, don't deserialize and use the static instance
            if(Message.EmptyMessage.class.equals(this.clazz)){
                this.resolve((T) Message.EMPTY);
            }else{
                //retreive the cached entity and create a new instance of the return result
                value = (T) Message.retrieveFromCache(this.clazz).newInstance();
                value.deserialize(object);
                this.resolve(value);
            }
        } catch (Exception e) {
            if(!this.isResolved()) this.fail(new DBusException(e.getClass().getName(),e.getMessage()));
        }
    }

    /**
     * Fail the promise with a DBusException
     * @param name
     * @param message
     */
    public void fail(String name, String message){
        if(!this.isResolved()) this.fail(new DBusException(name,message));
    }
}
