/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message.eventloop.sending;

import fr.viveris.jnidbus.message.eventloop.EventLoopRequest;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;
import fr.viveris.jnidbus.serialization.DBusObject;

/**
 * Represent a message waiting to be sent to Dbus
 */
public abstract class AbstractSendingRequest extends EventLoopRequest {
    private DBusObject message;

    public AbstractSendingRequest(DBusObject message, RequestCallback callback) {
        super(callback);
        this.message = message;
    }

    public DBusObject getMessage() {
        return message;
    }
}
