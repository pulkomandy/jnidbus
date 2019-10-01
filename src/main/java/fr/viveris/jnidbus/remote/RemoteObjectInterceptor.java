/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.remote;

import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.cache.Cache;
import fr.viveris.jnidbus.cache.RemoteObjectMetadata;
import fr.viveris.jnidbus.cache.SignalMetadata;
import fr.viveris.jnidbus.exception.RemoteObjectCheckException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.PendingCall;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;
import fr.viveris.jnidbus.message.eventloop.sending.CallSendingRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RemoteObjectInterceptor implements InvocationHandler {

    /**
     * Cache containing the reflection data for the methods and signal of the interface,, we don't need to
     * make the cache static as
     */
    private static final Cache<Method, RemoteObjectMetadata> METHOD_CACHE = new Cache<>();

    private static final Cache<Class<? extends Signal>, SignalMetadata> SIGNAL_CACHE = new Cache<>();

    private String destinationBus;
    private String objectPath;
    private String interfaceName;

    private EventLoop eventLoop;

    /**
     * Create an interceptor for the given interface, it will check all the methods of the interface and cache them for
     * later use
     *
     * @param destinationBus bus name on which the remote object is reachable
     * @param objectPath object path on the bus
     * @param remoteObjectClass Java interface class representing the object
     * @param eventLoop event loop on which we should dispatch the methods calls
     */
    public RemoteObjectInterceptor(String destinationBus, String objectPath, Class<?> remoteObjectClass, EventLoop eventLoop){
        this.destinationBus = destinationBus;
        this.objectPath = objectPath;
        this.eventLoop = eventLoop;

        RemoteInterface interfaceName = remoteObjectClass.getAnnotation(RemoteInterface.class);
        if(interfaceName == null) throw new IllegalArgumentException("The given interface is not annotated with RemoteInterface");
        this.interfaceName = interfaceName.value();

        //check class
        for(Method m : remoteObjectClass.getDeclaredMethods()){
            //check method and put in cache
            RemoteObjectInterceptor.getFromCache(m);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        //if there is no parameter, it means we have an empty call
        Message msg;
        if(objects == null){
            msg = Message.EMPTY;
        }else{
            msg = (Message) objects[0];
        }

        //get metadata from cache
        RemoteObjectMetadata meta = RemoteObjectInterceptor.getFromCache(method);

        //create a wildcard pendingCall, this wont fail as the method was checked and return the correct type
        final PendingCall pendingCall = new PendingCall(meta.getOutputMetadata().getMessageClass(),this.eventLoop);

        //send call and return the PendingCall, set the request callback to fail the pending call if the sending failed
        this.eventLoop.send(new CallSendingRequest(msg.serialize(),
                this.objectPath,
                this.interfaceName,
                meta.getMember(),
                this.destinationBus,
                pendingCall,
                new RequestCallback() {
                    @Override
                    public void call(Exception e) {
                        if(e != null) pendingCall.fail(e.getClass().getName(),"The call could not be sent");
                    }
                }));

        return pendingCall;
    }

    private static RemoteObjectMetadata getFromCache(Method method){
        if (METHOD_CACHE.getCachedEntity(method) != null) return METHOD_CACHE.getCachedEntity(method);

        //the method is not in cache, build the cache entity, add it and return
        try {
            RemoteObjectMetadata meta = new RemoteObjectMetadata(method);
            METHOD_CACHE.addCachedEntity(method,meta);
            return meta;
        } catch (RemoteObjectCheckException remoteObjectCheckException) {
            throw new IllegalStateException("The remote object method: "+method.getName()+" is invalid", remoteObjectCheckException);
        }
    }

    public static SignalMetadata getFromCache(Signal signal){
        SignalMetadata meta = SIGNAL_CACHE.getCachedEntity(signal.getClass());
        if (meta == null){
            meta = new SignalMetadata(signal);
            SIGNAL_CACHE.addCachedEntity(signal.getClass(),meta);
        }
        return meta;
    }

    public static void clearCache(){
        SIGNAL_CACHE.clear();
        METHOD_CACHE.clear();
    }
}
