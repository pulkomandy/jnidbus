/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects.primitives;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "ddad",
        fields = {"primitive","boxed","list"}
)
public class DoubleMessage extends Message {
    private double primitive;
    private Double boxed;
    private List<Double> list;

    public double getPrimitive() {
        return primitive;
    }

    public void setPrimitive(double primitive) {
        this.primitive = primitive;
    }

    public Double getBoxed() {
        return boxed;
    }

    public void setBoxed(Double boxed) {
        this.boxed = boxed;
    }

    public List<Double> getList() {
        return list;
    }

    public void setList(List<Double> list) {
        this.list = list;
    }
}
