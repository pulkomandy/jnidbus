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
import fr.viveris.jnidbus.test.common.DBusObjects.primitives.LongMessage;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

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
        this.doHandle(msg);
    }

    @Override
    public Signal<LongMessage> buildSignal(LongMessage value) {
        return new LongHandlerRemote.LongSignal(value);
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
