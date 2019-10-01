package fr.viveris.jnidbus.types;

import java.util.Objects;

public class ObjectPath {
    private String path;

    public ObjectPath(String path){
        //TODO: implement format check
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectPath)) return false;
        ObjectPath that = (ObjectPath) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
