package fr.viveris.jnidbus.message.eventloop;

import fr.viveris.jnidbus.message.PendingCall;

/**
 * Request made when a PendingCall listener needs to be executed on the event loop
 */
public class PendingCallRedispatchRequest extends EventLoopRequest {
    PendingCall pendingCall;

    public PendingCallRedispatchRequest(PendingCall pendingCall,RequestCallback callback) {
        super(callback);
        this.pendingCall = pendingCall;
    }

    public PendingCall getPendingCall() {
        return pendingCall;
    }
}
