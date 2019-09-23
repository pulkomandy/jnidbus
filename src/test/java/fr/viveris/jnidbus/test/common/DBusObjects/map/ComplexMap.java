package fr.viveris.jnidbus.test.common.DBusObjects.map;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.Map;

@DBusType(
        signature = "a{s(asi)}",
        fields = {"map"}
)
public class ComplexMap extends Message {
    private Map<String,SubObject1> map;

    public Map<String, SubObject1> getMap() {
        return map;
    }

    public void setMap(Map<String, SubObject1> map) {
        this.map = map;
    }

    @DBusType(
            signature = "asi",
            fields = {"array","integer"}
    )
    public static class SubObject1 extends Message{
        private String[] array;
        private int integer;

        public String[] getArray() {
            return array;
        }

        public void setArray(String[] array) {
            this.array = array;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }
    }
}
