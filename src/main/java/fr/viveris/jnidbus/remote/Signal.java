/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.remote;

import fr.viveris.jnidbus.serialization.Serializable;

/**
 * Class any JNIDBus signal must extend, the generic type will be the data attached to the signal
 * @param <In> data type attached to the signal
 */
public class Signal<In extends Serializable> {
    private In param;

    public Signal(In param) {
        this.param = param;
    }

    public In getParam() {
        return param;
    }

    /**
     * Return the interface of the signal, by default the RemoteInterface annotation of the enclosing class will be used.
     * This method can be overridden for test purposes or to declare signals outside of an enclosing class
     * @return
     */
    public String getRemoteInterface(){
        Class<?> enclosing = this.getClass().getEnclosingClass();
        if(enclosing == null) throw new IllegalArgumentException("The signal class must be enclosed by an interface annotated with RemoteInterface");

        RemoteInterface remoteInterface = enclosing.getAnnotation(RemoteInterface.class);
        if(remoteInterface == null) throw new IllegalArgumentException("The enclosing interface must be annotated with RemoteInterface");

        return remoteInterface.value();
    }

    public String getRemoteMember(){
        RemoteMember remoteMember = this.getClass().getAnnotation(RemoteMember.class);
        if(remoteMember == null) throw new IllegalArgumentException("The signal class must be annotated with RemoteMember");
        return remoteMember.value();
    }
}
