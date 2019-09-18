/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

@DBusType(
        signature = "ssii",
        fields = {"string1","string2","int1","int2"}
)
public class SimpleMessage extends Message {
    private String string1;
    private String string2;
    private int int1;
    private int int2;

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }

    public int getInt1() {
        return int1;
    }

    public void setInt1(int int1) {
        this.int1 = int1;
    }

    public int getInt2() {
        return int2;
    }

    public void setInt2(int int2) {
        this.int2 = int2;
    }
}
