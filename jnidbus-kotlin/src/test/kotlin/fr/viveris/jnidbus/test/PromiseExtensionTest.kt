package fr.viveris.jnidbus.test

import fr.viveris.jnidbus.addHandler
import fr.viveris.jnidbus.await
import fr.viveris.jnidbus.dispatching.GenericHandler
import fr.viveris.jnidbus.dispatching.MemberType
import fr.viveris.jnidbus.dispatching.annotation.Handler
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod
import fr.viveris.jnidbus.exception.DBusException
import fr.viveris.jnidbus.message.Message
import fr.viveris.jnidbus.message.Promise
import fr.viveris.jnidbus.remote.RemoteInterface
import fr.viveris.jnidbus.remote.RemoteMember
import fr.viveris.jnidbus.removeHandler
import fr.viveris.jnidbus.test.common.DBusObjects.SingleStringMessage
import fr.viveris.jnidbus.test.common.DBusTestCase
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PromiseExtensionTest : DBusTestCase() {
    @Test
    fun `Extension suspend and return correct result`() = runBlocking{
        val handler = CallHandler()
        this@PromiseExtensionTest.receiver.addHandler(handler)
        val remoteObj = this@PromiseExtensionTest.sender.createRemoteObject(
                this@PromiseExtensionTest.receiverBusName,
                "/Kotlin/PromiseExtensionTest",
                PromiseExtensionTestRemote::class.java)

        val pending = remoteObj.blockingCall()
        val msg = withTimeout(2500){
            pending.await()
        }
        this@PromiseExtensionTest.receiver.removeHandler(handler)

        assertEquals("test",msg.string)
    }

    @Test
    fun `Extension suspend and return correct exception`() = runBlocking{
        val handler = CallHandler()
        this@PromiseExtensionTest.receiver.addHandler(handler)
        val remoteObj = this@PromiseExtensionTest.sender.createRemoteObject(
                this@PromiseExtensionTest.receiverBusName,
                "/Kotlin/PromiseExtensionTest",
                PromiseExtensionTestRemote::class.java)

        val pending = remoteObj.failCall()
        try{
            withTimeout(2500){
                pending.await()
            }
            fail("No exception raised")
        }catch (e : DBusException){
            assertEquals(DBusException.METHOD_NOT_FOUND_CODE,e.code)
        }
    }

    @Test
    fun `Manual pending call cancellation cancels continuation`() = runBlocking{
        val handler = CallHandler()
        this@PromiseExtensionTest.receiver.addHandler(handler)
        val remoteObj = this@PromiseExtensionTest.sender.createRemoteObject(
                this@PromiseExtensionTest.receiverBusName,
                "/Kotlin/PromiseExtensionTest",
                PromiseExtensionTestRemote::class.java)

        val pending = remoteObj.blockingCall()
        val exc = Exception("Cancelled!");
        pending.fail(exc)
        try{
            withTimeout(2500){
                pending.await()
            }
            fail("No exception raised")
        }catch (e : Exception){
            assertEquals(exc.message,e.message)
        }
    }

    @Handler(path = "/Kotlin/PromiseExtensionTest", interfaceName = "Kotlin.PromiseExtensionTest")
    class CallHandler : GenericHandler() {

        @HandlerMethod(member = "blockingCall", type = MemberType.METHOD)
        fun blockingCall(emptyMessage: Message.EmptyMessage): Promise<SingleStringMessage> {
            val promise = Promise<SingleStringMessage>()
            GlobalScope.launch {
                delay(2000)
                promise.resolve(SingleStringMessage().apply { string = "test" })
            }
            return promise
        }
    }

    @RemoteInterface("Kotlin.PromiseExtensionTest")
    interface PromiseExtensionTestRemote {
        @RemoteMember("blockingCall")
        fun blockingCall(): Promise<SingleStringMessage>

        @RemoteMember("failCall")
        fun failCall(): Promise<SingleStringMessage>
    }
}