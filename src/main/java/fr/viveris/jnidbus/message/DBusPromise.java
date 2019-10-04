package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;

public class DBusPromise<T extends Serializable> extends Promise<T> {
    private Class<T> clazz;

    public DBusPromise(Class<T> clazz){
        this.clazz = clazz;
    }

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
           this.fail(new DBusException(e.getClass().getName(),e.getMessage()));
        }
    }

    public void fail(String name, String message){
        this.fail(new DBusException(name,message));
    }
}
