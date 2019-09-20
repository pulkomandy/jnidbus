/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
#include "./headers/context.h"

jclass find_class(context* context, const char* name){
    //try to get from cache
    std::string nameString = std::string(name);
    jclass classJVM = context->class_cache[nameString];

    //if class not found, fetch from the JVM
    if(classJVM == NULL){
        JNIEnv* env;
        get_env(context,&env);
        classJVM = (jclass) env->NewGlobalRef(env->FindClass(name));
        context->class_cache[nameString] = classJVM;
    }

    return classJVM;
}


jclass find_array_class(context* context, const char* name){
    //try to get from cache
    std::string nameString = std::string(name);
    jclass classJVM = context->class_cache["["+nameString];

    //if class not found, fetch from the JVM
    if(classJVM == NULL){
        JNIEnv* env;
        get_env(context,&env);
        classJVM = (jclass) env->NewGlobalRef(env->GetObjectClass(env->NewObjectArray(0,find_class(context,name),NULL)));
        context->class_cache["["+nameString] = classJVM;
    }

    return classJVM;
}

jmethodID find_method(context* context, const char* class_name, const char* name, const char* signature){
    //generate method key
    std::string nameString = std::string()+class_name+"_"+name+"_"+signature;

    //try to get from cache
    jmethodID method_id = context->method_cache[nameString];

    //if class not found, fetch from the JVM
    if(method_id == NULL){
        JNIEnv* env;
        get_env(context,&env);
        method_id = env->GetMethodID(find_class(context,class_name),name,signature);
        context->method_cache[nameString] = method_id;
    }

    return method_id;
}

jfieldID find_field(context* context, const char* class_name, const char* name, const char* signature){
    //generate field key
    std::string nameString = std::string()+class_name+"_"+name+"_"+signature;

    //try to get from cache
    jfieldID method_id = context->field_cache[nameString];

    //if class not found, fetch from the JVM
    if(method_id == NULL){
        JNIEnv* env;
        get_env(context,&env);
        method_id = env->GetFieldID(find_class(context,class_name),name,signature);
        context->field_cache[nameString] = method_id;
    }

    return method_id;
}

void get_env(context* context, JNIEnv** env){
    // it is safe to call attachThread multiple times as it will do nothing when the thread
    //is already attached beside giving us the JNIEnv
    context->vm->AttachCurrentThread((void **) env,NULL);
}

void close_context(context* ctx){
    JNIEnv* env;
    get_env(ctx,&env);

    //delete global ref of any cached class ref
	std::map<std::string,jclass>::iterator it = ctx->class_cache.begin();
	while (it != ctx->class_cache.end())
	{
        env->DeleteGlobalRef((jobject)it->second);
		it++;
	}
    
    close(ctx->epollFD);
    close(ctx->wakeupFD);
    free(ctx->epollStruct);
    free(ctx->bus_name);
    env->DeleteGlobalRef(ctx->eventLoop);
    env->DeleteGlobalRef(ctx->wakeup_lock);
}