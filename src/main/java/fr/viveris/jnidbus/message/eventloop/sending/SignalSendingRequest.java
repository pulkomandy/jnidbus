/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message.eventloop.sending;

import fr.viveris.jnidbus.message.eventloop.RequestCallback;
import fr.viveris.jnidbus.serialization.DBusObject;

/**
 * Represent a signal to be sent
 */
public class SignalSendingRequest extends AbstractSendingRequest{
    private String path;
    private String interfaceName;
    private String member;

    public SignalSendingRequest(DBusObject message, String path, String interfaceName, String member, RequestCallback callback) {
        super(message,callback);
        this.path = path;
        this.interfaceName = interfaceName;
        this.member = member;
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
}
