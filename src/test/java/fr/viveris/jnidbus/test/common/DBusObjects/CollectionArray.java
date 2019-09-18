/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.ArrayList;
import java.util.List;

@DBusType(
        signature = "ai",
        fields = "array"
)
public class CollectionArray extends Message {
    private List<Integer> array = new ArrayList<>();

    public List<Integer> getArray() {
        return array;
    }

    public void setArray(List<Integer> array) {
        this.array = array;
    }
}
