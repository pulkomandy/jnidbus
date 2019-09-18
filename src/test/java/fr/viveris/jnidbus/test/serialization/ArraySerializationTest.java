/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.DBusObjects.CollectionArray;
import fr.viveris.jnidbus.test.common.DBusObjects.CollectionOfCollectionArray;
import fr.viveris.jnidbus.dispatching.GenericHandler;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.serialization.DBusObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ArraySerializationTest extends DBusTestCase {

    @Test
    public void collectionOfPrimitiveSerializationAndUnserialization() throws InterruptedException {
        SignalHandler handler = new SignalHandler();
        this.receiver.addHandler(handler);
        CollectionArray typeObject = new CollectionArray();

        //test empty collection is correctly serialized
        DBusObject obj = typeObject.serialize();
        assertEquals("ai",obj.getSignature());
        assertTrue(obj.getValues()[0] instanceof Object[]);
        assertEquals(0,((Object[])obj.getValues()[0]).length);

        //test if JNI code can as well
        this.sender.sendSignal("/fr/viveris/jnidbus/test/serialization/ArraySerializationTest",new ArraySerializationTestRemote.CollectionArraySignal(typeObject));
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        CollectionArray received = handler.collectionArraySignal;
        handler.barrier = new CountDownLatch(1);
        assertNotNull(receiverBusName);
        assertEquals(0,received.getArray().size());

        //test non-empty collection is correctly serialized
        typeObject.getArray().add(42);
        obj = typeObject.serialize();
        assertEquals("ai",obj.getSignature());
        assertTrue(obj.getValues()[0] instanceof Object[]);
        assertEquals(1,((Object[])obj.getValues()[0]).length);
        assertEquals(42,((Object[])obj.getValues()[0])[0]);

        //test if JNI code can as well
        this.sender.sendSignal("/fr/viveris/jnidbus/test/serialization/ArraySerializationTest",new ArraySerializationTestRemote.CollectionArraySignal(typeObject));
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        received = handler.collectionArraySignal;
        handler.barrier = new CountDownLatch(1);
        assertNotNull(receiverBusName);
        assertEquals(1,received.getArray().size());
        assertEquals(42,received.getArray().get(0).intValue());
    }

    @Test
    public void recursiveCollectionOfPrimitiveSerializationAndUnserialization() throws InterruptedException {
        SignalHandler handler = new SignalHandler();
        this.receiver.addHandler(handler);
        CollectionOfCollectionArray typeObject = new CollectionOfCollectionArray();

        //test empty collection is correctly serialized
        DBusObject obj = typeObject.serialize();
        assertEquals("aai",obj.getSignature());
        assertTrue(obj.getValues()[0] instanceof Object[]);
        assertEquals(0,((Object[])obj.getValues()[0]).length);

        //test JNI code
        sender.sendSignal("/fr/viveris/jnidbus/test/serialization/ArraySerializationTest",new ArraySerializationTestRemote.CollectionOfCollectionArraySignal(typeObject));
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        CollectionOfCollectionArray received = handler.collectionOfCollectionArraySignal;
        handler.barrier = new CountDownLatch(1);
        assertNotNull(receiverBusName);
        assertEquals(0,received.getArray().size());

        //test non-empty collection is correctly serialized
        ArrayList<Integer> ints = new ArrayList<>();
        ints.add(42);
        typeObject.getArray().add(ints);
        obj = typeObject.serialize();
        assertEquals("aai",obj.getSignature());
        assertTrue(obj.getValues()[0] instanceof Object[]);
        assertEquals(1,((Object[])obj.getValues()[0]).length);
        Object[] recursiveArray = (Object[])obj.getValues()[0];
        assertEquals(1,((Object[])recursiveArray[0]).length);
        assertEquals(42,((Object[])recursiveArray[0])[0]);

        //test if JNI code can as well
        this.sender.sendSignal("/fr/viveris/jnidbus/test/serialization/ArraySerializationTest",new ArraySerializationTestRemote.CollectionOfCollectionArraySignal(typeObject));
        assertTrue(handler.barrier.await(2, TimeUnit.SECONDS));
        received = handler.collectionOfCollectionArraySignal;
        handler.barrier = new CountDownLatch(1);
        assertNotNull(receiverBusName);
        assertEquals(1,received.getArray().size());
        assertEquals(1,received.getArray().get(0).size());
        assertEquals(42,received.getArray().get(0).get(0).intValue());


    }

    @Handler(
            path = "/fr/viveris/jnidbus/test/serialization/ArraySerializationTest",
            interfaceName = "fr.viveris.jnidbus.test.Serialization.ArraySerializationTest"
    )
    public class SignalHandler extends GenericHandler {
        private CountDownLatch barrier = new CountDownLatch(1);
        private CollectionArray collectionArraySignal;
        private CollectionOfCollectionArray collectionOfCollectionArraySignal;

        @HandlerMethod(
                member = "collectionArray",
                type = MemberType.SIGNAL
        )
        public void collectionArray(CollectionArray signal){
            this.collectionArraySignal = signal;
            this.barrier.countDown();
        }

        @HandlerMethod(
                member = "collectionOfCollectionArraySignal",
                type = MemberType.SIGNAL
        )
        public void collectionOfCollectionArraySignal(CollectionOfCollectionArray signal){
            this.collectionOfCollectionArraySignal = signal;
            this.barrier.countDown();
        }
    }

    @RemoteInterface("fr.viveris.jnidbus.test.Serialization.ArraySerializationTest")
    public interface ArraySerializationTestRemote{

        @RemoteMember("collectionArray")
        class CollectionArraySignal extends Signal<CollectionArray> {
            public CollectionArraySignal(CollectionArray msg) {
                super(msg);
            }
        }

        @RemoteMember("collectionOfCollectionArraySignal")
        class CollectionOfCollectionArraySignal extends Signal<CollectionOfCollectionArray> {
            public CollectionOfCollectionArraySignal(CollectionOfCollectionArray msg) {
                super(msg);
            }
        }

    }
}
