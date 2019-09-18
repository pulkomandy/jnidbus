/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

@DBusType(
        signature = "s",
        fields = "string"
)
public class SingleStringMessage extends Message {
    private String string;

    public String getString() {
        return string;
    }
    public void setString(String string) {
        this.string = string;
    }
}
