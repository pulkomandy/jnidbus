/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message.eventloop.sending;

import fr.viveris.jnidbus.message.eventloop.RequestCallback;
import fr.viveris.jnidbus.serialization.DBusObject;

/**
 * Represent a reply to be sent
 */
public class ReplySendingRequest extends AbstractSendingRequest {
    long messagePointer;

    /**
     * For debug purposes
     */
    private String interfaceName;
    private String member;

    public ReplySendingRequest(DBusObject message, long messagePointer, String interfaceName, String member, RequestCallback callback) {
        super(message,callback);
        this.messagePointer = messagePointer;
        this.interfaceName = interfaceName;
        this.member = member;
    }

    public long getMessagePointer() {
        return messagePointer;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMember() {
        return member;
    }
}
