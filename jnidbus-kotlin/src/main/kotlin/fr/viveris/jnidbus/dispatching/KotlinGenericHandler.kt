package fr.viveris.jnidbus.dispatching

import fr.viveris.jnidbus.message.Promise
import fr.viveris.jnidbus.serialization.DBusType
import fr.viveris.jnidbus.serialization.Serializable
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.javaMethod

/**
 * Class overriding the default GenericHandler method analysis to handle suspending methods.
 */
abstract class KotlinGenericHandler : GenericHandler() {
    /**
     * Analyze all the extending class method to find and analyze @HandlerMethod methods. Suspending methods are supported
     */
    @Suppress("UNCHECKED_CAST")
    override fun getAvailableCriterias(): HashMap<Criteria, HandlerMethod> {
        val methods = this::class.declaredMemberFunctions
        val returned = HashMap<Criteria, HandlerMethod>()

        for (m in methods) {
            //check if the method is a handler method, if not keep looking
            val annotation = m.findAnnotation<fr.viveris.jnidbus.dispatching.annotation.HandlerMethod>() ?: continue

            //check method input and output
            val params = m.parameters
            var returnType = m.returnType
            if (params.size != 2 && !params[1].type.isSubtypeOf(Serializable::class.createType())) {
                throw IllegalArgumentException("Incorrect number of parameter or the parameter is not Serializable")
            }
            val param = params[1].type.classifier as KClass<Serializable>

            //if the return type is a promise, check its generic type
            val serializableReturnType : KClass<Serializable>
            if (returnType.classifier != null && returnType.classifier!!.equals(Promise::class)) {
                serializableReturnType = returnType.arguments[0].type!!.classifier as KClass<Serializable>
            }else if (returnType.isSubtypeOf(Serializable::class.createType())){
                serializableReturnType = returnType.classifier as KClass<Serializable>
            }else{
                throw IllegalArgumentException("A handler method return type must be Serializable")
            }

            val paramAnnotation = param.findAnnotation<DBusType>()
            val returnAnnotation = serializableReturnType.findAnnotation<DBusType>()

            var ouputSignature = ""

            if (paramAnnotation == null) {
                throw IllegalArgumentException("A handler method parameter must have the DBusType annotation")
            }

            if (annotation.type == MemberType.METHOD && returnAnnotation == null) {
                throw IllegalArgumentException("A handler method return type must have the DBusType annotation")
            } else if (returnAnnotation != null) {
                ouputSignature = returnAnnotation.signature
            }

            val hm = HandlerMethod(this, m.javaMethod, serializableReturnType.javaObjectType)

            //the types are valid, put it in the map. This line will throw if the serializable types are in fact invalid
            returned[Criteria(annotation.member, paramAnnotation.signature, ouputSignature, annotation.type)] = hm

        }
        return returned
    }
}