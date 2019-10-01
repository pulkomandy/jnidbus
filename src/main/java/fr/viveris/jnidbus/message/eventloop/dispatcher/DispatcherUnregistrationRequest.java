package fr.viveris.jnidbus.message.eventloop.dispatcher;

import fr.viveris.jnidbus.dispatching.Dispatcher;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;

public class DispatcherUnregistrationRequest extends AbstractDispatcherRequest {
    public DispatcherUnregistrationRequest(Dispatcher dispatcher, RequestCallback callback) {
        super(dispatcher,callback);
    }
}
