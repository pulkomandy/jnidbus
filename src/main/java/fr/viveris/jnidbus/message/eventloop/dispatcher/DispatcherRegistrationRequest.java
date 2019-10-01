package fr.viveris.jnidbus.message.eventloop.dispatcher;

import fr.viveris.jnidbus.dispatching.Dispatcher;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;

public class DispatcherRegistrationRequest extends AbstractDispatcherRequest {
    public DispatcherRegistrationRequest(Dispatcher dispatcher, RequestCallback callback) {
        super(dispatcher,callback);
    }
}
