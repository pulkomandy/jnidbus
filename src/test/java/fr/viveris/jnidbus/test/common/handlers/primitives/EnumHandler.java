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
import fr.viveris.jnidbus.test.common.DBusObjects.primitives.EnumMessage;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/primitive/enum",
        interfaceName = "Handlers.Primitive.EnumHandler"
)
public class EnumHandler extends CommonHandler<EnumMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(EnumMessage msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<EnumMessage> buildSignal(EnumMessage value) {
        return new EnumHandlerRemote.EnumSignal(value);
    }

    @RemoteInterface("Handlers.Primitive.EnumHandler")
    public interface EnumHandlerRemote{

        @RemoteMember("handle")
        class EnumSignal extends Signal<EnumMessage> {
            public EnumSignal(EnumMessage params) {
                super(params);
            }
        }
    }
}
