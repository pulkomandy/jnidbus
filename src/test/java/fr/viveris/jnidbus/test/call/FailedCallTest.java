/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.call;

import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.Listener;
import fr.viveris.jnidbus.test.common.DBusObjects.SingleStringMessage;
import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.exception.DBusException;
import fr.viveris.jnidbus.exception.MessageSignatureMismatchException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.PendingCall;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

public class FailedCallTest extends DBusTestCase {

    @Test
    public void callOnNonExistentMethod() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandler(handler);
        FailedCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/FailedCallTest",FailedCallTestRemote.class);

        PendingCall<Message.EmptyMessage> pending = remoteObj.unknownCall();
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
        this.receiver.addHandler(handler);
        FailedCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/FailedCallTest",FailedCallTestRemote.class);

        PendingCall<SingleStringMessage> pending = remoteObj.mismatchCall();
        Listener<SingleStringMessage> l = new Listener<>();
        pending.setListener(l);

        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(2, TimeUnit.SECONDS));
        assertNull(l.getValue());
        assertNotNull(l.getT());
        assertEquals(MessageSignatureMismatchException.class.getName(),l.getT().getCode());
    }

    @Test
    public void callReturnsError() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandler(handler);
        FailedCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/FailedCallTest",FailedCallTestRemote.class);

        PendingCall<Message.EmptyMessage> pending = remoteObj.failCall();
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
            path = "/fr/viveris/jnidbus/test/call/FailedCallTest",
            interfaceName = "fr.viveris.jnidbus.test.Call.FailedCallTest"
    )
    public class CallHandler extends GenericHandler {
        private CountDownLatch barrier = new CountDownLatch(1);

        @HandlerMethod(
                member = "mismatchCall",
                type = MemberType.METHOD
        )
        public Message.EmptyMessage mismatchCall(Message.EmptyMessage emptyMessage){
            this.barrier.countDown();
            return Message.EMPTY;
        }

        @HandlerMethod(
                member = "failCall",
                type = MemberType.METHOD
        )
        public Message.EmptyMessage failCall(Message.EmptyMessage emptyMessage) throws DBusException {
            this.barrier.countDown();
            throw new DBusException("test.error.code","TestMessage");
        }
    }

    @RemoteInterface("fr.viveris.jnidbus.test.Call.FailedCallTest")
    public interface FailedCallTestRemote{

        @RemoteMember("unknownCall")
        PendingCall<Message.EmptyMessage> unknownCall();

        @RemoteMember("mismatchCall")
        PendingCall<SingleStringMessage> mismatchCall();

        @RemoteMember("failCall")
        PendingCall<Message.EmptyMessage> failCall();
    }
}
