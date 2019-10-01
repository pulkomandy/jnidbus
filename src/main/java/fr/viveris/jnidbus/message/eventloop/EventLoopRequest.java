package fr.viveris.jnidbus.message.eventloop;

/**
 * An EventLoopRequest is used to ask the event loop to call native Dbus operations in a thread-safe manner. All requests
 * come with a callback that can be null if the request is a fire-and-forget.
 */
public abstract class EventLoopRequest {
    private RequestCallback callback;

    public EventLoopRequest(RequestCallback callback){
        this.callback = callback;
    }

    public RequestCallback getCallback() {
        return callback;
    }
}
