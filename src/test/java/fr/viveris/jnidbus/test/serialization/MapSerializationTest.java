package fr.viveris.jnidbus.test.serialization;

import fr.viveris.jnidbus.serialization.DBusObject;
import fr.viveris.jnidbus.test.common.DBusObjects.map.ComplexMap;
import fr.viveris.jnidbus.test.common.DBusObjects.map.SimpleMap;
import fr.viveris.jnidbus.test.common.handlers.map.ComplexMapHandler;
import fr.viveris.jnidbus.test.common.handlers.map.SimpleMapHandler;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MapSerializationTest extends SerializationTestCase {
    @Test
    public void simpleMapTest() throws InterruptedException {
        SimpleMap msg = new SimpleMap();
        msg.setMap(new HashMap<String, Integer>(){{
            put("A",1);
            put("B",2);
        }});

        DBusObject serialized = msg.serialize();

        SimpleMap received = this.sendAndReceive(new SimpleMapHandler(),msg);

        assertEquals(2,received.getMap().size());
        assertEquals(1,(int)received.getMap().get("A"));
        assertEquals(2,(int)received.getMap().get("B"));
    }

    @Test
    public void complexMapTest() throws InterruptedException {
        final ComplexMap.SubObject1 sub1 = new ComplexMap.SubObject1();
        final ComplexMap.SubObject1 sub2 = new ComplexMap.SubObject1();

        String[] array1 = new String[]{"A","String"};
        String[] array2 = new String[]{"Another","string"};

        sub1.setArray(array1);
        sub1.setInteger(42);

        sub2.setArray(array2);
        sub2.setInteger(50);

        ComplexMap msg = new ComplexMap();
        msg.setMap(new HashMap<String, ComplexMap.SubObject1>(){{
            put("A",sub1);
            put("B",sub2);
        }});

        ComplexMap received = this.sendAndReceive(new ComplexMapHandler(),msg);

        assertEquals(2,received.getMap().size());

        ComplexMap.SubObject1 receivedSub1 = received.getMap().get("A");
        ComplexMap.SubObject1 receivedSub2 = received.getMap().get("B");

        assertArrayEquals(array1,receivedSub1.getArray());
        assertArrayEquals(array2,receivedSub2.getArray());

        assertEquals(42,receivedSub1.getInteger());
        assertEquals(50,receivedSub2.getInteger());
    }
}
