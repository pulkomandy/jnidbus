package fr.viveris.jnidbus

import fr.viveris.jnidbus.exception.DBusException
import fr.viveris.jnidbus.message.Promise
import fr.viveris.jnidbus.serialization.Serializable
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * suspend the current coroutine until a result or an error is received. This function will throw the exception received
 * by the Promise.
 */
suspend fun <T : Serializable> Promise<T>.await() : T = suspendCancellableCoroutine{cont ->
    this.then{value,exception ->
        if(exception != null) cont.resumeWithException(exception)
        else cont.resume(value)
    }

    cont.invokeOnCancellation {
        this.fail(CancellationException("The call was cancelled by the coroutine"));
    }
}