/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common;

import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.message.Promise;
import fr.viveris.jnidbus.serialization.Serializable;

import java.util.concurrent.CountDownLatch;

public class Listener<T extends Serializable> implements Promise.Callback<T> {
    private CountDownLatch barrier = new CountDownLatch(1);
    private DBusException t = null;
    private T value = null;

    @Override
    public void value(T value,Exception e) {
        this.value = value;
        this.t = (DBusException) e;
        barrier.countDown();
    }

    public CountDownLatch getBarrier() {
        return barrier;
    }

    public DBusException getT() {
        return t;
    }

    public T getValue() {
        return value;
    }
}
