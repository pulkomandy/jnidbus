package Call;

import Common.DBusTestCase;
import Common.Listener;
import Common.DBusObjects.SingleStringMessage;
import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.PendingCall;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

public class BasicCallTest extends DBusTestCase {

    @Test
    public void emptyCall() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandler(handler);
        BasicCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName,"/Call/BasicCallTest",BasicCallTestRemote.class);
        PendingCall<Message.EmptyMessage> pending = remoteObj.emptyCall();
        Listener<Message.EmptyMessage> l = new Listener<>();
        pending.setListener(l);
        assertTrue(handler.barrier.await(5, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(5, TimeUnit.SECONDS));
        assertEquals(Message.EMPTY,l.getValue());
        assertNull(l.getT());
    }

    @Test
    public void callIsSerializedAndUnserialized() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandler(handler);
        BasicCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName,"/Call/BasicCallTest",BasicCallTestRemote.class);

        SingleStringMessage msg = new SingleStringMessage();
        msg.setString("test");
        PendingCall<SingleStringMessage> pending = remoteObj.stringCall(msg);
        Listener<SingleStringMessage> l = new Listener<>();
        pending.setListener(l);

        assertTrue(handler.barrier.await(5, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(5, TimeUnit.SECONDS));
        assertEquals(msg.getString(),l.getValue().getString());
        assertNull(l.getT());
    }

    @Handler(
            path = "/Call/BasicCallTest",
            interfaceName = "Call.BasicCallTest"
    )
    public class CallHandler extends GenericHandler {
        private CountDownLatch barrier = new CountDownLatch(1);

        @HandlerMethod(
                member = "emptyCall",
                type = MemberType.METHOD
        )
        public Message.EmptyMessage emptyCall(Message.EmptyMessage emptyMessage){
            this.barrier.countDown();
            return Message.EMPTY;
        }

        @HandlerMethod(
                member = "stringCall",
                type = MemberType.METHOD
        )
        public SingleStringMessage stringCall(SingleStringMessage msg){
            this.barrier.countDown();
            SingleStringMessage ret = new SingleStringMessage();
            ret.setString(msg.getString());
            return ret;
        }
    }

    @RemoteInterface("Call.BasicCallTest")
    public interface BasicCallTestRemote {

        @RemoteMember("emptyCall")
        PendingCall<Message.EmptyMessage> emptyCall();

        @RemoteMember("stringCall")
        PendingCall<SingleStringMessage> stringCall(SingleStringMessage msg);

    }
}
