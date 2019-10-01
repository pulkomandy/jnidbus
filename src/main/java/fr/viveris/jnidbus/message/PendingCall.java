/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;

/**
 * Represent a call result. You can see this as some sort of Promise, you can attach a listener to this class to be
 * asynchronously notified when a result or an error arrives. If the result arrived before you attached the listener,
 * the listener will be notified instantly and the result wont be lost. The listener will eb notified ONCE, if an error
 * come before a result, the result will be stored in the PendingCall, but the listener wont be notified.
 *
 * This mechanism allows the developer to manually cancel the PendingCall by calling the "fail" method and be sure the
 * listener wont receive unwanted results as DBus can not cancel calls.
 *
 * If the received result is not the same as the expected type, the listener will be notified with a SignatureMismatch
 * exception
 *
 * @param <T> return type of the call
 */
public class PendingCall<T extends Serializable> {
    public static final String PENDING_CALL_CANCELLED_ERROR_CODE = "fr.viveris.vizada.jnidbus.cancelled";
    /**
     * Event loop on which we should dispatch execution
     */
    private EventLoop eventLoop;

    /**
     * Return type of the call
     */
    private Class<T> clazz;

    /**
     * Listener to notify
     */
    private Listener<T> listener = null;

    /**
     * Result received
     */
    private T result = null;

    /**
     * Error received or set manually
     */
    private DBusException error = null;

    /**
     * is the PendingCall cancelled
     */
    private volatile boolean isCancelled = false;


    /**
     * Create a new PendingCall returning an instance of the given class.
     *
     * @param clazz return type class
     * @param eventLoop event loop the pending call must be dispatched if the listener is bound after the result was received
     */
    public PendingCall(Class<T> clazz, EventLoop eventLoop){
        this.eventLoop = eventLoop;
        this.clazz = clazz;
    }

    /**
     * Bind a listener to this PendingCall. Only one listener is allowed to be registered and if a result or error has
     * already been received, the listener will be notified instantly
     *
     * @param listener listener to bind
     */
    synchronized public void setListener(Listener<T> listener){
        //only one listener is allowed
        if(this.listener != null) throw new IllegalStateException("A listener has already been bound to this PendingCall");
        this.listener = listener;

        //if the result is already here, ask for a redispatch
        if(this.isResolved()){
            this.eventLoop.redispatch(this,null);
        }
    }

    /**
     * Force a listener notification executed on the current thread.
     */
    synchronized public void forceNotification(){
        //only one listener is allowed
        if(this.listener == null) throw new IllegalStateException("No listener where found");

        //if the result is already here, notify instantly
        if(this.result != null && !this.isCancelled){
            this.listener.notify(this.result);
        } else if(this.error != null){
            this.listener.error(this.error);
        }
    }

    /**
     * deserialize the pre-deserialized object and if everything goes well, notify the listener and store the result
     *
     * @param response pre-deserialized object
     */
    synchronized public void notify(DBusObject response){
        try {
            T value;
            //if the message is an EmptyMessage, don't deserialize and use the static instance
            if(this.clazz.equals(Message.EmptyMessage.class)){
                value = (T) Message.EMPTY;
            }else{
                //retreive the cached entity and create a new instance of the return result
                value = (T) Message.retrieveFromCache(this.clazz).newInstance();
                value.deserialize(response);
            }
            this.result = value;
            //if the listener is registered and that the PendingCall was not previously failed, notify
            if(this.listener != null && this.error == null){
                this.listener.notify(value);
            }
        } catch (Exception e) {
            this.error = new DBusException(e.getClass().getName(),e.getMessage());
            if(this.listener != null && this.result == null){
                this.listener.error(this.error);
            }
        }
    }

    public T getResult() {
        return result;
    }

    public DBusException getError() {
        return error;
    }

    synchronized public void cancel(){
        if(!this.isResolved()){
            this.isCancelled = true;
            this.fail(PENDING_CALL_CANCELLED_ERROR_CODE,"the call was forcibly cancelled");
        }
    }

    /**
     * Returns whether a a result or error was received or not
     *
     * @return did the promise received an error or a result
     */
    synchronized public boolean isResolved(){
        return this.error != null || this.result != null;
    }

    /**
     * Set the PendingCall error and if the listener was not previously notified, notify it
     *
     * @param errorName DBus error name
     * @param message error message
     */
    synchronized public void fail(String errorName, String message){
        DBusException exc = new DBusException(errorName,message);
        this.error = exc;
        if(this.listener != null && this.result == null){
            this.listener.error(exc);
        }
    }

    /**
     * A Listener will be notified ONCE with an error or a result. A Listener should not throw anything, if it does the
     * behavior of the library is undefined
     *
     * @param <T> expected result type
     */
    public interface Listener<T extends Serializable>{
        void notify(T value);
        void error(DBusException t);
    }
}
