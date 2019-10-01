/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects.primitives;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;
import fr.viveris.jnidbus.types.ObjectPath;

import java.util.List;

@DBusType(
        signature = "oao",
        fields = {"primitive","list"}
)
public class ObjectPathMessage extends Message {
    private ObjectPath primitive;
    private List<ObjectPath> list;

    public ObjectPath getPrimitive() {
        return primitive;
    }

    public void setPrimitive(ObjectPath primitive) {
        this.primitive = primitive;
    }

    public List<ObjectPath> getList() {
        return list;
    }

    public void setList(List<ObjectPath> list) {
        this.list = list;
    }
}
