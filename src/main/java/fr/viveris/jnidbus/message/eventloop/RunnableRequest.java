package fr.viveris.jnidbus.message.eventloop;

/**
 * Ask the EventLoop to execute the given Runnable
 */
public class RunnableRequest extends EventLoopRequest {
    private Runnable runnable;

    public RunnableRequest(Runnable runnable, RequestCallback callback) {
        super(callback);
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return runnable;
    }
}
