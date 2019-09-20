package fr.viveris.jnidbus.test.common.handlers.arrays;

import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.arrays.PrimitiveArray;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/arrays/primitive",
        interfaceName = "Handlers.Arrays.Primitive"
)
public class PrimitiveArrayHandler extends CommonHandler<PrimitiveArray> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(PrimitiveArray msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<PrimitiveArray> buildSignal(PrimitiveArray value) {
        return new PrimitiveArrayHandler.PrimitiveArrayHandlerRemote.PrimitiveArraySignal(value);
    }

    @RemoteInterface("Handlers.Arrays.Primitive")
    public interface PrimitiveArrayHandlerRemote{

        @RemoteMember("handle")
        class PrimitiveArraySignal extends Signal<PrimitiveArray> {
            public PrimitiveArraySignal(PrimitiveArray params) {
                super(params);
            }
        }
    }
}
