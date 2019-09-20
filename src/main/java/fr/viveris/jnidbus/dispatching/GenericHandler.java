/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
package fr.viveris.jnidbus.dispatching;

import fr.viveris.jnidbus.dispatching.annotation.Handler;
import fr.viveris.jnidbus.message.Promise;
import fr.viveris.jnidbus.serialization.DBusType;
import fr.viveris.jnidbus.serialization.Serializable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;

/**
 * Abstract class any handler class should extend, it only define one method which will be able to generate all the criteria and
 * handlerMethods from the declared methods.
 */
public abstract class GenericHandler {

    /**
     * Create a map of all the criteria and handler methods this handler provides. When generating the HandlerMethod class,
     * the serializable types will be checked and cached so any attempt to register a handler with invalid types will throw
     *
     * @return map of the criteria with their corresponding handler methods
     */
    public HashMap<Criteria,HandlerMethod> getAvailableCriterias(){
        Method[] methods = this.getClass().getMethods();
        HashMap<Criteria,HandlerMethod> returned = new HashMap<>();

        for(Method m : methods){
            //check if the method is a handler method, if not keep looking
            fr.viveris.jnidbus.dispatching.annotation.HandlerMethod annotation = this.getHandlerMethodAnnotation(m);
            if(annotation == null) continue;

            //check method input and output
            Class<?>[] params = m.getParameterTypes();
            Class<?> returnType = m.getReturnType();
            if(params.length != 1) throw new IllegalArgumentException("Incorrect number of parameter on an handler method, there should be only one input parameter");

            //if the return type is a promise, check its generic type
            if(Promise.class.isAssignableFrom(returnType)){
                returnType = (Class) ((ParameterizedType)m.getGenericReturnType()).getActualTypeArguments()[0];
            }

            DBusType paramAnnotation = params[0].getAnnotation(DBusType.class);
            DBusType returnAnnotation = returnType.getAnnotation(DBusType.class);

            String ouputSignature = "";

            if(paramAnnotation == null || !Serializable.class.isAssignableFrom(params[0])){
                throw new IllegalArgumentException("A handler method parameter must have the DBusType annotation and be Serializable");
            }

            if(annotation.type() == MemberType.METHOD && (returnAnnotation == null || !Serializable.class.isAssignableFrom(returnType))){
                throw new IllegalArgumentException("A handler method return type must have the DBusType annotation and be Serializable");
            }else if(returnAnnotation != null){
                ouputSignature = returnAnnotation.signature();
            }

            HandlerMethod hm;
            if(Void.TYPE.equals(returnType)){
                hm = new HandlerMethod(this,m,null);
            }else{
                hm = new HandlerMethod(this,m,returnType.asSubclass(Serializable.class));
            }
            //the types are valid, put it in the map. This line will throw if the serializable types are in fact invalid
            returned.put(new Criteria(annotation.member(),paramAnnotation.signature(),ouputSignature,annotation.type()),hm);

        }
        return returned;
    }

    /**
     * Returns the Handler annotation for this handler, this method can be overridden for test purposes or to easily
     * register the same handler to multiple paths/interfaces
     *
     * @return the Handler annotation
     */
    public Handler getHandlerAnnotation(){
        return this.getClass().getAnnotation(Handler.class);
    }

    /**
     * Returns the HandlerMethod annotation for the given method handler, this method can be overridden for test purposes
     *
     * @return the Handler annotation
     */
    public fr.viveris.jnidbus.dispatching.annotation.HandlerMethod getHandlerMethodAnnotation(Method method){
        return method.getAnnotation(fr.viveris.jnidbus.dispatching.annotation.HandlerMethod.class);
    }
}
