package fr.viveris.jnidbus.message.eventloop;

import fr.viveris.jnidbus.message.PendingCall;

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
