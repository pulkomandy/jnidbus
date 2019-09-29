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
import fr.viveris.jnidbus.test.common.DBusObjects.primitives.ObjectPathMessage;
import fr.viveris.jnidbus.test.common.handlers.CommonHandler;

@Handler(
        path = "/handlers/primitive/object_path",
        interfaceName = "Handlers.Primitive.ObjectPathHandler"
)
public class ObjectPathHandler extends CommonHandler<ObjectPathMessage> {

    @HandlerMethod(
            member = "handle",
            type = MemberType.SIGNAL
    )
    public void handle(ObjectPathMessage msg){
        this.doHandle(msg);
    }

    @Override
    public Signal<ObjectPathMessage> buildSignal(ObjectPathMessage value) {
        return new ObjectPathHandlerRemote.ObjectPathSignal(value);
    }

    @RemoteInterface("Handlers.Primitive.ObjectPathHandler")
    public interface ObjectPathHandlerRemote{

        @RemoteMember("handle")
        class ObjectPathSignal extends Signal<ObjectPathMessage> {
            public ObjectPathSignal(ObjectPathMessage params) {
                super(params);
            }
        }
    }
}
