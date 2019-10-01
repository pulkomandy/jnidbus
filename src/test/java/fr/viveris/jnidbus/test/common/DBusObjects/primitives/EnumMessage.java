/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects.primitives;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "sasasiaiai",
        fields = {"byName","byNameArray","byNameList","byOrdinal","byOrdinalArray","byOrdinalList"}
)
public class EnumMessage extends Message {
    private Enum byName;
    private Enum[] byNameArray;
    private List<Enum> byNameList;

    private Enum byOrdinal;
    private Enum[] byOrdinalArray;
    private List<Enum> byOrdinalList;

    public Enum getByName() {
        return byName;
    }

    public void setByName(Enum byName) {
        this.byName = byName;
    }

    public Enum[] getByNameArray() {
        return byNameArray;
    }

    public void setByNameArray(Enum[] byNameArray) {
        this.byNameArray = byNameArray;
    }

    public List<Enum> getByNameList() {
        return byNameList;
    }

    public void setByNameList(List<Enum> byNameList) {
        this.byNameList = byNameList;
    }

    public Enum getByOrdinal() {
        return byOrdinal;
    }

    public void setByOrdinal(Enum byOrdinal) {
        this.byOrdinal = byOrdinal;
    }

    public Enum[] getByOrdinalArray() {
        return byOrdinalArray;
    }

    public void setByOrdinalArray(Enum[] byOrdinalArray) {
        this.byOrdinalArray = byOrdinalArray;
    }

    public List<Enum> getByOrdinalList() {
        return byOrdinalList;
    }

    public void setByOrdinalList(List<Enum> byOrdinalList) {
        this.byOrdinalList = byOrdinalList;
    }

    public static enum Enum{A,B,C}
}
