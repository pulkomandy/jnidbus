package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.serialization.Serializable;
import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

import java.util.concurrent.TimeUnit;


public abstract class SerializationTestCase extends DBusTestCase {
    protected <T extends Serializable> T sendAndReceive(CommonHandler<T> handler, T value) throws InterruptedException {
        this.receiver.addHandler(handler);
        this.sender.sendSignal(handler.getHandlerAnnotation().path(),handler.buildSignal(value));
        if(!handler.getBarrier().await(100, TimeUnit.MILLISECONDS)){
            throw new IllegalStateException("No signal received, serialization/deserialization probably failed");
        }
        this.receiver.removeHandler(handler);
        return handler.getValue();
    }
}
