/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects.primitives;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "bbab",
        fields = {"primitive","boxed","list"}
)
public class BooleanMessage extends Message {
    private boolean primitive;
    private Boolean boxed;
    private List<Boolean> list;

    public boolean getPrimitive() {
        return primitive;
    }

    public void setPrimitive(boolean primitive) {
        this.primitive = primitive;
    }

    public Boolean getBoxed() {
        return boxed;
    }

    public void setBoxed(Boolean boxed) {
        this.boxed = boxed;
    }

    public List<Boolean> getList() {
        return list;
    }

    public void setList(List<Boolean> list) {
        this.list = list;
    }
}
