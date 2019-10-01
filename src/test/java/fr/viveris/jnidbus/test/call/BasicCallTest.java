/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.call;

import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.PendingCall;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.test.common.DBusObjects.SingleStringMessage;
import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.Listener;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

public class BasicCallTest extends DBusTestCase {

    @Test
    public void emptyCall() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandlerBlocking(handler);
        BasicCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/BasicCallTest",BasicCallTestRemote.class);
        PendingCall<Message.EmptyMessage> pending = remoteObj.emptyCall();
        Listener<Message.EmptyMessage> l = new Listener<>();
        pending.setListener(l);
        assertTrue(handler.barrier.await(5, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(5, TimeUnit.SECONDS));
        assertEquals(Message.EMPTY,l.getValue());
        assertNull(l.getT());
    }

    @Test
    public void callIsSerializedAnddeserialized() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandlerBlocking(handler);
        BasicCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/BasicCallTest",BasicCallTestRemote.class);

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
            path = "/fr/viveris/jnidbus/test/call/BasicCallTest",
            interfaceName = "fr.viveris.jnidbus.test.Call.BasicCallTest"
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

    @RemoteInterface("fr.viveris.jnidbus.test.Call.BasicCallTest")
    public interface BasicCallTestRemote {

        @RemoteMember("emptyCall")
        PendingCall<Message.EmptyMessage> emptyCall();

        @RemoteMember("stringCall")
        PendingCall<SingleStringMessage> stringCall(SingleStringMessage msg);

    }
}
