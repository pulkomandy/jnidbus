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
import fr.viveris.jnidbus.test.common.DBusObjects.primitives.BooleanMessage;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/primitive/boolean",
        interfaceName = "Handlers.Primitive.BooleanHandler"
)
public class BooleanHandler extends CommonHandler<BooleanMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(BooleanMessage msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<BooleanMessage> buildSignal(BooleanMessage value) {
        return new BooleanHandlerRemote.BooleanSignal(value);
    }

    @RemoteInterface("Handlers.Primitive.BooleanHandler")
    public interface BooleanHandlerRemote{

        @RemoteMember("handle")
        class BooleanSignal extends Signal<BooleanMessage> {
            public BooleanSignal(BooleanMessage params) {
                super(params);
            }
        }
    }
}
