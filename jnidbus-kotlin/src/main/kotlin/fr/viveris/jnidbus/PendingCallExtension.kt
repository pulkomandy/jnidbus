package fr.viveris.jnidbus

import fr.viveris.jnidbus.exception.DBusException
import fr.viveris.jnidbus.message.PendingCall
import fr.viveris.jnidbus.serialization.Serializable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * suspend the current coroutine until a result or an error is received. If the coroutine is cancelled, the PendingCall
 * will also get cancelled. This function will throw the exception received by the PendingCall.
 */
suspend fun <T : Serializable> PendingCall<T>.await() : T = suspendCancellableCoroutine{cont ->
    this.setListener(object : PendingCall.Listener<T>{
        override fun notify(value: T) {
            cont.resume(value)
        }

        override fun error(t: DBusException?) {
            cont.resumeWithException(t!!)
        }
    })

    cont.invokeOnCancellation {
        this.cancel()
    }
}