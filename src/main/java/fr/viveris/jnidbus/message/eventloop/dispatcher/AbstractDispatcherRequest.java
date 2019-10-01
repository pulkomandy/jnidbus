package fr.viveris.jnidbus.message.eventloop.dispatcher;

import fr.viveris.jnidbus.dispatching.Dispatcher;
import fr.viveris.jnidbus.message.eventloop.EventLoopRequest;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;

/**
 * Abstract class representing a register or unregister of dispatcher to the event loop
 */
public abstract class AbstractDispatcherRequest extends EventLoopRequest {
    private Dispatcher dispatcher;

    public AbstractDispatcherRequest(Dispatcher dispatcher, RequestCallback callback) {
        super(callback);
        this.dispatcher = dispatcher;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }
}
