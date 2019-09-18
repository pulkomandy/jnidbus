/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects.primitives;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "nnan",
        fields = {"primitive","boxed","list"}
)
public class ShortMessage extends Message {
    private short primitive;
    private Short boxed;
    private List<Short> list;

    public short getPrimitive() {
        return primitive;
    }

    public void setPrimitive(short primitive) {
        this.primitive = primitive;
    }

    public Short getBoxed() {
        return boxed;
    }

    public void setBoxed(Short boxed) {
        this.boxed = boxed;
    }

    public List<Short> getList() {
        return list;
    }

    public void setList(List<Short> list) {
        this.list = list;
    }
}
