package Common.handlers;

import Common.DBusObjects.primitives.LongMessage;
import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;

@Handler(
        path = "/handlers/primitive/long",
        interfaceName = "Handlers.Primitive.LongHandler"
)
public class LongHandler extends CommonHandler<LongMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(LongMessage msg){
        this.barrier.countDown();
        this.value = msg;
    }

    @RemoteInterface("Handlers.Primitive.LongHandler")
    public interface LongHandlerRemote{

        @RemoteMember("handle")
        class LongSignal extends Signal<LongMessage> {
            public LongSignal(LongMessage params) {
                super(params);
            }
        }
    }
}
