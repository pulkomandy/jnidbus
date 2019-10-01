package fr.viveris.jnidbus.message.eventloop;

/**
 * Functionnal interface allowing the developper to be notified when the event loop actually processed the request
 */
public interface RequestCallback {
    /**
     * Called by the event loop when the request is processed. If the process raised an exception, it will be given as
     * an argument
     *
     * @param e the exception raised, can be null
     */
    void call(Exception e);
}
