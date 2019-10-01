/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message.eventloop.sending;

import fr.viveris.jnidbus.message.eventloop.RequestCallback;

/**
 * Represent an error to be sent in reply to a call
 */
public class ErrorReplySendingRequest extends AbstractSendingRequest {
    private Throwable error;
    private long messagePointer;

    /**
     * For debug purposes
     */
    private String interfaceName;
    private String member;

    public ErrorReplySendingRequest(Throwable error, long messagePointer, String interfaceName, String member, RequestCallback callback) {
        super(null,callback);
        this.error = error;
        this.messagePointer = messagePointer;
        this.interfaceName = interfaceName;
        this.member = member;
    }

    public Throwable getError() {
        return error;
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
