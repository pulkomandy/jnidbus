/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message.eventloop.sending;

import fr.viveris.jnidbus.message.DBusPromise;
import fr.viveris.jnidbus.message.Promise;
import fr.viveris.jnidbus.message.eventloop.RequestCallback;
import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.serialization.Serializable;

/**
 * Represent a dbus call waiting to be sent
 */
public class CallSendingRequest<T extends Serializable> extends AbstractSendingRequest {
    private String path;
    private String interfaceName;
    private String member;
    private String dest;
    private DBusPromise<T> promise;

    public CallSendingRequest(DBusObject message, String path, String interfaceName, String member, String dest, DBusPromise<T> promise, RequestCallback callback) {
        super(message,callback);
        this.path = path;
        this.interfaceName = interfaceName;
        this.member = member;
        this.dest = dest;
        this.promise = promise;
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

    public DBusPromise<T> getPromise() {
        return promise;
    }
}
