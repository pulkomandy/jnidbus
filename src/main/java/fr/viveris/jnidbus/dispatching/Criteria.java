/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.dispatching;

import java.util.Objects;

/**
 * The dispatcher will create a criteria for each message it wants to dispatch and try to match it against the criteria
 * of the registered handlers to find which method to call and how to deserialize the message. Each handler method will
 * correspond to a precise criteria and two handler method can not have the same criteria.
 *
 * The criteria will use the member, input signature and type to match against another criteria, the output signature is
 * only there for debug purposes.
 */
public class Criteria {

    private String member;
    private String inputSignature;
    private String outputSignature;
    private MemberType type;

    /**
     * Create a new criteria, its output signature can be null
     *
     * @param member dbus member to match
     * @param inputSignature input signature to match
     * @param outputSignature used for debug only, as DBus does not do any match on output signature it is just a way for the developer
     *        to quickly see what DBus signature is generated.
     * @param type type of message to match (call or signal)
     */
    public Criteria(String member, String inputSignature, String outputSignature, MemberType type) {
        this.member = member;
        this.inputSignature = inputSignature;
        this.outputSignature = outputSignature;
        this.type = type;
    }

    public String getMember() {
        return member;
    }

    public String getInputSignature() {
        return inputSignature;
    }

    public String getOutputSignature() { return outputSignature; }

    public MemberType getType() { return type; }

    @Override
    public String toString() {
        return "Criteria{" +
                "member='" + member + '\'' +
                ", inputSignature='" + inputSignature + '\'' +
                ", outputSignature='" + outputSignature + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Criteria)) return false;
        Criteria criteria = (Criteria) o;
        return member.equals(criteria.member) &&
                inputSignature.equals(criteria.inputSignature) &&
                type == criteria.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(member, inputSignature, type);
    }

}
