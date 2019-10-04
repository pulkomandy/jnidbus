package fr.viveris.jnidbus.dispatching

import fr.viveris.jnidbus.exception.DBusException
import fr.viveris.jnidbus.message.Promise
import fr.viveris.jnidbus.serialization.Serializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

/**
 * Invocator used by jnidbus to process suspending kotlin functions, the invocator must be explicitly register using
 * the static KotlinMethodInvocator.registerKotlinInvocator() method
 */
class KotlinMethodInvocator(
        val scope : CoroutineScope
) : HandlerMethod.MethodInvocator{
    /**
     * Equivalent of the static{} initializer in Java
     */
    companion object{
        /**
         * Register the invocator to jnidbus, a scope can be given to customize where the suspending handlers should
         * be executed
         */
        fun registerKotlinInvocator(scope : CoroutineScope = GlobalScope) {
            HandlerMethod.kotlinInvocator = KotlinMethodInvocator(scope)
        }
    }

    /**
     * Used by jnidbus to call the suspending handler. A suspending handler will always return a promise and be launched
     * in the GlobalScope using the common worker pool.
     *
     * @param handler handler instance
     * @param method method to call, must be a kotlin method
     * @param param parameter to use to call the handler
     *
     * @return Any must be used because of the way the jnidbus Dispatcher is built, but it will always return a Promise
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Serializable>call(handler : Any?, method: Method, param: Serializable?): Any {
        val promise = Promise<T>()
        val kotlinMethod = method.kotlinFunction!!

        if(!kotlinMethod.isSuspend) return kotlinMethod.call(handler,param) as T

        val job = this.scope.launch {
            val returned = kotlinMethod.callSuspend(handler,param)
            if(returned !is Promise<*>) promise.resolve(returned as T)
            else{
                throw Exception("Suspending functions can not return Promises")
            }
        }
        job.invokeOnCompletion {
            //if the job complete exceptionally (cancel or exception) fail the promise
            if(it != null){
                promise.fail(DBusException(it.javaClass.name,it.message))
            }
        }
        return promise
    }
}