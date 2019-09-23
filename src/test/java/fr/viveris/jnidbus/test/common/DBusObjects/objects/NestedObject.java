package fr.viveris.jnidbus.test.common.DBusObjects.objects;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.List;

@DBusType(
        signature = "a(a(iais))",
        fields = {"objects"}
)
public class NestedObject extends Message {
    private List<SubObject1> objects;

    public List<SubObject1> getObjects() {
        return objects;
    }

    public void setObjects(List<SubObject1> objects) {
        this.objects = objects;
    }

    @DBusType(
            signature = "a(iais)",
            fields = {"objects"}
    )
    public static class SubObject1 extends Message{
        private List<SubObject2> objects;

        public List<SubObject2> getObjects() {
            return objects;
        }

        public void setObjects(List<SubObject2> objects) {
            this.objects = objects;
        }
    }

    @DBusType(
            signature = "iais",
            fields = {"integer","intArrays","string"}
    )
    public static class SubObject2 extends Message{
        private int integer;
        private int[] intArrays;
        private String string;

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

        public int[] getIntArrays() {
            return intArrays;
        }

        public void setIntArrays(int[] intArrays) {
            this.intArrays = intArrays;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }
    }
}
