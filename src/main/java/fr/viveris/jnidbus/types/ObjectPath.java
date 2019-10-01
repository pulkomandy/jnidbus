package fr.viveris.jnidbus.types;

import java.util.Objects;
import java.util.regex.Pattern;

public class ObjectPath {
    private static final Pattern format = Pattern.compile("^\\/([a-zA-Z1-9_]+\\/?)+(?<!\\/)$");
    private String path;

    public ObjectPath(String path){
        if(!this.matchFormat(path)) throw new IllegalArgumentException("The given path do not respect the DBus object path format");
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if(!this.matchFormat(path)) throw new IllegalArgumentException("The given path do not respect the DBus object path format");
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

    private boolean matchFormat(String path){
        return format.matcher(path).matches();
    }
}
