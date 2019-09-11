/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.dispatching;

import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.Promise;
import fr.viveris.jnidbus.message.sendingrequest.ErrorReplySendingRequest;
import fr.viveris.jnidbus.message.sendingrequest.ReplySendingRequest;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Object called by the JNI code when a message arrives on an object path. A dispatcher will contain a map of dbus interfaces it can dispatch to.
 * each interface name will contain a list of criteria it can match, when a criteria is matched, it will look it up in another map whcih contains
 * to which handler method the criteria correspond.
 *
 * When all of this is done and that a match is found, it will try unserialize the message and call the method. If the method is a DBus method call
 * it will also wait for the return value and send it to the event loop to return to the caller.
 *
 * As dispatcher registration is asynchronous, there is a synchronization mechanism that the DBus class will use to make the call to addHandler
 * block until the dispatcher is effectively registered.
 */
public class Dispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

    /**
     * Is the dispatcher effectively registered to dbus, or is it waiting to be
     */
    private volatile boolean isRegistered = false;

    /**
     * Barrier to which await if needed
     */
    private CountDownLatch barrier = new CountDownLatch(1);

    /**
     * Object path of the dispatcher
     */
    private String path;

    /**
     * Map of all the available interfaces and their available criteria
     */
    private HashMap<String,ArrayList<Criteria>> handlersCriterias;

    /**
     * Map of all the criteria and their handler methods
     */
    private HashMap<Criteria,HandlerMethod> handlers;

    /**
     * Event loop to which send calls responses
     */
    private EventLoop eventLoop;

    /**
     * Create a new dispatcher bound to the given object path
     *
     * @param path object path of the dispatcher
     * @param eventLoop event loop to which send the calls replies
     */
    public Dispatcher(String path, EventLoop eventLoop){
        this.path = path;
        this.handlersCriterias = new HashMap<>();
        this.handlers = new HashMap<>();
        this.eventLoop = eventLoop;
    }

    /**
     * Add a criteria for the given interface to the dispatcher. The dispatcher will check if the given criteria do not conflict with an already
     * registered criteria and throw if it does
     *
     * @param interfaceName interface to which bind the criteria
     * @param criteria criteria to register
     * @param handlerMethod handler method to call when the criteria is matched
     */
    public void addCriteria(String interfaceName, Criteria criteria, HandlerMethod handlerMethod){
        ArrayList<Criteria> interfaceCriterias = this.handlersCriterias.get(interfaceName);

        //if the interface was never sued before, create its list of criteria
        if(interfaceCriterias == null){
            interfaceCriterias = new ArrayList<>();
            this.handlersCriterias.put(interfaceName,interfaceCriterias);
        }

        //check for conflicts
        if(interfaceCriterias.contains(criteria)) throw new IllegalArgumentException("The criteria "+criteria+" is conflicting with an already registered criteria");

        //add the criteria and its handler method
        interfaceCriterias.add(criteria);
        this.handlers.put(criteria,handlerMethod);
    }

    /**
     * Method called by the JNI call when a message should be dispatched. The msgPointer parameter is nullable, if it is null then it means the message is a signal,
     * else it is a method call to which we should reply. When the message is a call the method will return true if the message was dispatched, false if there is
     * no handler registered for the criteria, by doing this Dbus can return a standard error to the caller telling the member could not be found
     *
     * @param args pre-unserialized message
     * @param interfaceName interface on which the message was received
     * @param member member of the message
     * @param msgPointer pointer to the message we should reply to, can be 0
     * @return did the dispatcher handle the message
     */
    public boolean dispatch(DBusObject args, String interfaceName, String member, long msgPointer) {
        LOG.debug("Dispatcher {} received a message for {}.{}({})",this.path,interfaceName,member,args.getSignature());
        //get the list of criteria for the given interface and return false if nothing is found
        ArrayList<Criteria> availableHandlers = this.handlersCriterias.get(interfaceName);
        if(availableHandlers == null) return false;

        //generate the criteria from the message
        Criteria requestCriteria;
        if(msgPointer == 0){
            requestCriteria = new Criteria(member,args.getSignature(),null, MemberType.SIGNAL);
        }else{
            requestCriteria = new Criteria(member,args.getSignature(),null, MemberType.METHOD);
        }

        //try to find a matching criteria
        for(Criteria c: availableHandlers){
            if(c.equals(requestCriteria)){
                LOG.debug("Dispatcher found a handler, trying to unserialize");
                //match found, try to unserialize
                HandlerMethod handler = this.handlers.get(c);
                try{
                    Serializable param;
                    //if the message is empty, use the special EMPTY message
                    if(args.getSignature().equals("")){
                        param = Message.EMPTY;
                    }else{
                        param = handler.getInputType().newInstance();
                        param.unserialize(args);
                    }
                    //get the return value of the handler, and if not null send a reply
                    Object returnObject = handler.call(param);
                    if(returnObject != null && msgPointer != 0){
                        if(returnObject instanceof Message){
                            LOG.debug("Handler returned a result, dispatch the reply: {}",returnObject.toString());
                            this.eventLoop.send(new ReplySendingRequest(((Message)returnObject).serialize(),msgPointer,interfaceName,member));
                        }else if(returnObject instanceof Promise){
                            LOG.debug("Handler returned a promise, set the message pointer and proceed");
                            ((Promise) returnObject).setMessagePointer(msgPointer,interfaceName,member,this.eventLoop);
                        }else{
                            LOG.error("The handler returned an unknown result ({}), no reply will be sent",returnObject);
                        }
                    }else if(msgPointer != 0){
                        LOG.error("The handler returned a null object, no reply will be sent");
                    }
                }catch (Exception e){
                    //if the message is a call, reply an error
                    if(msgPointer != 0){
                        this.eventLoop.send(new ErrorReplySendingRequest(e.getCause(),msgPointer, interfaceName, member));
                    }else{
                        LOG.warn("An exception was raised during signal handling",e);
                    }
                }
                return true;
            }
        }
        //no handler was found
        return false;
    }

    /**
     * Called by the event loop when the dispatcher is effectively registered. It will update its state and unlock the barrier
     */
    public void setAsRegistered(){
        this.isRegistered = true;
        this.barrier.countDown();
    }

    /**
     * await on the synchronization barrier
     * @throws InterruptedException thrown when the registration is interrupted
     */
    public void awaitRegistration() throws InterruptedException {
        this.barrier.await();
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public String getPath() {
        return path;
    }
}
