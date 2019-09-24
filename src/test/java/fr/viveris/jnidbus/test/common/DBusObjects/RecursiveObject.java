/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

@DBusType(
        signature = "i(s)s",
        fields = {"integer","object","string"}
)
public class RecursiveObject extends Message {
    private int integer;
    private SubObject object = new SubObject();
    private String string;


    public int getInteger() {
        return integer;
    }

    public void setInteger(int integer) {
        this.integer = integer;
    }

    public SubObject getObject() {
        return object;
    }

    public void setObject(SubObject object) {
        this.object = object;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @DBusType(
            signature = "s",
            fields = {"string"}
    )
    public static class SubObject extends Message{
        private String string;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }
    }
}
