package Call;

import Common.DBusTestCase;
import Common.Listener;
import Common.DBusObjects.StringMessage;
import fr.viveris.vizada.jnidbus.dispatching.Criteria;
import fr.viveris.vizada.jnidbus.dispatching.GenericHandler;
import fr.viveris.vizada.jnidbus.dispatching.annotation.Handler;
import fr.viveris.vizada.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.vizada.jnidbus.exception.DBusException;
import fr.viveris.vizada.jnidbus.exception.MessageSignatureMismatch;
import fr.viveris.vizada.jnidbus.message.Call;
import fr.viveris.vizada.jnidbus.message.DbusMethodCall;
import fr.viveris.vizada.jnidbus.message.Message;
import fr.viveris.vizada.jnidbus.message.PendingCall;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

public class FailedCallTest extends DBusTestCase {
    @Test
    public void callOnNonExistentMethod() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addMessageHandler(handler);
        PendingCall<Message.EmptyMessage> pending = this.sender.call(new UnknownCall(this.receiverBusName));
        Listener<Message.EmptyMessage> l = new Listener<>();
        pending.setListener(l);
        assertFalse(handler.barrier.await(2, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(2, TimeUnit.SECONDS));
        assertNull(l.getValue());
        assertNotNull(l.getT());
        assertEquals(DBusException.METHOD_NOT_FOUND_CODE,l.getT().getCode());
    }

    @Test
    public void callReturnsWrongSignature() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addMessageHandler(handler);
        PendingCall<StringMessage> pending = this.sender.call(new MismatchCall(this.receiverBusName));
        Listener<StringMessage> l = new Listener<>();
        pending.setListener(l);
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(2, TimeUnit.SECONDS));
        assertNull(l.getValue());
        assertNotNull(l.getT());
        assertEquals(MessageSignatureMismatch.class.getName(),l.getT().getCode());
    }

    @Test
    public void callReturnsError() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addMessageHandler(handler);
        PendingCall<Message.EmptyMessage> pending = this.sender.call(new FailCall(this.receiverBusName));
        Listener<Message.EmptyMessage> l = new Listener<>();
        pending.setListener(l);
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(2, TimeUnit.SECONDS));
        assertNull(l.getValue());
        assertNotNull(l.getT());
        assertEquals("test.error.code",l.getT().getCode());
        assertEquals("TestMessage",l.getT().getMessage());
    }

    @Handler(
            path = "/Call/FailedCallTest",
            interfaceName = "Call.FailedCallTest"
    )
    public class CallHandler extends GenericHandler {
        private CountDownLatch barrier = new CountDownLatch(1);

        @HandlerMethod(
                member = "mismatchCall",
                type = Criteria.HandlerType.METHOD
        )
        public Message.EmptyMessage mismatchCall(Message.EmptyMessage emptyMessage){
            this.barrier.countDown();
            return Message.EMPTY;
        }

        @HandlerMethod(
                member = "failCall",
                type = Criteria.HandlerType.METHOD
        )
        public Message.EmptyMessage failCall(Message.EmptyMessage emptyMessage) throws DBusException {
            this.barrier.countDown();
            throw new DBusException("test.error.code","TestMessage");
        }
    }

    @DbusMethodCall(
            //as destination is dynamic, we override the getDestination method instead of using the annotation
            destination = "",
            path = "/Call/FailedCallTest",
            interfaceName = "Call.FailedCallTest",
            member = "unknownCall"

    )
    public static class UnknownCall extends Call<Message.EmptyMessage,Message.EmptyMessage> {
        private String dest;
        public UnknownCall(String dest) {
            super(Message.EMPTY,Message.EmptyMessage.class);
            this.dest = dest;
        }

        @Override
        public String getDestination() {
            return this.dest;
        }
    }

    @DbusMethodCall(
            //as destination is dynamic, we override the getDestination method instead of using the annotation
            destination = "",
            path = "/Call/FailedCallTest",
            interfaceName = "Call.FailedCallTest",
            member = "mismatchCall"

    )
    public static class MismatchCall extends Call<Message.EmptyMessage,StringMessage> {
        private String dest;
        public MismatchCall(String dest) {
            super(Message.EMPTY,StringMessage.class);
            this.dest = dest;
        }

        @Override
        public String getDestination() {
            return this.dest;
        }
    }

    @DbusMethodCall(
            //as destination is dynamic, we override the getDestination method instead of using the annotation
            destination = "",
            path = "/Call/FailedCallTest",
            interfaceName = "Call.FailedCallTest",
            member = "failCall"

    )
    public static class FailCall extends Call<Message.EmptyMessage,Message.EmptyMessage> {
        private String dest;
        public FailCall(String dest) {
            super(Message.EMPTY, Message.EmptyMessage.class);
            this.dest = dest;
        }

        @Override
        public String getDestination() {
            return this.dest;
        }
    }
}
