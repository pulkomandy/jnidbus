package fr.viveris.jnidbus;

import fr.viveris.jnidbus.bindings.bus.Connection;
import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.cache.SignalMetadata;
import fr.viveris.jnidbus.dispatching.Criteria;
import fr.viveris.jnidbus.dispatching.Dispatcher;
import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.HandlerMethod;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.exception.ConnectionException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.sendingrequest.SignalSendingRequest;
import fr.viveris.jnidbus.remote.RemoteObjectInterceptor;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.serialization.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Public API of the library. contains all the primitives to interact with Dbus.
 */
public class Dbus implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Dbus.class);
    /**
     * First of all, load the JNI library
     */
    static{
        //when running in a tomcat env, it is advised to use the Library.loadLibrary method to allow multiple webapp
        //to use the shared library without any overhead for the developer, but we don't want to make tomcat a dependency
        //of the library, so we use reflection to detect if we have the ability to load the library with tomcat
        try {
            Class<?> clazz = Dbus.class.getClassLoader().loadClass("org.apache.tomcat.jni.Library");
            LOG.debug("Tomcat detected, trying to use tomcat-jni to load shared library");
            clazz.getDeclaredMethod("loadLibrary",String.class).invoke("jnidbus");
        } catch (Exception e) {
            LOG.info("Loading shared library using System.loadLibrary()");
            System.loadLibrary("jnidbus");
        }
    }

    /**
     * DBus connection object
     */
    private Connection connection;

    /**
     * Event loop to which send the messages
     */
    private EventLoop eventLoop;

    /**
     * List of the registered dispatchers mapped by their object path
     */
    private HashMap<String, Dispatcher> dispatchers;

    /**
     * Create a new DBus connection with the given type and name.
     *
     * @param type bus type, please refer to the DBus documentation for more info on them
     * @param busName bus name, should respect the Dbus name format (similar to the java namespace format)
     * @param busAddress path the dbus daemon is listening on, if null is passed, libdbus will fall back to the
     *                   DBUS_SESSION_BUS_ADDRESS environment variable
     * @throws ConnectionException thrown id something goes wrong with dbus (bus name already in used, bus unavailable, etc...)
     */
    public Dbus(BusType type, String busName,String busAddress) throws ConnectionException {
        this.connection = Connection.createConnection(type,busName,busAddress);
        this.eventLoop = new EventLoop(this.connection);
        this.dispatchers = new HashMap<>();
        LOG.info("DBus successfully connected and bound to the bus {} ",busName);
    }

    public Dbus(BusType type, String busName) throws ConnectionException {
        this(type,busName,null);
    }

    /**
     * Add a handler object, this method will create and register a new dispatcher if needed. All the Messages classes used
     * by the object will be checked and cached in the process, allowing the developer to quickly detect mapping mistakes.
     *
     * If one of the given handler object methods clash with another handler method, an exception will be thrown
     *
     * @param handler handler to register
     */
    public void addHandler(GenericHandler handler){
        LOG.info("Adding DBus handler {}",handler.getClass().getName());
        //get annotation
        Handler handlerAnnotation = handler.getHandlerAnnotation();
        if(handlerAnnotation == null) throw new IllegalStateException("The given handler does not have the Handler annotation");

        //get all criteria provided by this handler
        HashMap<Criteria, HandlerMethod> criterias = handler.getAvailableCriterias();

        //try to get the dispatcher, if no dispatcher are found, create one
        Dispatcher dispatcher = this.dispatchers.get(handlerAnnotation.path());
        boolean dispatcherCreated = false;
        if(dispatcher == null){
            LOG.debug("No dispatcher found for the given object path, creating one");
            dispatcher = new Dispatcher(handlerAnnotation.path(),this.eventLoop);
            dispatcherCreated = true;
        }

        //put the criteria in the dispatcher
        for(Criteria c : criterias.keySet()){
            LOG.debug("Adding criteria for {}.{}({}) to dispatcher",handlerAnnotation.interfaceName(),c.getMember(),c.getInputSignature());
            dispatcher.addCriteria(handlerAnnotation.interfaceName(),c,criterias.get(c));
        }

        //if the dispatcher was created, register it to Dbus a block until it is effectively registered
        if(dispatcherCreated){
            LOG.debug("Adding dispatcher to event loop and register object path {}",handlerAnnotation.path());
            this.eventLoop.addPathHandler(dispatcher);
            try {
                dispatcher.awaitRegistration();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Dispatcher registration was interrupted");
            }
            this.dispatchers.put(handlerAnnotation.path(),dispatcher);
        }
    }

    public void removeHandler(GenericHandler handler){
        //get annotation
        Handler handlerAnnotation = handler.getHandlerAnnotation();
        if(handlerAnnotation == null) throw new IllegalStateException("The given handler does not have the Handler annotation");

        Dispatcher dispatcher = this.dispatchers.get(handlerAnnotation.path());
        if(dispatcher == null) return;

        //get all criteria provided by this handler
        HashMap<Criteria, HandlerMethod> criterias = handler.getAvailableCriterias();

        //remove them
        for(Criteria c : criterias.keySet()){
            LOG.debug("Removing criteria for {}.{}({}) to dispatcher",handlerAnnotation.interfaceName(),c.getMember(),c.getInputSignature());
            dispatcher.removeCriteria(handlerAnnotation.interfaceName(),c,criterias.get(c));
        }

        //unregister dispatcher if there is no more handlers
        if(dispatcher.isEmpty()){
            this.eventLoop.removePathHandler(dispatcher);
        }

    }

    /**
     * Create a new remote object instance which annotated methods will be translated into DBus calls and signals. The
     * given class must be annotated with the RemoteInterface annotation and all of its methods must be annotated with
     * the RemoteMember annotation. The created instance is a Java proxy registered to the current DBus instance ClassLoader.
     *
     * All the methods of the given interface should return either a Promise of a Message or void
     *
     * @param destinationBus bus name on which the object methods will eb called
     * @param objectPath object path on the bus
     * @param objectInterface java interface representing the remote object
     * @param <T> java interface type
     * @return proxy instance which will perform the calls
     */
    @SuppressWarnings("unchecked")
    public <T> T createRemoteObject(String destinationBus, String objectPath, Class<T> objectInterface){
        return (T) Proxy.newProxyInstance(objectInterface.getClassLoader(),new Class[]{objectInterface},new RemoteObjectInterceptor(destinationBus,objectPath,objectInterface,this.eventLoop));
    }

    /**
     * Send the given signal isntance on the given object path
     * @param objectPath
     * @param signal
     */
    public void sendSignal(String objectPath, Signal signal){
        SignalMetadata meta = RemoteObjectInterceptor.getFromCache(signal);
        this.eventLoop.send(new SignalSendingRequest(signal.getParam().serialize(),objectPath,meta.getInterfaceName(),meta.getMember()));
    }

    /**
     * Empty all the internal JNIDBus cache, usefull when doing hot reload to prevent ClassLoader leaks
     */
    public static void clearCache(){
        Message.clearCache();
        RemoteObjectInterceptor.clearCache();
        Signature.clearCache();
    }


    @Override
    public void close() throws Exception {
        LOG.info("Closing DBus connection and unregister bus name {}",this.connection.getBusName());
        this.eventLoop.close();
    }
}
