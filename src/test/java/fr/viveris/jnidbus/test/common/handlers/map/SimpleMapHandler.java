package fr.viveris.jnidbus.test.common.handlers.map;

import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.map.SimpleMap;
import fr.viveris.jnidbus.test.common.DBusObjects.objects.NestedObject;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/map/simple",
        interfaceName = "Handlers.Map.SimpleMapHandler"
)
public class SimpleMapHandler extends CommonHandler<SimpleMap> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(SimpleMap msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<SimpleMap> buildSignal(SimpleMap value) {
        return new SimpleMapHandlerRemote.SimpleMapSignal(value);
    }

    @RemoteInterface("Handlers.Map.SimpleMapHandler")
    public interface SimpleMapHandlerRemote{

        @RemoteMember("handle")
        class SimpleMapSignal extends Signal<SimpleMap> {
            public SimpleMapSignal(SimpleMap params) {
                super(params);
            }
        }
    }
}

