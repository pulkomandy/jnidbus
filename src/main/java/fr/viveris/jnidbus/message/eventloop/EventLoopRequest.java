package fr.viveris.jnidbus.message.eventloop;

public abstract class EventLoopRequest {
    private RequestCallback callback;

    public EventLoopRequest(RequestCallback callback){
        this.callback = callback;
    }

    public RequestCallback getCallback() {
        return callback;
    }
}
