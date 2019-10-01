/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message.eventloop.sending;

import fr.viveris.jnidbus.message.PendingCall;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;
import fr.viveris.jnidbus.serialization.DBusObject;

/**
 * Represent a dbus call waiting to be sent
 */
public class CallSendingRequest extends AbstractSendingRequest {
    private String path;
    private String interfaceName;
    private String member;
    private String dest;
    private PendingCall pendingCall;

    public CallSendingRequest(DBusObject message, String path, String interfaceName, String member, String dest, PendingCall pendingCall, RequestCallback callback) {
        super(message,callback);
        this.path = path;
        this.interfaceName = interfaceName;
        this.member = member;
        this.dest = dest;
        this.pendingCall = pendingCall;
    }

    public String getPath() {
        return path;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMember() {
        return member;
    }

    public String getDest() {
        return dest;
    }

    public PendingCall getPendingCall() {
        return pendingCall;
    }
}
