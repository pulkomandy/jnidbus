/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.cache;

import fr.viveris.jnidbus.exception.RemoteObjectCheckException;
import fr.viveris.jnidbus.message.Message;
import fr.viveris.jnidbus.message.Promise;
import fr.viveris.jnidbus.remote.RemoteMember;
import fr.viveris.jnidbus.serialization.Serializable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RemoteObjectMetadata {
    private Method method;
    private String member;
    private MessageMetadata inputMetadata;
    private MessageMetadata outputMetadata;

    /**
     * Create a new cache entry for the given remote method call. the method will be checked and an exception will be
     * thrown if an error was detected
     *
     * @param method method to analyze
     * @throws RemoteObjectCheckException thrown when the method is not a valid remote object method
     */
    @SuppressWarnings("unchecked")
    public RemoteObjectMetadata(Method method) throws RemoteObjectCheckException {
        this.method = method;

        RemoteMember annotation = method.getAnnotation(RemoteMember.class);
        if(annotation == null) throw new RemoteObjectCheckException("No RemoteMember annotation were found");

        this.member = annotation.value();

        //process input type
        Class<?>[] params = method.getParameterTypes();
        if(params.length == 0){
            this.inputMetadata = Message.retrieveFromCache(Message.EmptyMessage.class);
        } else if(params.length == 1){
            if(!Serializable.class.isAssignableFrom(params[0])) throw new RemoteObjectCheckException("A remote method parameter must be serializable");
            Class<? extends Serializable> inputClass = params[0].asSubclass(Serializable.class);
            this.inputMetadata = Message.retrieveFromCache(inputClass);
        }else{
            throw new RemoteObjectCheckException("A remote method can only have one parameter");
        }

        //process output type
        Type output = method.getGenericReturnType();
        if(!(output instanceof ParameterizedType)) throw new RemoteObjectCheckException("The return type of the method is not generic or its generic type is not explicit");
        if(!Promise.class.equals(((ParameterizedType)output).getRawType())) throw new RemoteObjectCheckException("The return type is not a Promise");

        Type realOutputType = ((ParameterizedType)output).getActualTypeArguments()[0];
        this.outputMetadata = Message.retrieveFromCache(((Class)realOutputType).asSubclass(Serializable.class));

    }

    public Method getMethod() {
        return method;
    }

    public String getMember() {
        return member;
    }

    public MessageMetadata getInputMetadata() {
        return inputMetadata;
    }

    public MessageMetadata getOutputMetadata() {
        return outputMetadata;
    }
}
