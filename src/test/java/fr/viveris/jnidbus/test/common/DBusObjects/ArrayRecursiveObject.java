/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.DBusObjects;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.ArrayList;
import java.util.List;

@DBusType(
        signature = "a(asi)",
        fields = {"objects"}
)
public class ArrayRecursiveObject extends Message {
    List<SubArrayRecursiveObject> objects = new ArrayList<>();

    public List<SubArrayRecursiveObject> getObjects() {
        return objects;
    }

    public void setObjects(List<SubArrayRecursiveObject> objects) {
        this.objects = objects;
    }

    @DBusType(
            signature = "asi",
            fields = {"strings","integer"}
    )
    public static class SubArrayRecursiveObject extends Message{
        List<String> strings = new ArrayList<>();
        int integer;

        public List<String> getStrings() {
            return strings;
        }

        public void setStrings(List<String> strings) {
            this.strings = strings;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }
    }
}
