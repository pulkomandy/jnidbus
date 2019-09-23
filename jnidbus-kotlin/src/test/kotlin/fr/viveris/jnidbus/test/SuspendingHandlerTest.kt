package fr.viveris.jnidbus.test

import fr.viveris.jnidbus.await
import fr.viveris.jnidbus.dispatching.KotlinGenericHandler
import fr.viveris.jnidbus.dispatching.KotlinMethodInvocator
import fr.viveris.jnidbus.dispatching.MemberType
import fr.viveris.jnidbus.dispatching.annotation.Handler
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod
import fr.viveris.jnidbus.message.Message
import fr.viveris.jnidbus.message.PendingCall
import fr.viveris.jnidbus.remote.RemoteInterface
import fr.viveris.jnidbus.remote.RemoteMember
import fr.viveris.jnidbus.test.common.DBusObjects.SingleStringMessage
import fr.viveris.jnidbus.test.common.DBusTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SuspendingHandlerTest : DBusTestCase(){

    @BeforeTest
    fun registerInvocator(){
        KotlinMethodInvocator.registerKotlinInvocator()
    }

    @Test
    fun `Handler can suspend and return value`() = runBlocking{
        val handler = CallHandler()
        this@SuspendingHandlerTest.receiver.addHandler(handler)
        val remoteObj = this@SuspendingHandlerTest.sender.createRemoteObject(
                this@SuspendingHandlerTest.receiverBusName,
                "/Kotlin/SuspendingHandlerTest",
                SuspendingHandlerTestRemote::class.java)

        val pending = remoteObj.blockingCall()
        val msg = withTimeout(2500){
            pending.await()
        }

        assertEquals("test",msg.string)
    }

    @Test
    fun `Handler can do standard calls`() = runBlocking{
        val handler = CallHandler()
        this@SuspendingHandlerTest.receiver.addHandler(handler)
        val remoteObj = this@SuspendingHandlerTest.sender.createRemoteObject(
                this@SuspendingHandlerTest.receiverBusName,
                "/Kotlin/SuspendingHandlerTest",
                SuspendingHandlerTestRemote::class.java)

        val pending = remoteObj.standardCall()
        val msg = withTimeout(2500){
            pending.await()
        }

        assertEquals("test",msg.string)
    }

    @Handler(path = "/Kotlin/SuspendingHandlerTest", interfaceName = "Kotlin.SuspendingHandlerTest")
    class CallHandler : KotlinGenericHandler() {

        @HandlerMethod(member = "blockingCallWithSuspend", type = MemberType.METHOD)
        @Suppress("UNUSED_PARAMETER")
        suspend fun blockingCallWithSuspend(emptyMessage: Message.EmptyMessage): SingleStringMessage {
            delay(2000)
            return SingleStringMessage().apply { string = "test" }
        }

        @HandlerMethod(member = "standardCall", type = MemberType.METHOD)
        @Suppress("UNUSED_PARAMETER")
        fun standardCall(emptyMessage: Message.EmptyMessage): SingleStringMessage {
            return SingleStringMessage().apply { string = "test" }
        }
    }

    @RemoteInterface("Kotlin.SuspendingHandlerTest")
    interface SuspendingHandlerTestRemote {

        @RemoteMember("blockingCallWithSuspend")
        fun blockingCall(): PendingCall<SingleStringMessage>

        @RemoteMember("standardCall")
        fun standardCall(): PendingCall<SingleStringMessage>
    }
}