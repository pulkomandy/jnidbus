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
import fr.viveris.jnidbus.message.Promise;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.test.common.DBusObjects.SingleStringMessage;
import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.Listener;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

public class AsyncCallTest extends DBusTestCase {

    @Test
    public void asyncBlockingCall() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandler(handler);
        AsyncCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/AsyncCallTest",AsyncCallTestRemote.class);

        PendingCall<Message.EmptyMessage> pending = remoteObj.blockingCall();
        Listener<Message.EmptyMessage> l = new Listener<>();
        pending.setListener(l);

        assertTrue(handler.barrier.await(5, TimeUnit.SECONDS));
        assertTrue(l.getBarrier().await(5, TimeUnit.SECONDS));
        assertEquals(Message.EMPTY,l.getValue());
        assertNull(l.getT());
    }

    @Test
    public void asyncBlockingCallDoNotBlockEventLoop() throws InterruptedException {
        CallHandler handler = new CallHandler();
        this.receiver.addHandler(handler);
        AsyncCallTestRemote remoteObj = this.sender.createRemoteObject(this.receiverBusName, "/fr/viveris/jnidbus/test/call/AsyncCallTest",AsyncCallTestRemote.class);

        PendingCall<Message.EmptyMessage> pendingEmpty = remoteObj.blockingCall();
        PendingCall<SingleStringMessage> pendingString = remoteObj.instantCall();
        Listener<Message.EmptyMessage> lEmpty = new Listener<>();
        Listener<SingleStringMessage> lString = new Listener<>();
        pendingEmpty.setListener(lEmpty);
        pendingString.setListener(lString);

        assertTrue(lString.getBarrier().await(1, TimeUnit.SECONDS));
        assertTrue(lEmpty.getBarrier().await(5, TimeUnit.SECONDS));
        assertEquals(Message.EMPTY,lEmpty.getValue());
        assertEquals("test",lString.getValue().getString());
    }

    @Handler(
            path = "/fr/viveris/jnidbus/test/call/AsyncCallTest",
            interfaceName = "fr.viveris.jnidbus.test.Call.AsyncCallTest"
    )
    public class CallHandler extends GenericHandler {
        private CountDownLatch barrier = new CountDownLatch(1);

        @HandlerMethod(
                member = "blockingCall",
                type = MemberType.METHOD
        )
        public Promise<Message.EmptyMessage> blockingCall(Message.EmptyMessage emptyMessage){
            final Promise<Message.EmptyMessage> promise = new Promise<>();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //sleep
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //unlock barrier and resolve promise
                    CallHandler.this.barrier.countDown();
                    promise.resolve(Message.EMPTY);
                }
            }).start();
            return promise;
        }

        @HandlerMethod(
                member = "instantReturn",
                type = MemberType.METHOD
        )
        public Promise<SingleStringMessage> instantReturn(Message.EmptyMessage msg){
            Boolean b = true;
            Promise<SingleStringMessage> promise = new Promise<>();
            this.barrier.countDown();
            SingleStringMessage ret = new SingleStringMessage();
            ret.setString("test");
            promise.resolve(ret);
            return promise;
        }
    }

    @RemoteInterface("fr.viveris.jnidbus.test.Call.AsyncCallTest")
    public interface AsyncCallTestRemote{
        @RemoteMember("blockingCall")
        PendingCall<Message.EmptyMessage> blockingCall();

        @RemoteMember("instantReturn")
        PendingCall<SingleStringMessage> instantCall();
    }
}
