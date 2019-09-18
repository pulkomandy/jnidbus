/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.test.common.DBusObjects.primitives.*;
import fr.viveris.jnidbus.test.common.DBusTestCase;
import fr.viveris.jnidbus.test.common.handlers.*;
import fr.viveris.jnidbus.serialization.DBusObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PrimitiveTypesSerializationTest extends DBusTestCase {

    @Test
    public void integerTest() throws InterruptedException {
        IntHandler handler = new IntHandler();
        this.receiver.addHandler(handler);
        IntMessage msg = new IntMessage();
        msg.setPrimitive(1);
        msg.setBoxed(2);
        msg.setList(Arrays.asList(3));
        this.sender.sendSignal("/handlers/primitive/int",new IntHandler.IntHandlerRemote.IntSignal(msg));
        assertTrue(handler.getBarrier().await(2, TimeUnit.SECONDS));
        assertEquals(1,handler.getValue().getPrimitive());
        assertEquals(2,handler.getValue().getBoxed().intValue());
        assertEquals(3,handler.getValue().getList().get(0).intValue());
    }

    @Test
    public void booleanTest() throws InterruptedException {
        BooleanHandler handler = new BooleanHandler();
        this.receiver.addHandler(handler);
        BooleanMessage msg = new BooleanMessage();
        msg.setPrimitive(true);
        msg.setBoxed(false);
        msg.setList(Arrays.asList(true));
        DBusObject obj = msg.serialize();
        this.sender.sendSignal("/handlers/primitive/boolean",new BooleanHandler.BooleanHandlerRemote.BooleanSignal(msg));
        assertTrue(handler.getBarrier().await(2, TimeUnit.SECONDS));
        assertTrue(handler.getValue().getPrimitive());
        assertFalse(handler.getValue().getBoxed());
        assertTrue(handler.getValue().getList().get(0));
    }

    @Test
    public void byteTest() throws InterruptedException {
        ByteHandler handler = new ByteHandler();
        this.receiver.addHandler(handler);
        ByteMessage msg = new ByteMessage();
        msg.setPrimitive((byte)1);
        msg.setBoxed((byte)2);
        msg.setList(Arrays.asList((byte)3));
        this.sender.sendSignal("/handlers/primitive/byte",new ByteHandler.ByteHandlerRemote.ByteSignal(msg));
        assertTrue(handler.getBarrier().await(2, TimeUnit.SECONDS));
        assertEquals(1,handler.getValue().getPrimitive());
        assertEquals(2,handler.getValue().getBoxed().byteValue());
        assertEquals(3,handler.getValue().getList().get(0).byteValue());
    }

    @Test
    public void shortTest() throws InterruptedException {
        ShortHandler handler = new ShortHandler();
        this.receiver.addHandler(handler);
        ShortMessage msg = new ShortMessage();
        msg.setPrimitive((short)1);
        msg.setBoxed((short)2);
        msg.setList(Arrays.asList((short)3));
        this.sender.sendSignal("/handlers/primitive/short",new ShortHandler.ShortHandlerRemote.ShortSignal(msg));
        assertTrue(handler.getBarrier().await(2, TimeUnit.SECONDS));
        assertEquals(1,handler.getValue().getPrimitive());
        assertEquals(2,handler.getValue().getBoxed().shortValue());
        assertEquals(3,handler.getValue().getList().get(0).shortValue());
    }

    @Test
    public void longTest() throws InterruptedException {
        LongHandler handler = new LongHandler();
        this.receiver.addHandler(handler);
        LongMessage msg = new LongMessage();
        msg.setPrimitive(1L);
        msg.setBoxed(2L);
        msg.setList(Arrays.asList(3L));
        this.sender.sendSignal("/handlers/primitive/long",new LongHandler.LongHandlerRemote.LongSignal(msg));
        assertTrue(handler.getBarrier().await(2, TimeUnit.SECONDS));
        assertEquals(1L,handler.getValue().getPrimitive());
        assertEquals(2L,handler.getValue().getBoxed().longValue());
        assertEquals(3L,handler.getValue().getList().get(0).longValue());
    }

    @Test
    public void doubleTest() throws InterruptedException {
        DoubleHandler handler = new DoubleHandler();
        this.receiver.addHandler(handler);
        DoubleMessage msg = new DoubleMessage();
        msg.setPrimitive(1.1);
        msg.setBoxed(2.2);
        msg.setList(Arrays.asList(3.3));
        this.sender.sendSignal("/handlers/primitive/double",new DoubleHandler.DoubleHandlerRemote.DoubleSignal(msg));
        assertTrue(handler.getBarrier().await(2, TimeUnit.SECONDS));
        assertEquals(1.1,handler.getValue().getPrimitive(),0);
        assertEquals(2.2,handler.getValue().getBoxed(),0);
        assertEquals(3.3,handler.getValue().getList().get(0),0);
    }
}
