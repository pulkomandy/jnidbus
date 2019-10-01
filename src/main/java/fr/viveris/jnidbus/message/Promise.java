/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.message.eventloop.sending.ErrorReplySendingRequest;
import fr.viveris.jnidbus.message.eventloop.sending.ReplySendingRequest;
import fr.viveris.jnidbus.serialization.Serializable;

import java.util.concurrent.atomic.AtomicLong;

public class Promise<T extends Serializable> {
    /**
     * State of the promise or message pointer:
     *  0: no result and no msg pointer
     *  1: result but no msg pointer
     *  2: error but no msg pointer
     *  else: msg pointer
     */
    private AtomicLong msgPointer = new AtomicLong(0);

    /**
     * Event loop on which we will dispatch the response
     */
    private EventLoop eventLoop;

    /**
     * Result of the promise
     */
    private T result;

    /**
     * error of the promise
     */
    private DBusException error;

    /**
     * For debug purposes
     */
    private String interfaceName;
    private String member;

    public void resolve(T result){
        this.result = result;
        if(msgPointer.compareAndSet(0,1)){
            //don't dispatch, msg pointer not set yet
        }else if(this.msgPointer.get() != 2){
            this.dispatch(false);
        }

    }

    public void reject(DBusException exc){
        this.error = exc;
        if(msgPointer.compareAndSet(0,2)){
            //don't dispatch, msg pointer not set yet
        }else if(this.msgPointer.get() != 1){
            this.dispatch(true);
        }
    }

    public void setMessagePointer(long ptr, String interfaceName, String member, EventLoop eventLoop){
        this.eventLoop = eventLoop;
        this.interfaceName = interfaceName;
        this.member = member;
        //if there is already a result, dispatch
        if(this.msgPointer.compareAndSet(0,ptr)){
            //don't dispatch, result is not here yet
        }else if(this.msgPointer.compareAndSet(1,ptr)){
            this.dispatch(false);
        }else if(this.msgPointer.compareAndSet(2,ptr)){
            this.dispatch(true);
        }
    }

    private void dispatch(boolean error){
        if(error){
            this.eventLoop.send(new ErrorReplySendingRequest(this.error,this.msgPointer.get(),this.interfaceName,this.member,null));
        }else{
            this.eventLoop.send(new ReplySendingRequest(this.result.serialize(),this.msgPointer.get(),this.interfaceName,this.member,null));
        }
    }

}
