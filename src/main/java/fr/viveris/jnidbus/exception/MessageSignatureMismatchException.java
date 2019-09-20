/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.exception;

/**
 * Thrown by a Message which could not be deserialized due to an input DBusObject having a different signature than the
 * one of the Message being deserialized
 */
public class MessageSignatureMismatchException extends Exception {
    public MessageSignatureMismatchException(String message) {
        super(message);
    }
}
