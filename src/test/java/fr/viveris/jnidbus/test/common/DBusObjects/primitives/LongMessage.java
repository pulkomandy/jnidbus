/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects.primitives;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "xxax",
        fields = {"primitive","boxed","list"}
)
public class LongMessage extends Message {
    private long primitive;
    private Long boxed;
    private List<Long> list;

    public long getPrimitive() {
        return primitive;
    }

    public void setPrimitive(long primitive) {
        this.primitive = primitive;
    }

    public Long getBoxed() {
        return boxed;
    }

    public void setBoxed(Long boxed) {
        this.boxed = boxed;
    }

    public List<Long> getList() {
        return list;
    }

    public void setList(List<Long> list) {
        this.list = list;
    }
}
