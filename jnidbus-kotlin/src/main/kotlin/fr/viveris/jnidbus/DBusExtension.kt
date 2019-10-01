package fr.viveris.jnidbus

import fr.viveris.jnidbus.dispatching.GenericHandler
import fr.viveris.jnidbus.remote.Signal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Ask the event loop to send the signal and suspend the coroutine until the signal is effectively sent. The method will
 * throw if DBus failed to send the signal
 */
suspend fun Dbus.sendSignal(objectPath : String, signal : Signal<*>) : Unit = suspendCoroutine{cont ->
    this.sendSignal(objectPath,signal){
        if(it == null){
            cont.resumeWithException(it)
        }else{
            cont.resume(Unit);
        }
    }
}

/**
 * Ask the event loop to register the given handler and suspend the coroutine until it is effectively done
 */
suspend fun Dbus.addHandler(handler : GenericHandler) : Unit = suspendCoroutine{cont ->
    this.addHandler(handler){
        if(it == null){
            cont.resumeWithException(it)
        }else{
            cont.resume(Unit);
        }
    }
}

/**
 * Ask the event loop to unregister the given handler and suspend the coroutine until it is effectively done
 */
suspend fun Dbus.removeHandler(handler : GenericHandler) : Unit = suspendCoroutine{cont ->
    this.removeHandler(handler){
        if(it == null){
            cont.resumeWithException(it)
        }else{
            cont.resume(Unit);
        }
    }
}