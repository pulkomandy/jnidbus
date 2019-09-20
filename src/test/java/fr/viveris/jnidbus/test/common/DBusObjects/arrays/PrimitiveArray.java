package fr.viveris.jnidbus.test.common.DBusObjects.arrays;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "aiaiai",
        fields = {"primitive","primitiveBoxed", "collection"}
)
public class PrimitiveArray extends Message {
    private int[] primitive;
    private Integer[] primitiveBoxed;
    private List<Integer> collection;

    public int[] getPrimitive() {
        return primitive;
    }

    public void setPrimitive(int[] primitive) {
        this.primitive = primitive;
    }

    public Integer[] getPrimitiveBoxed() {
        return primitiveBoxed;
    }

    public void setPrimitiveBoxed(Integer[] primitiveBoxed) {
        this.primitiveBoxed = primitiveBoxed;
    }

    public List<Integer> getCollection() {
        return collection;
    }

    public void setCollection(List<Integer> collection) {
        this.collection = collection;
    }
}
