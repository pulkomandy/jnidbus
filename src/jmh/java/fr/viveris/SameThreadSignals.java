/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris;

import fr.viveris.jnidbus.BusType;
import fr.viveris.jnidbus.Dbus;
import fr.viveris.jnidbus.bindings.bus.EventLoop;
import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.exception.ConnectionException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.ArrayRecursiveObject;
import fr.viveris.jnidbus.test.common.DBusObjects.SimpleMessage;
import fr.viveris.jnidbus.test.common.DBusTestCase;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(time = 3)
@Fork(2)
public class SameThreadSignals {
    private Dbus sender;
    private String busName;
    private SignalHandler handler;

    public SameThreadSignals(){
        this.busName = "fr.viveris.vizada.jnidbus.benchmarks.SingleThreadSignals."+ DBusTestCase.generateRandomString();
        try {
            this.sender = new Dbus(BusType.SESSION,this.busName,System.getProperty("dbus.busPath"));
            this.handler = new SignalHandler();
            sender.addHandler(this.handler);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    @TearDown
    public void teardown() throws Exception {
        this.sender.close();
    }

    @Benchmark
    @OperationsPerInvocation(EventLoop.MAX_SEND_PER_TICK)
    public void singleThreadSendReceiveEmpty() throws InterruptedException {
        this.handler.latch = new CountDownLatch(EventLoop.MAX_SEND_PER_TICK);
        for(int i = 0; i < EventLoop.MAX_SEND_PER_TICK; i++){
            this.sender.sendSignal("/Benchmarks/SingleThreadSignals",new SameThreadSignalsRemote.EmptySignal());
        }
        this.handler.latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(EventLoop.MAX_SEND_PER_TICK)
    public void singleThreadSendReceiveComplex() throws InterruptedException {
        this.handler.latch = new CountDownLatch(EventLoop.MAX_SEND_PER_TICK);

        ArrayRecursiveObject obj = new ArrayRecursiveObject();
        ArrayRecursiveObject.SubArrayRecursiveObject sub1 = new ArrayRecursiveObject.SubArrayRecursiveObject();
        ArrayRecursiveObject.SubArrayRecursiveObject sub2 = new ArrayRecursiveObject.SubArrayRecursiveObject();
        sub1.setInteger(42);
        sub1.getStrings().add("comp1");
        sub1.getStrings().add("comp2");
        sub1.getStrings().add("comp3");
        sub2.setInteger(24);
        sub2.getStrings().add("comp11");
        sub2.getStrings().add("comp21");
        obj.getObjects().add(sub1);
        obj.getObjects().add(sub2);

        for(int i = 0; i < EventLoop.MAX_SEND_PER_TICK; i++){
            this.sender.sendSignal("/Benchmarks/SingleThreadSignals",new SameThreadSignalsRemote.ComplexSignal(obj));
        }
        this.handler.latch.await();
    }

    @Handler(
            path = "/Benchmarks/SingleThreadSignals",
            interfaceName = "Benchmarks.SingleThreadSignals"
    )
    public class SignalHandler extends GenericHandler{
        CountDownLatch latch;

        @HandlerMethod(
                member = "emptyMessage",
                type = MemberType.SIGNAL
        )
        public void emptyMessage(Message.EmptyMessage emptyMessage){
            this.latch.countDown();
        }

        @HandlerMethod(
                member = "complexMessage",
                type = MemberType.SIGNAL
        )
        public void complexMessage(ArrayRecursiveObject complexMessage){
            this.latch.countDown();
        }

        @HandlerMethod(
                member = "simpleMessage",
                type = MemberType.SIGNAL
        )
        public void simpleMessage(SimpleMessage simpleMessage){
            this.latch.countDown();
        }
    }

    @RemoteInterface("Benchmarks.SingleThreadSignals")
    public interface SameThreadSignalsRemote{

        @RemoteMember("emptyMessage")
        class EmptySignal extends Signal<Message.EmptyMessage> {
            public EmptySignal() {
                super(Message.EMPTY);
            }
        }

        @RemoteMember("complexMessage")
        class ComplexSignal extends Signal<ArrayRecursiveObject>{
            public ComplexSignal(ArrayRecursiveObject msg) {
                super(msg);
            }
        }

        @RemoteMember("simpleMessage")
        class SimpleSignal extends Signal<SimpleMessage>{
            public SimpleSignal(SimpleMessage msg) {
                super(msg);
            }
        }

    }
}
