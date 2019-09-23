package fr.viveris.jnidbus.test.common.DBusObjects.map;

import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.serialization.DBusType;

import java.util.Map;

@DBusType(
        signature = "a{si}",
        fields = {"map"}
)
public class SimpleMap extends Message {
    private Map<String,Integer> map;

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }
}
