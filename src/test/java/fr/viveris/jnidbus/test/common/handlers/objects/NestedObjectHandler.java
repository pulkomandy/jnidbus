package fr.viveris.jnidbus.test.common.handlers.objects;

import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.objects.NestedObject;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/object/nested",
        interfaceName = "Handlers.Object.NestedObjectHandler"
)
public class NestedObjectHandler extends CommonHandler<NestedObject> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(NestedObject msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<NestedObject> buildSignal(NestedObject value) {
        return new NestedObjectHandlerRemote.NestedObjectSignal(value);
    }

    @RemoteInterface("Handlers.Object.NestedObjectHandler")
    public interface NestedObjectHandlerRemote{

        @RemoteMember("handle")
        class NestedObjectSignal extends Signal<NestedObject> {
            public NestedObjectSignal(NestedObject params) {
                super(params);
            }
        }
    }
}
