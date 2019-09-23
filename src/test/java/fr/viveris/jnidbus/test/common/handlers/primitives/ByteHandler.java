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
import fr.viveris.jnidbus.test.common.DBusObjects.primitives.ByteMessage;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/primitive/byte",
        interfaceName = "Handlers.Primitive.ByteHandler"
)
public class ByteHandler extends CommonHandler<ByteMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(ByteMessage msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<ByteMessage> buildSignal(ByteMessage value) {
        return new ByteHandlerRemote.ByteSignal(value);
    }

    @RemoteInterface("Handlers.Primitive.ByteHandler")
    public interface ByteHandlerRemote{

        @RemoteMember("handle")
        class ByteSignal extends Signal<ByteMessage> {
            public ByteSignal(ByteMessage params) {
                super(params);
            }
        }
    }
}
