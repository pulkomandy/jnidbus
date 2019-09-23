/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.cache;

import fr.viveris.jnidbus.remote.Signal;

public class SignalMetadata {
    private String interfaceName;
    private String member;

    /**
     * Create a new cache entry for the given signal class. The class will be checked and an exception will be thrown if
     * an error was found
     * @param signal signal class to analyze
     */
    public SignalMetadata(Signal signal){
        this.interfaceName = signal.getRemoteInterface();
        this.member = signal.getRemoteMember();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMember() {
        return member;
    }
}
