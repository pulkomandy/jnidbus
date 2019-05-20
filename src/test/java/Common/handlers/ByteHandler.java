package Common.handlers;

import Common.DBusObjects.primitives.ByteMessage;
import fr.viveris.jnidbus.dispatching.HandlerType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.message.DbusSignal;
import fr.viveris.jnidbus.message.Signal;

@Handler(
        path = "/handlers/primitive/byte",
        interfaceName = "Handlers.Primitive.ByteHandler"
)
public class ByteHandler extends CommonHandler<ByteMessage> {

    @HandlerMethod(
            member = "handle",
            type = HandlerType.SIGNAL
    )
    public void handle(ByteMessage msg){
        this.barrier.countDown();
        this.value = msg;
    }

    @DbusSignal(
            path = "/handlers/primitive/byte",
            interfaceName = "Handlers.Primitive.ByteHandler",
            member = "handle"
    )
    public static class ByteSignal extends Signal<ByteMessage>{
        public ByteSignal(ByteMessage params) {
            super(params);
        }
    }
}