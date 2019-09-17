package fr.viveris.jnidbus.bindings.bus;

import fr.viveris.jnidbus.dispatching.Dispatcher;
import fr.viveris.jnidbus.exception.ClosedEventLoopException;
import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.exception.EventLoopSetupException;
import fr.viveris.jnidbus.message.PendingCall;
import fr.viveris.jnidbus.message.sendingrequest.*;
import fr.viveris.jnidbus.serialization.DBusObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The event loop is the core of the library, it will be responsible for sending, receiving and waiting for events. The whole object is thread safe and communication
 * between this class and other threads is done using Queues. This allows us to easily implement a safe E.L without having to deal with low level and error prone
 * wait/notify mechanism.
 *
 * We have to proceed like this as the event loop must not be waiting for event when writing because Dbus might want to listen for write events in order to send
 * efficiently. In addition there is some synchronization around the wakeup call, which will unblock the tick() call, this synchronization is made in order to
 * avoid extra calls to wakeup() which could cause the loop to spin faster than it needs to.
 *
 * Any call made to the event loop while it has not started will block until the event loop started or failed to start. This class is also responsible for closing
 * the underlying DBus connection as we want to make sure all the pending events are processed before closing.
 */
public class EventLoop implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoop.class);
    /**
     * Limit the number of send by tick to avoid event loop stall
     */
    public static final int MAX_SEND_PER_TICK = 128;

    /**
     * Pointer to the native context, this must not be modified by any java code
     */
    private long dBusContextPointer;

    /**
     * Event loop state
     */
    private AtomicBoolean isClosed = new AtomicBoolean(true);

    /**
     * used by the JNI to synchronize wakeup call with the event loop state
     */
    private final Object wakeupLock = new Object();

    /**
     * Atomic state telling the caller if a call to wakeup is needed for the vent loop to process its call
     */
    volatile private boolean shouldWakeup = false;

    /**
     * Thread running the event loop
     */
    private Thread thread;

    /**
     * Queue of dispatchers to register to Dbus
     */
    private ConcurrentLinkedDeque<Dispatcher> handlerAddingQueue = new ConcurrentLinkedDeque<>();

    /**
     * Queue of dispatchers to register to Dbus
     */
    private ConcurrentLinkedDeque<Dispatcher> handlerRemovingQueue = new ConcurrentLinkedDeque<>();

    /**
     * Queue of sending request. A sending request represent any type of message we want to send through DBus (signal, calls, method return, errors)
     */
    private ConcurrentLinkedDeque<AbstractSendingRequest> signalSendingQueue = new ConcurrentLinkedDeque<>();

    /**
     * Queue containing the PendingCall that received the result before the listener was set and that need to dispatch the
     * listener notification on the event loop
     */
    private ConcurrentLinkedDeque<PendingCall> redispatchRequestQueue = new ConcurrentLinkedDeque<>();

    /**
     * Java class representing the Dbus connection
     */
    private Connection connection;

    /**
     * synchronization barrier used to ensure the event loop has fully started (or failed) before accepting calls
     */
    private CountDownLatch startBarrier = new CountDownLatch(1);

    /**
     * Exception raised during initialization
     */
    private EventLoopSetupException setupException = null;

    /**
     * Launch a new event loop for the given connection. The constructor will create the thread hosting the event loop and return.
     * any exception thrown during the event loop initialization will be stored as a property of the event loop and thrown each time
     * a call is made to it.
     * @param connection connection the event loop will be bound to
     */
    public EventLoop(Connection connection){
        this.connection = connection;
        this.dBusContextPointer = connection.getDbusContextPointer();
        LOG.debug("Starting DBus event loop for bus {}",this.connection.getBusName());
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EventLoop.this.run();
                } catch (EventLoopSetupException e) {
                    LOG.error("Exception in event loop {} : ",EventLoop.this.connection.getBusName(),e);
                    EventLoop.this.setupException = e;
                    EventLoop.this.isClosed.set(true);
                    EventLoop.this.startBarrier.countDown();
                    EventLoop.this.connection.close();
                }
            }
        });
        this.thread.setName("DBus event loop (bus="+this.connection.getBusName()+")");
        this.thread.start();
    }

    /**
     * Initialize epoll and its wakeup file descriptor. It will also register watch functions to Dbus and populate the native context
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     */
    private native boolean setup(long contextPtr) throws EventLoopSetupException;

    /**
     * A tick will trigger a call to epoll_wait (using the given timeout parameter), if an event arrives it will be dispatched to Dbus
     * watches, which will parse and dispatch messages to the correct JVM dispatcher. This call is blocking and will return only if
     * events where processed, if a wakeup call was made or if the timeout has expired.
     *
     * @param timeout timeout for the call, -1 means it will never timeout, 0 means epoll_wait will return instantly, any other value
     *                will make epoll_wait block the the given number of milliseconds
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     */
    private native void tick(long contextPtr, int timeout);

    /**
     * Write arbitrary data to the wakeup file descriptor, which will unblock any ongoing tick() call
     *
     @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
      *                 for performances purposes
     */
    public native void wakeup(long contextPtr);

    /**
     * Send a reply to a received call
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     * @param msg pre-serialized object that will be transferred into the native dbus message
     * @param msgPointer pointer to the message we reply to
     */
    private native void sendReply(long contextPtr, DBusObject msg, long msgPointer);

    /**
     * Send an error back to the caller
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     * @param msgPointer pointer to the message the error relates to
     * @param errorName name of the error. This name should respect the dbus error name format (which is the same as Java's namespace format)
     * @param errorMessage message of the error
     */
    private native void sendReplyError(long contextPtr, long msgPointer, String errorName, String errorMessage);

    /**
     * Send a signal on the bus
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     * @param path dbus object path of the signal
     * @param interfaceName dbus interface of the signal
     * @param member name of the signal
     * @param msg pre-serialized object that will be transferred into the native dbus message
     */
    private native void sendSignal(long contextPtr, String path, String interfaceName, String member, DBusObject msg);

    /**
     * fr.viveris.jnidbus.Call a dbus distant method. Calls are always made asynchronously and the result will eb given to the PendingCall
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     * @param path dbus object path of the call
     * @param interfaceName dbus interface of the call
     * @param member member of the dbus interface
     * @param msg pre-serialized object that will be transferred into the native dbus message
     * @param dest destination bus to which send the message
     * @param pendingCall listener that will be notified when the call state changes
     */
    private native void sendCall(long contextPtr, String path, String interfaceName, String member, DBusObject msg, String dest, PendingCall pendingCall);

    /**
     * register an object path handler to dbus which will dispatch any message on the given object path to the given JVM dispatcher. The dispatcher
     * will receive all the message for this object path, there is currently no mechanism to filter out what the dispatcher will receive, which can
     * lead to unwanted message being processed by dbus but this has no effect on the JVM code. We might change this later
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     * @param path dbus object path we want to register to
     * @param handler handler to call when messages arrive on the object path
     */
    private native void addPathHandler(long contextPtr, String path, Dispatcher handler);

    /**
     * Unregister a path, thus destroying its dispatcher
     *
     * @param contextPtr pointer to the native context, its better to give it as an argument rather than fetching it from the native
     *                   for performances purposes
     * @param path dbus object path we want to unregister
     */
    private native void removePathHandler(long contextPtr, String path);

    private void run() throws EventLoopSetupException {
        //setup the event loop
        this.setup(this.dBusContextPointer);
        //if everything went fine, we can update the state and unlock the barrier. there is a slight data race here as a call arriving
        //just after the state update will be processed before the calls blocked by the barrier at that time
        this.isClosed.set(false);
        this.startBarrier.countDown();

        LOG.debug("Event loop {} successfully started",this.connection.getBusName());

        //tick while the event loop is valid
        while(!this.isClosed.get()){
            //from now messages added to the queue by other threads may or may not be added before the tick() call, so we
            //update the flag to make the caller call wakeup
            this.shouldWakeup = true;

            //add dispatchers
            Dispatcher d;
            while((d = this.handlerAddingQueue.poll()) != null){
                this.addPathHandler(this.dBusContextPointer,d.getPath(),d);
                //notify the dispatcher it has been registered
                d.setAsRegistered();
                LOG.debug("Event loop {} successfully registered the dispatcher for the path {}",this.connection.getBusName(),d.getPath());
            }

            //remove dispatchers
            while((d = this.handlerRemovingQueue.poll()) != null){
                this.removePathHandler(this.dBusContextPointer,d.getPath());
                LOG.debug("Event loop {} successfully unregistered the dispatcher for the path {}",this.connection.getBusName(),d.getPath());
            }

            //execute redispatch requests
            PendingCall p;
            while ((p = this.redispatchRequestQueue.poll()) != null){
                p.forceNotification();
            }

            int i = 0;
            AbstractSendingRequest abstractRequest;
            while((abstractRequest = this.signalSendingQueue.poll()) != null){
                if(abstractRequest instanceof CallSendingRequest){
                    CallSendingRequest req = (CallSendingRequest)abstractRequest;
                    LOG.debug("Sending DBus call {}.{}({}) on path {} for the bus {}",req.getInterfaceName(),req.getMember(),req.getMessage().getSignature(),req.getPath(),req.getDest());
                    this.sendCall(this.dBusContextPointer,req.getPath(),req.getInterfaceName(),req.getMember(),req.getMessage(),req.getDest(),req.getPendingCall());

                }else if(abstractRequest instanceof ErrorReplySendingRequest){
                    ErrorReplySendingRequest req = (ErrorReplySendingRequest)abstractRequest;
                    LOG.debug("Sending DBus error {} thrown form {}.{}",req.getError().toString(),req.getInterfaceName(),req.getMember());
                    req.getError().printStackTrace();
                    if(req.getError() instanceof DBusException){
                        DBusException cast = (DBusException)req.getError();
                        this.sendReplyError(this.dBusContextPointer,req.getMessagePointer(),cast.getCode(),cast.getMessage());
                    }else{
                        this.sendReplyError(this.dBusContextPointer,req.getMessagePointer(),req.getError().getClass().getName(),req.getError().getMessage());
                    }

                }else if(abstractRequest instanceof ReplySendingRequest){
                    ReplySendingRequest req = (ReplySendingRequest)abstractRequest;
                    LOG.debug("Sending DBus reply form {}.{}({})",req.getInterfaceName(),req.getMember(),req.getMessage().getSignature());
                    this.sendReply(this.dBusContextPointer,req.getMessage(),req.getMessagePointer());

                }else if(abstractRequest instanceof SignalSendingRequest){
                    SignalSendingRequest req = (SignalSendingRequest)abstractRequest;
                    LOG.debug("Sending DBus signal {}.{}({}) on path {}",req.getInterfaceName(),req.getMember(),req.getMessage().getSignature(),req.getPath());
                    this.sendSignal(this.dBusContextPointer,req.getPath(),req.getInterfaceName(),req.getMember(),req.getMessage());
                }
                if(++i >= MAX_SEND_PER_TICK){ break; }
            }

            //launch tick
            this.tick(this.dBusContextPointer,-1);
        }

        this.connection.close();
    }

    /**
     * Register a dispatcher to Dbus, this method is asynchronous and the dispatcher will be processed during the next tick. When the dispatcher
     * is successfully registered, its setAsRegistered method will be called
     * @param dispatcher dispatcher to register to dbus
     */
    public void addPathHandler(Dispatcher dispatcher){
        this.checkEventLoop();
        this.handlerAddingQueue.add(dispatcher);
        this.wakeupIfNeeded();
    }

    /**
     *
     * @param dispatcher dispatcher to unregister
     */
    public void removePathHandler(Dispatcher dispatcher){
        this.checkEventLoop();
        this.handlerRemovingQueue.add(dispatcher);
        this.wakeupIfNeeded();
    }

    /**
     * Asynchronously send a message to dbus.
     * @param request sending request we want the event loop to process
     */
    public void send(AbstractSendingRequest request){
        this.checkEventLoop();
        this.signalSendingQueue.add(request);
        this.wakeupIfNeeded();
    }

    public void redispatch(PendingCall call){
        this.checkEventLoop();
        this.redispatchRequestQueue.add(call);
        this.wakeupIfNeeded();
    }

    /**
     * Utility method checking the state of the event loop. It will throw an exception fi the loop failed to start or is closed. This method will
     * also block during the event loop initialization sa any call made during this intermediate state will be executed at the right time
     */
    private void checkEventLoop(){
        if(this.isClosed.get()){
            try { this.startBarrier.await(); } catch (InterruptedException e) { }
            if(this.setupException != null) throw new IllegalStateException("The event loop failed to start",this.setupException);
            if(this.isClosed.get()) throw new ClosedEventLoopException("The event loop is closed");
        }
    }

    /**
     * Utility method that will make a wakeup call according to the shouldWakeup flag and if a call was made, update the flag to false
     * so the next calls avoid doing it
     */
    private void wakeupIfNeeded(){
        synchronized (this.wakeupLock){
            if(shouldWakeup){
                this.wakeup(this.dBusContextPointer);
                this.shouldWakeup = false;
            }
        }
    }

    /**
     * Close the event loop. During the event loop shutdown, the close method will be called on the Connection object
     * @throws IOException never thrown
     */
    @Override
    public void close() throws IOException {
        this.checkEventLoop();
        this.isClosed.set(true);
        this.wakeup(this.dBusContextPointer);
    }

}
