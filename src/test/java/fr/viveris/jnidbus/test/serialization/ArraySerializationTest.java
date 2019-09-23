/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.test.common.DBusObjects.arrays.NestedPrimitiveArray;
import fr.viveris.jnidbus.test.common.DBusObjects.arrays.PrimitiveArray;
import fr.viveris.jnidbus.test.common.handlers.arrays.NestedPrimitiveArrayHandler;
import fr.viveris.jnidbus.test.common.handlers.arrays.PrimitiveArrayHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ArraySerializationTest extends SerializationTestCase {

    @Test
    public void emptyArrayOfPrimitiveTest() throws InterruptedException {
        PrimitiveArray msg = new PrimitiveArray();
        msg.setPrimitive(new int[0]);
        msg.setPrimitiveBoxed(new Integer[0]);
        msg.setCollection(new ArrayList<Integer>());

        PrimitiveArray received = this.sendAndReceive(new PrimitiveArrayHandler(),msg);

        assertEquals(0,received.getPrimitive().length);
        assertEquals(0,received.getPrimitiveBoxed().length);
        assertEquals(0,received.getCollection().size());
    }

    @Test
    public void arrayOfPrimitiveTest() throws InterruptedException {
        PrimitiveArray msg = new PrimitiveArray();
        int[] primitiveArray = new int[]{1,2,3};
        Integer[] primitiveBoxed = new Integer[]{4,5,6};
        Integer[] listValues = new Integer[]{7,8,9};
        msg.setPrimitive(primitiveArray);
        msg.setPrimitiveBoxed(primitiveBoxed);
        msg.setCollection(Arrays.asList(listValues));

        PrimitiveArray received = this.sendAndReceive(new PrimitiveArrayHandler(),msg);

        assertEquals(3,received.getPrimitive().length);
        assertArrayEquals(primitiveArray,received.getPrimitive());

        assertEquals(3,received.getPrimitiveBoxed().length);
        assertArrayEquals(primitiveBoxed,received.getPrimitiveBoxed());

        assertEquals(3,received.getCollection().size());
        assertArrayEquals(listValues,received.getCollection().toArray());
    }

    @Test
    public void emptyNestedArrayTest() throws InterruptedException {
        NestedPrimitiveArray msg = new NestedPrimitiveArray();
        msg.setPrimitive(new int[0][0]);
        msg.setPrimitiveBoxed(new Integer[0][0]);
        msg.setCollection(new ArrayList<List<Integer>>());

        NestedPrimitiveArray received = this.sendAndReceive(new NestedPrimitiveArrayHandler(),msg);

        assertEquals(0,received.getPrimitive().length);
        assertEquals(0,received.getPrimitiveBoxed().length);
        assertEquals(0,received.getCollection().size());
    }

    @Test
    public void nestedArrayTest() throws InterruptedException {
        NestedPrimitiveArray msg = new NestedPrimitiveArray();

        int[] primitiveArray = new int[]{1,2,3};
        Integer[] primitiveBoxed = new Integer[]{4,5,6};
        Integer[] listValues = new Integer[]{7,8,9};
        msg.setPrimitive(new int[][]{primitiveArray});
        msg.setPrimitiveBoxed(new Integer[][]{primitiveBoxed});
        List<List<Integer>> list = new ArrayList<>();
        list.add(Arrays.asList(listValues));
        msg.setCollection(list);

        NestedPrimitiveArray received = this.sendAndReceive(new NestedPrimitiveArrayHandler(),msg);

        assertEquals(1,received.getPrimitive().length);
        assertEquals(1,received.getPrimitiveBoxed().length);
        assertEquals(1,received.getCollection().size());

        assertArrayEquals(primitiveArray,received.getPrimitive()[0]);
        assertArrayEquals(primitiveBoxed,received.getPrimitiveBoxed()[0]);
        assertArrayEquals(listValues,received.getCollection().get(0).toArray());
    }
}
