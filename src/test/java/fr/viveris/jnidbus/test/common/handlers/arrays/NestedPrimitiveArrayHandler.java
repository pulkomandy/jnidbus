package fr.viveris.jnidbus.test.common.handlers.arrays;

import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.arrays.NestedPrimitiveArray;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/arrays/nestedPrimitive",
        interfaceName = "Handlers.Arrays.NestedPrimitive"
)
public class NestedPrimitiveArrayHandler extends CommonHandler<NestedPrimitiveArray> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(NestedPrimitiveArray msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<NestedPrimitiveArray> buildSignal(NestedPrimitiveArray value) {
        return new NestedPrimitiveArrayHandler.PrimitiveArrayHandlerRemote.NestedPrimitiveArraySignal(value);
    }

    @RemoteInterface("Handlers.Arrays.NestedPrimitive")
    public interface PrimitiveArrayHandlerRemote{

        @RemoteMember("handle")
        class NestedPrimitiveArraySignal extends Signal<NestedPrimitiveArray> {
            public NestedPrimitiveArraySignal(NestedPrimitiveArray params) {
                super(params);
            }
        }
    }
}
