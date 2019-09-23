/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.test.common.DBusObjects.objects.NestedObject;
import fr.viveris.jnidbus.test.common.handlers.objects.NestedObjectHandler;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RecursiveObjectSerializationTest extends SerializationTestCase {

    @Test
    public void recursiveNestedObject() throws InterruptedException {
        NestedObject msg = new NestedObject();
        NestedObject.SubObject1 sub1 = new NestedObject.SubObject1();
        NestedObject.SubObject2 sub21 = new NestedObject.SubObject2();
        NestedObject.SubObject2 sub22 = new NestedObject.SubObject2();

        int[] sub21Array = new int[]{1,2,3};
        int[] sub22Array = new int[]{4,5,6};

        sub21.setInteger(42);
        sub21.setString("SomeString");
        sub21.setIntArrays(sub21Array);

        sub22.setInteger(65);
        sub22.setString("SomeOtherString");
        sub22.setIntArrays(sub22Array);

        sub1.setObjects(Arrays.asList(sub21,sub22));

        msg.setObjects(Arrays.asList(sub1));

        NestedObject received = this.sendAndReceive(new NestedObjectHandler(),msg);

        assertEquals(1,received.getObjects().size());

        assertEquals(2,received.getObjects().get(0).getObjects().size());

        NestedObject.SubObject2 received21 = received.getObjects().get(0).getObjects().get(0);
        NestedObject.SubObject2 received22 = received.getObjects().get(0).getObjects().get(1);

        assertEquals(42,received21.getInteger());
        assertEquals(65,received22.getInteger());

        assertEquals("SomeString",received21.getString());
        assertEquals("SomeOtherString",received22.getString());

        assertArrayEquals(sub21Array,received21.getIntArrays());
        assertArrayEquals(sub22Array,received22.getIntArrays());

    }
}
