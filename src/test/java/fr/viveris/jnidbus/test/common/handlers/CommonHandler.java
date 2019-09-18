/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.handlers;

import fr.viveris.jnidbus.dispatching.GenericHandler;

import java.util.concurrent.CountDownLatch;

public abstract class CommonHandler<T> extends GenericHandler {
    protected CountDownLatch barrier = new CountDownLatch(1);
    protected T value;

    public CountDownLatch getBarrier() {
        return barrier;
    }

    public T getValue() {
        return value;
    }
}
