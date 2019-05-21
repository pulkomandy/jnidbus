package Common.handlers;

import Common.DBusObjects.primitives.ShortMessage;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;

@Handler(
        path = "/handlers/primitive/short",
        interfaceName = "Handlers.Primitive.ShortHandler"
)
public class ShortHandler extends CommonHandler<ShortMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(ShortMessage msg){
        this.barrier.countDown();
        this.value = msg;
    }

    @RemoteInterface("Handlers.Primitive.ShortHandler")
    public interface ShortHandlerRemote{

        @RemoteMember("handle")
        class ShortSignal extends Signal<ShortMessage> {
            public ShortSignal(ShortMessage params) {
                super(params);
            }
        }
    }
}
