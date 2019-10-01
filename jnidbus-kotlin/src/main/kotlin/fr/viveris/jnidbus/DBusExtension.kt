package fr.viveris.jnidbus

import fr.viveris.jnidbus.dispatching.GenericHandler
import fr.viveris.jnidbus.remote.Signal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun Dbus.sendSignal(objectPath : String, signal : Signal<*>) : Unit = suspendCoroutine{cont ->
    this.sendSignal(objectPath,signal){
        if(it == null){
            cont.resumeWithException(it)
        }else{
            cont.resume(Unit);
        }
    }
}

suspend fun Dbus.addHandler(handler : GenericHandler) : Unit = suspendCoroutine{cont ->
    this.addHandler(handler){
        if(it == null){
            cont.resumeWithException(it)
        }else{
            cont.resume(Unit);
        }
    }
}

suspend fun Dbus.removeHandler(handler : GenericHandler) : Unit = suspendCoroutine{cont ->
    this.removeHandler(handler){
        if(it == null){
            cont.resumeWithException(it)
        }else{
            cont.resume(Unit);
        }
    }
}