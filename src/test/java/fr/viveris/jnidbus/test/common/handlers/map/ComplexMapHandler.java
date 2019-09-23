package fr.viveris.jnidbus.test.common.handlers.map;

import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.map.ComplexMap;
import fr.viveris.jnidbus.test.common.DBusObjects.map.SimpleMap;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/map/complex",
        interfaceName = "Handlers.Map.ComplexMapHandler"
)
public class ComplexMapHandler extends CommonHandler<ComplexMap> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(ComplexMap msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<ComplexMap> buildSignal(ComplexMap value) {
        return new ComplexMapHandlerRemote.ComplexMapSignal(value);
    }

    @RemoteInterface("Handlers.Map.ComplexMapHandler")
    public interface ComplexMapHandlerRemote{

        @RemoteMember("handle")
        class ComplexMapSignal extends Signal<ComplexMap> {
            public ComplexMapSignal(ComplexMap params) {
                super(params);
            }
        }
    }
}

