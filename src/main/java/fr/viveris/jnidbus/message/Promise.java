/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.message;

import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.message.eventloop.sending.ErrorReplySendingRequest;
import fr.viveris.jnidbus.message.eventloop.sending.ReplySendingRequest;
import fr.viveris.jnidbus.serialization.Serializable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-blocking implementation of a promise, the thread safety is assured by AtomicReference and a spin-wait loop.
 * You can specify the callback execution thread by specifying the Executor parameter when binding the callback
 *
 * As this implementation is lock-free, the internal state is controlled by an AtomicReference on the PromiseState enum
 * which contains the finite list of state the promise can be in, as there can be a data race when both the value and the callback
 * are set at the same time, a spin-wait loop is used to do an active-waiting for a couple of CPU cycle and prevent any race
 *
 * @param <T> value type expected to be passed to the callback
 */
public class Promise<T> {
    //Executor used by default, execute the runnable on the current thread
    private static final Executor NOOP_EXECUTOR = new NoOpExecutor();

    //all the different states our promise can be in
    private enum PromiseState{
        EMPTY,
        VALUE_BUT_NO_CALLBACK,
        EXCEPTION_BUT_NO_CALLBACK,
        CALLBACK_BUT_NO_VALUE_OR_EXCEPTION,
        RESOLVED_WITH_VALUE,
        RESOLVED_WITH_EXCEPTION
    }

    //current promise state
    private AtomicReference<PromiseState> state = new AtomicReference<PromiseState>(PromiseState.EMPTY);

    //promise values
    private AtomicReference<Callback<T>> callback = new AtomicReference<Callback<T>>();
    private AtomicReference<T> value = new AtomicReference<T>();
    private AtomicReference<Exception> exception = new AtomicReference<Exception>();
    private AtomicReference<Executor> executor = new AtomicReference<Executor>();

    /**
     * Create a new empty promise that can be resolved at any time
     */
    public Promise(){}

    /**
     * Create a new promise that is already resolved
     * @param value
     */
    public Promise(T value){
        this.resolve(value);
    }

    /**
     * Create a new promise that is already failed
     * @param e
     */
    public Promise(Exception e){
        this.fail(e);
    }

    /**
     * Resolve the promise and set it to the given value, if the promise is already resolved or failed, an exception will
     * be thrown. If a callback is already set, dispatch its execution
     * @param value
     */
    public void resolve(T value){
        if(value == null) throw new IllegalArgumentException("Null exception");
        if(this.state.get() == PromiseState.VALUE_BUT_NO_CALLBACK) throw new IllegalStateException("The promise already contains a value");
        if(this.state.get() == PromiseState.RESOLVED_WITH_VALUE || this.state.get() == PromiseState.RESOLVED_WITH_EXCEPTION) throw new IllegalStateException("Promise already resolved");
        if(this.state.get() == PromiseState.EXCEPTION_BUT_NO_CALLBACK) throw new IllegalStateException("Promise already failed");

        else if(this.state.compareAndSet(PromiseState.EMPTY,PromiseState.VALUE_BUT_NO_CALLBACK)){
            this.value.set(value);
        }else if(this.state.compareAndSet(PromiseState.CALLBACK_BUT_NO_VALUE_OR_EXCEPTION,PromiseState.RESOLVED_WITH_VALUE)){
            this.value.set(value);
            this.executeCallback(this.waitForValue(this.callback),value, null);
        }
    }

    /**
     * Fail the promise and set the exception to the given value, if the promise is already resolved or failed, an exception will
     * be thrown. If a callback is already set, dispatch its execution
     * @param e
     */
    public void fail(Exception e){
        if(e == null) throw new IllegalArgumentException("Null exception");
        if(this.state.get() == PromiseState.EXCEPTION_BUT_NO_CALLBACK) throw new IllegalStateException("The promise already contains an exception");
        if(this.state.get() == PromiseState.RESOLVED_WITH_VALUE || this.state.get() == PromiseState.RESOLVED_WITH_EXCEPTION) throw new IllegalStateException("Promise already resolved");
        if(this.state.get() == PromiseState.VALUE_BUT_NO_CALLBACK) throw new IllegalStateException("Promise already contain value");

        else if(this.state.compareAndSet(PromiseState.EMPTY,PromiseState.EXCEPTION_BUT_NO_CALLBACK)){
            this.exception.set(e);
        }else if(this.state.compareAndSet(PromiseState.CALLBACK_BUT_NO_VALUE_OR_EXCEPTION,PromiseState.RESOLVED_WITH_EXCEPTION)){
            this.exception.set(e);
            this.executeCallback(this.waitForValue(this.callback),null, e);
        }
    }

    /**
     * set the promise callback, the callback will be called when either a value or an exception is set in the promise,
     * the callback will be executed in the given Executor
     * @param executor executor the callback will be dispatched to
     * @param callback callback to execute
     */
    public void then(Executor executor, Callback<T> callback){
        if(!this.callback.compareAndSet(null,callback)) {
            throw new IllegalStateException("Callback already bound");
        }else if(this.state.compareAndSet(PromiseState.EMPTY,PromiseState.CALLBACK_BUT_NO_VALUE_OR_EXCEPTION)){
            this.executor.set(executor);

        }else if(this.state.compareAndSet(PromiseState.VALUE_BUT_NO_CALLBACK,PromiseState.RESOLVED_WITH_VALUE)){
            this.executor.set(executor);
            this.executeCallback(callback,this.waitForValue(this.value),null);

        }else if(this.state.compareAndSet(PromiseState.EXCEPTION_BUT_NO_CALLBACK,PromiseState.RESOLVED_WITH_EXCEPTION)){
            this.executor.set(executor);
            this.executeCallback(callback,null, this.waitForValue(this.exception));
        }
    }

