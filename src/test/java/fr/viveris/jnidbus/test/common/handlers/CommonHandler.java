/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.handlers;

import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.serialization.Serializable;

import java.util.concurrent.CountDownLatch;

public abstract class CommonHandler<T extends Serializable> extends GenericHandler {
    private CountDownLatch barrier = new CountDownLatch(1);
    private T value;

    public CountDownLatch getBarrier() {
        return barrier;
    }

    protected void doHandle(T value){
        this.value = value;
        this.barrier.countDown();
    }

    public T getValue() {
        return value;
    }

    public abstract Signal<T> buildSignal(T value);
}
