/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.test.common.handlers.primitives;

import fr.viveris.jnidbus.dispatching.MemberType;
import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.dispatching.annotation.HandlerMethod;
import fr.viveris.jnidbus.remote.RemoteInterface;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.remote.Signal;
import fr.viveris.jnidbus.test.common.DBusObjects.primitives.DoubleMessage;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/primitive/double",
        interfaceName = "Handlers.Primitive.DoubleHandler"
)
public class DoubleHandler extends CommonHandler<DoubleMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(DoubleMessage msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<DoubleMessage> buildSignal(DoubleMessage value) {
        return new DoubleHandlerRemote.DoubleSignal(value);
    }

    @RemoteInterface("Handlers.Primitive.DoubleHandler")
    public interface DoubleHandlerRemote{

        @RemoteMember("handle")
        class DoubleSignal extends Signal<DoubleMessage> {
            public DoubleSignal(DoubleMessage params) {
                super(params);
            }
        }
    }
}