    /**
     * Same as then(Executor, Callback<T>) but without specifying the Executor. The callback will be executed on the thread
     * calling resolve() or on the thread calling then() depending upon which comes last
     * @param callback
     */
    public void then(Callback<T> callback){
        this.then(NOOP_EXECUTOR,callback);
    }

    /**
     * Same method as then(Promise.Callback) except that this method return a Promise and pass the exception implicitly
     * to the returned promise, use this method to chain promises more easily than with then(Promise.Callback). you can
     * specify on which thread the callable will be executed.
     * @param executor executor on which the callback will be executed
     * @param callable
     * @param <U>
     * @return
     */
    public <U extends Serializable> Promise<U> then(Executor executor, final ImplicitExceptionCallback<T,U> callable){
        final Promise<U> returned = new Promise<U>();
        this.then(executor,new Callback<T>() {
            @Override
            public void value(T value, Exception exc) {
                if(exc != null) returned.fail(exc);
                try{
                    returned.resolve(callable.value(value));
                }catch (Exception e){
                    returned.fail(e);
                }
            }
        });
        return returned;
    }

    /**
     * Same as then(Executor, ImplicitExceptionCallback<T,U>) but without specifying the executor The callback will be
     * executed on the thread calling resolve() or on the thread calling then() depending upon which comes last
     * @param callable
     * @param <U>
     * @return
     */
    public <U extends Serializable> Promise<U> then(final ImplicitExceptionCallback<T,U> callable){
        return this.then(NOOP_EXECUTOR,callable);
    }


    /**
     * When dealing with non-blocking primitives, you sometime need to wait for values for just a couple of CPU cycles.
     * This method will loop over the reference until it is set to a non-null value. Use this method when you know that
     * the reference will be set to a non-null value shortly.
     *
     * @param ref
     * @param <A>
     * @return
     */
    private <A> A waitForValue(AtomicReference<A> ref){
        A returned = null;
        while (returned == null){
            returned = ref.get();
            //if the value is not here yet, yield the Thread to consume less CPU cycles and wait for it.
            //when migrating to Java 8, use Thread.onSpinWait() which uses x86-specific instruction to further optimise
            //the loop
            if(returned == null) Thread.yield();
        }

        return returned;
    }

    /**
     * Execute the callback in the given executor
     * @param callback
     * @param value
     * @param e
     */
    private void executeCallback(final Callback<T> callback, final T value, final Exception e){
        this.waitForValue(this.executor).execute(new Runnable() {
            @Override
            public void run() {
                callback.value(value,e);
            }
        });
    }

    /**
     * Return true if an exception is set for the promise
     * @return
     */
    public boolean isFailed(){
        return this.state.get() == PromiseState.EXCEPTION_BUT_NO_CALLBACK || this.state.get() == PromiseState.RESOLVED_WITH_EXCEPTION;
    }

    /**
     * Return true if a value is set for the promise
     * @return
     */
    public boolean hasValue(){
        return this.state.get() == PromiseState.VALUE_BUT_NO_CALLBACK || this.state.get() == PromiseState.RESOLVED_WITH_VALUE;
    }

    /**
     * Returns true if the promise has a value or is failed
     * @return
     */
    public boolean isResolved(){
        return this.hasValue() || this.isFailed();
    }

    /**
     * Get the value for this promise if hasValue() returns true, else return null
     * @return
     */
    public T getValue(){
        if(this.hasValue()){
            return this.waitForValue(this.value);
        }
        else return null;
    }

    /**
     * Get the exception for this promise if isFailed() returns true, else return null
     * @return
     */
    public Exception getException(){
        if(this.isFailed()){
            return this.waitForValue(this.exception);
        }
        else return null;
    }

    /**
     * Callback interface for the promise
     * @param <T>
     */
    public interface Callback<T>{
        /**
         * Method called when the promise is either resolved or failed, only one of either value or exception will be
         * not-null.
         *
         * @param value
         * @param e
         */
        void value(T value, Exception e);
    }

    /**
     * Callback used when we don't want to process the exception and want it to be implicitly passed to the following
     * promise
     * @param <In> value from the calling promise
     * @param <Out> value returned by the current promise
     */
    public interface ImplicitExceptionCallback<In,Out>{
        Out value(In value);
    }

    /**
     * Returns a Promise that will be resolved when all the given promises are resolved, the value passed to the returned
     * promise is the array of promises that were awaited. If one of the promises fail, the returned promise will be failed
     * too
     *
     * @param promises
     * @return
     */
    public static Promise<Promise[]> awaitAll(final Promise<?>... promises){
        if(promises.length == 0) return new Promise<>(new Promise[0]);

        final int nbrPromise = promises.length;
        final AtomicInteger counter = new AtomicInteger(0);
        final Promise<Promise[]> returned = new Promise<>();
        final AtomicBoolean isResolved = new AtomicBoolean(false);

        for(Promise<?> promise : promises){
            promise.then(NOOP_EXECUTOR, new Callback() {
                @Override
                public void value(Object value, Exception e) {
                    if(e != null && isResolved.compareAndSet(false,true)) returned.fail(e);
                    if(counter.incrementAndGet() == nbrPromise && isResolved.compareAndSet(false,true)) returned.resolve(promises);
                }
            });
        }
        return returned;
    }

    /**
     * Default executor used for callback execution, it will execute the callback in the current thread
     */
    private static class NoOpExecutor implements Executor{
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    }
}
