package fr.viveris.jnidbus.message.sendingrequest;

import fr.viveris.jnidbus.serialization.DBusObject;

/**
 * Represent a reply to be sent
 */
public class ReplySendingRequest extends AbstractSendingRequest {
    long messagePointer;

    public ReplySendingRequest(DBusObject message, long messagePointer) {
        super(message);
        this.messagePointer = messagePointer;
    }

    public long getMessagePointer() {
        return messagePointer;
    }
}