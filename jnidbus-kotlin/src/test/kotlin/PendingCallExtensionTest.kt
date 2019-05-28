import Common.DBusObjects.SingleStringMessage
import Common.DBusTestCase
import fr.viveris.await
import fr.viveris.jnidbus.dispatching.GenericHandler
import fr.viveris.jnidbus.dispatching.MemberType
import fr.viveris.jnidbus.dispatching.annotation.Handler
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod
import fr.viveris.jnidbus.exception.DBusException
import fr.viveris.jnidbus.message.Message
import fr.viveris.jnidbus.message.PendingCall
import fr.viveris.jnidbus.message.Promise
import fr.viveris.jnidbus.remote.RemoteInterface
import fr.viveris.jnidbus.remote.RemoteMember
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PendingCallExtensionTest : DBusTestCase() {
    @Test
    fun `Extension suspend and return correct result`() = runBlocking{
        val handler = CallHandler()
        this@PendingCallExtensionTest.receiver.addHandler(handler)
        val remoteObj = this@PendingCallExtensionTest.sender.createRemoteObject(
                this@PendingCallExtensionTest.receiverBusName,
                "/Call/AsyncCallTest",
                AsyncCallTestRemote::class.java)

        val pending = remoteObj.blockingCall()
        val msg = withTimeout(2500){
            pending.await()
        }

        assertEquals("test",msg.string)
    }

    @Test
    fun `Extension suspend and return correct exception`() = runBlocking{
        val handler = CallHandler()
        this@PendingCallExtensionTest.receiver.addHandler(handler)
        val remoteObj = this@PendingCallExtensionTest.sender.createRemoteObject(
                this@PendingCallExtensionTest.receiverBusName,
                "/Call/AsyncCallTest",
                AsyncCallTestRemote::class.java)

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
        this@PendingCallExtensionTest.receiver.addHandler(handler)
        val remoteObj = this@PendingCallExtensionTest.sender.createRemoteObject(
                this@PendingCallExtensionTest.receiverBusName,
                "/Call/AsyncCallTest",
                AsyncCallTestRemote::class.java)

        val pending = remoteObj.blockingCall()
        pending.cancel()
        try{
            withTimeout(2500){
                pending.await()
            }
            fail("No exception raised")
        }catch (e : DBusException){
            assertEquals(PendingCall.PENDING_CALL_CANCELLED_ERROR_CODE,e.code)
        }
    }

    @Handler(path = "/Call/AsyncCallTest", interfaceName = "Call.AsyncCallTest")
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

    @RemoteInterface("Call.AsyncCallTest")
    interface AsyncCallTestRemote {
        @RemoteMember("blockingCall")
        fun blockingCall(): PendingCall<SingleStringMessage>

        @RemoteMember("failCall")
        fun failCall(): PendingCall<SingleStringMessage>
    }
}