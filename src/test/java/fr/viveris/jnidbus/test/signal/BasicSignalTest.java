package fr.viveris.jnidbus.test.signal;

import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.DBusObjects.SingleStringMessage;
import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BasicSignalTest extends DBusTestCase {

    public static String testString = "test";

    @Test
    public void emptySignal() throws InterruptedException {
        SignalHandler handler = new SignalHandler();
        this.receiver.addHandler(handler);
        this.sender.sendSignal("/fr/viveris/jnidbus/signal/BasicSignalTest",new BasicSignalTestRemote.EmptySignal());
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testHandlerCanBeUnregistered() throws InterruptedException {
        SignalHandler handler = new SignalHandler();
        this.receiver.addHandler(handler);
        this.sender.sendSignal("/fr/viveris/jnidbus/signal/BasicSignalTest",new BasicSignalTestRemote.EmptySignal());
        assertTrue(handler.barrier.await(500, TimeUnit.MILLISECONDS));
        this.receiver.removeHandler(handler);
        handler.reset();
        this.sender.sendSignal("/fr/viveris/jnidbus/signal/BasicSignalTest",new BasicSignalTestRemote.EmptySignal());
        assertFalse(handler.barrier.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void signalWithWrongSignatureIsNotDispatched() throws InterruptedException {
        SignalHandler handler = new SignalHandler();
        this.receiver.addHandler(handler);
        SingleStringMessage msg = new SingleStringMessage();
        msg.setString(testString);
        this.sender.sendSignal("/fr/viveris/jnidbus/signal/BasicSignalTest",new BasicSignalTestRemote.StringSignalOnWrongEndpoint(msg));
        assertFalse(handler.barrier.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void signalIsSerializedAndUnserialized() throws InterruptedException {
        SignalHandler handler = new SignalHandler();
        this.receiver.addHandler(handler);
        SingleStringMessage msg = new SingleStringMessage();
        msg.setString(testString);
        this.sender.sendSignal("/fr/viveris/jnidbus/signal/BasicSignalTest",new BasicSignalTestRemote.StringSignal(msg));
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
    }


    @Handler(
            path = "/fr/viveris/jnidbus/signal/BasicSignalTest",
            interfaceName = "fr.viveris.jnidbus.fr.viveris.jnidbus.test.Signal.BasicSignalTest"
    )
    public class SignalHandler extends GenericHandler {
        private CountDownLatch barrier = new CountDownLatch(1);

        @HandlerMethod(
                member = "emptySignal",
                type = MemberType.SIGNAL
        )
        public void emptySignal(Message.EmptyMessage emptyMessage){
            this.barrier.countDown();
        }

        @HandlerMethod(
                member = "stringSignal",
                type = MemberType.SIGNAL
        )
        public void stringSignal(SingleStringMessage string){
            if(string.getString().equals(BasicSignalTest.testString)){
                this.barrier.countDown();
            }
        }

        public void reset(){
            this.barrier = new CountDownLatch(1);
        }
    }

    @RemoteInterface("fr.viveris.jnidbus.fr.viveris.jnidbus.test.Signal.BasicSignalTest")
    public interface BasicSignalTestRemote{

        @RemoteMember("emptySignal")
        class EmptySignal extends Signal<Message.EmptyMessage> {
            public EmptySignal() {
                super(Message.EMPTY);
            }
        }

        @RemoteMember("wrongEndpoint")
        class StringSignalOnWrongEndpoint extends Signal<SingleStringMessage>{
            public StringSignalOnWrongEndpoint(SingleStringMessage msg) {
                super(msg);
            }
        }

        @RemoteMember("stringSignal")
        class StringSignal extends Signal<SingleStringMessage>{
            public StringSignal(SingleStringMessage msg) {
                super(msg);
            }
        }

    }
}
