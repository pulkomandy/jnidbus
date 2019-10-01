/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.test.common.DBusObjects.primitives.*;
import fr.viveris.jnidbus.test.common.handlers.primitives.*;
import fr.viveris.jnidbus.types.ObjectPath;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class PrimitiveTypesSerializationTest extends SerializationTestCase {

    @Test
    public void integerTest() throws InterruptedException {
        IntHandler handler = new IntHandler();
        IntMessage msg = new IntMessage();
        msg.setPrimitive(1);
        msg.setBoxed(2);
        msg.setList(Arrays.asList(3));
        IntMessage received = this.sendAndReceive(new IntHandler(),msg);
        assertEquals(1,received.getPrimitive());
        assertEquals(2,received.getBoxed().intValue());
        assertEquals(3,received.getList().get(0).intValue());
    }

    @Test
    public void booleanTest() throws InterruptedException {
        BooleanMessage msg = new BooleanMessage();
        msg.setPrimitive(true);
        msg.setBoxed(false);
        msg.setList(Arrays.asList(true));
        BooleanMessage received = this.sendAndReceive(new BooleanHandler(),msg);
        assertTrue(received.getPrimitive());
        assertFalse(received.getBoxed());
        assertTrue(received.getList().get(0));
    }

    @Test
    public void byteTest() throws InterruptedException {
        ByteMessage msg = new ByteMessage();
        msg.setPrimitive((byte)1);
        msg.setBoxed((byte)2);
        msg.setList(Arrays.asList((byte)3));
        ByteMessage received = this.sendAndReceive(new ByteHandler(),msg);
        assertEquals(1,received.getPrimitive());
        assertEquals(2,received.getBoxed().byteValue());
        assertEquals(3,received.getList().get(0).byteValue());
    }

    @Test
    public void shortTest() throws InterruptedException {
        ShortMessage msg = new ShortMessage();
        msg.setPrimitive((short)1);
        msg.setBoxed((short)2);
        msg.setList(Arrays.asList((short)3));
        ShortMessage received = this.sendAndReceive(new ShortHandler(),msg);
        assertEquals(1,received.getPrimitive());
        assertEquals(2,received.getBoxed().shortValue());
        assertEquals(3,received.getList().get(0).shortValue());
    }

    @Test
    public void longTest() throws InterruptedException {
        LongMessage msg = new LongMessage();
        msg.setPrimitive(1L);
        msg.setBoxed(2L);
        msg.setList(Arrays.asList(3L));
        LongMessage received = this.sendAndReceive(new LongHandler(), msg);
        assertEquals(1L,received.getPrimitive());
        assertEquals(2L,received.getBoxed().longValue());
        assertEquals(3L,received.getList().get(0).longValue());
    }

    @Test
    public void doubleTest() throws InterruptedException {
        DoubleMessage msg = new DoubleMessage();
        msg.setPrimitive(1.1);
        msg.setBoxed(2.2);
        msg.setList(Arrays.asList(3.3));
        DoubleMessage received = this.sendAndReceive(new DoubleHandler(), msg);
        assertEquals(1.1,received.getPrimitive(),0);
        assertEquals(2.2,received.getBoxed(),0);
        assertEquals(3.3,received.getList().get(0),0);
    }

    @Test
    public void objectPathTest() throws InterruptedException {
        ObjectPathMessage msg = new ObjectPathMessage();
        msg.setPrimitive(new ObjectPath("/a/b/c"));
        msg.setList(Arrays.asList(new ObjectPath("/d/e/f"),new ObjectPath("/h/i/j")));
        ObjectPathMessage received = this.sendAndReceive(new ObjectPathHandler(), msg);
        assertEquals(new ObjectPath("/a/b/c"),received.getPrimitive());
        assertArrayEquals(Arrays.asList(new ObjectPath("/d/e/f"),new ObjectPath("/h/i/j")).toArray(),received.getList().toArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongObjectPathFormat(){
        ObjectPath path = new ObjectPath("a//b/c/");
    }

    @Test
    public void enumTest() throws InterruptedException {
        EnumMessage msg = new EnumMessage();
        msg.setByName(EnumMessage.Enum.A);
        msg.setByOrdinal(EnumMessage.Enum.B);
        msg.setByNameArray(EnumMessage.Enum.values());
        msg.setByOrdinalArray(EnumMessage.Enum.values());
        msg.setByNameList(Arrays.asList(EnumMessage.Enum.values()));
        msg.setByOrdinalList(Arrays.asList(EnumMessage.Enum.values()));

        EnumMessage received = this.sendAndReceive(new EnumHandler(),msg);

        assertEquals(EnumMessage.Enum.A,received.getByName());
        assertEquals(EnumMessage.Enum.B,received.getByOrdinal());
        assertArrayEquals(EnumMessage.Enum.values(),received.getByNameArray());
        assertArrayEquals(EnumMessage.Enum.values(),received.getByOrdinalArray());
        assertArrayEquals(EnumMessage.Enum.values(),received.getByNameList().toArray());
        assertArrayEquals(EnumMessage.Enum.values(),received.getByOrdinalList().toArray());
    }
}
