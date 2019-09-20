/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.exception;

/**
 * thrown when a message is being checked and something is wrong
 */
public class MessageCheckException extends Exception{
    public MessageCheckException(String message, Class clazz) {
        super(String.format(message+" [class: %s ]",clazz.getName()));
    }

    public MessageCheckException(String message, Class clazz, String fieldName) {
        super(String.format(message+" [class: %s  field: %s]",clazz.getName(),fieldName));
    }
}
