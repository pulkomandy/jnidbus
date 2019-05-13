#include "./headers/serialization.h"

using namespace std;

void serialize(context* ctx, jobject message, DBusMessageIter* container){
    JNIEnv* env;
    get_env(ctx,&env);

    //get the serialized object and populate the message with it
    jmethodID id = env->GetMethodID(find_class(ctx,"fr/viveris/vizada/jnidbus/message/Message"), "serialize", "()Lfr/viveris/vizada/jnidbus/serialization/DBusObject;");
    jobject serialized = env->CallObjectMethod(message, id);
    jthrowable excOccured = env->ExceptionOccurred();

    //if the serialization went well
    if(!excOccured && serialized != NULL){
        jclass dbusObjectClass = find_class(ctx,"fr/viveris/vizada/jnidbus/serialization/DBusObject");
        
        //get signature and values, then transfer them to DBus
        jstring dbusTypesJVM = (jstring) env->CallObjectMethod(serialized, env->GetMethodID(dbusObjectClass, "getSignature", "()Ljava/lang/String;"));
        jobjectArray dbusValues = (jobjectArray) env->CallObjectMethod(serialized, env->GetMethodID(dbusObjectClass, "getValues", "()[Ljava/lang/Object;"));
        const char* dbusTypesNative = env->GetStringUTFChars(dbusTypesJVM, 0);
        
        if(!dbus_signature_validate(dbusTypesNative,NULL)){
            env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"The given signture is not a valid DBus signature");
        }else if(strlen(dbusTypesNative) > 0){
            //cut the signature in single char and transform the JVM types in primitive types
            jobject valueJVM;
            int i = 0;
            DBusSignatureIter signatureIter;
            dbus_signature_iter_init(&signatureIter,dbusTypesNative);
            do{
                valueJVM = env->GetObjectArrayElement(dbusValues,i++);
                int currentSignature = dbus_signature_iter_get_current_type(&signatureIter);
                //if the value is a string, transform to primitive char*, put it in the message and free it
                //there is no risk of use-after-free as DBus copy the string in an internal buffer.
                switch(currentSignature){
                    case DBUS_TYPE_ARRAY:
                    {
                        DBusSignatureIter sub_signature;
                        dbus_signature_iter_recurse(&signatureIter,&sub_signature);
                        DBusMessageIter sub_container;
                        char* signature = dbus_signature_iter_get_signature(&sub_signature);
                        dbus_message_iter_open_container(container,DBUS_TYPE_ARRAY,signature,&sub_container);
                        dbus_free(signature);
                        serialize_array(ctx,dbus_signature_iter_get_current_type(&sub_signature),(jobjectArray) valueJVM, &sub_container,&sub_signature);
                        dbus_message_iter_close_container(container,&sub_container);
                        break;
                    }
                    case DBUS_TYPE_STRUCT:
                    {
                        //TODO
                        break;
                    }
                    case DBUS_TYPE_INVALID:
                    {
                        //do nothing, it means the iterator is finished
                        break;
                    }
                    default:
                    {
                        serialize_element(ctx,currentSignature,valueJVM,container);
                        break;
                    }
                }
            }while(dbus_signature_iter_next(&signatureIter) && !env->ExceptionOccurred());
        }
        env->ReleaseStringUTFChars(dbusTypesJVM, dbusTypesNative);
    }
}

jobject unserialize(context* ctx, DBusMessageIter* container){
    JNIEnv* env;
    get_env(ctx,&env);

    vector<jobject> values;
    char* signature = dbus_message_iter_get_signature(container);
    jclass dbusObjectClass = find_class(ctx,"fr/viveris/vizada/jnidbus/serialization/DBusObject");

    do{
        switch(dbus_message_iter_get_arg_type(container)){
            case DBUS_TYPE_ARRAY:
            {
                DBusMessageIter sub_container;
                dbus_message_iter_recurse(container, &sub_container);
                values.push_back((jobject)unserialize_array(ctx,dbus_message_iter_get_element_type(container),&sub_container));
                break;
            }
            case DBUS_TYPE_STRUCT:
            {
                //TODO
                break;
            }
            case DBUS_TYPE_INVALID:
            {
                //do nothing, it means the iterator is finished
                break;
            }
            default:
            {
                values.push_back(unserialize_element(ctx,container));
                break;
            }
        }
    }while(dbus_message_iter_next(container));

    jmethodID constructor = env->GetMethodID(dbusObjectClass, "<init>", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    jobjectArray objectArray = env->NewObjectArray(values.size(),env->FindClass("java/lang/Object"),NULL);
    
    //fill array
    for(int i = 0; i<values.size();i++){
        env->SetObjectArrayElement(objectArray,i,values.at(i));
    }

    jobject unserialized = env->NewObject(dbusObjectClass,constructor,env->NewStringUTF(signature),objectArray);

    dbus_free(signature);
    return unserialized;
}

void serialize_array(context* ctx, int dbus_type, jobjectArray array, DBusMessageIter* container, DBusSignatureIter* signature){
    JNIEnv* env;
    get_env(ctx,&env);
    int array_length = 0;
    if(array != NULL){
        array_length = env->GetArrayLength(array);
    }

    if(dbus_type == DBUS_TYPE_ARRAY){
        //open a container with the correct signature
        DBusSignatureIter sub_signature;
        dbus_signature_iter_recurse(signature,&sub_signature);
        char* charSignature = dbus_signature_iter_get_signature(&sub_signature);
        DBusMessageIter sub_container;
        dbus_message_iter_open_container(container,DBUS_TYPE_ARRAY,charSignature,&sub_container);
        dbus_free(charSignature);

        //empty or null array, open the container anyway to have to correct signature
        if(array == NULL || array_length == 0){
            serialize_array(ctx,dbus_signature_iter_get_current_type(&sub_signature),NULL, &sub_container,&sub_signature);
        }else{
            int i = 0;
            jobject valueJVM;
            while(i < array_length){
                valueJVM = env->GetObjectArrayElement(array,i++);
                serialize_array(ctx,dbus_signature_iter_get_current_type(&sub_signature),(jobjectArray) valueJVM, &sub_container,&sub_signature);
            }
        }

        dbus_message_iter_close_container(container,&sub_container);
        
    }else if(dbus_type == DBUS_TYPE_STRUCT){
        //TODO
    }else{
        if(array_length > 0){
            int i = 0;
            jobject valueJVM;

            while(i < array_length){
                valueJVM = env->GetObjectArrayElement(array,i++);
                serialize_element(ctx,dbus_type,valueJVM,container);
            }
        }
    }
}

jobjectArray unserialize_array(context* ctx, int dbus_type, DBusMessageIter* container){
    JNIEnv* env;
    get_env(ctx,&env);
    vector<jobject> values;

    if(dbus_type == DBUS_TYPE_ARRAY){
        do {
            DBusMessageIter sub_container;
            dbus_message_iter_recurse(container, &sub_container);
            if(dbus_message_iter_get_arg_type(&sub_container) != DBUS_TYPE_INVALID){
                values.push_back((jobject)unserialize_array(ctx,dbus_message_iter_get_element_type(container),&sub_container));
            }
        }while (dbus_message_iter_next(container));
    }else if(dbus_type == DBUS_TYPE_STRUCT){
        //TODO
    }else{
        do {
            jobject val = unserialize_element(ctx,container);
            if(val != NULL){
                values.push_back(val);
            }
        }while (dbus_message_iter_next(container));
    }

    jobjectArray objectArray = env->NewObjectArray(values.size(),env->FindClass("java/lang/Object"),NULL);
    //fill array
    for(int i = 0; i<values.size();i++){
        env->SetObjectArrayElement(objectArray,i,values.at(i));
    }
    return objectArray;
}

/**
 * Transfer the JVM object into the container
 */
void serialize_element(context* ctx, int dbus_type, jobject object, DBusMessageIter* container){
    JNIEnv* env;
    get_env(ctx,&env);
    
    switch(dbus_type){
        case DBUS_TYPE_STRING:
        {
            const char* valueNative = env->GetStringUTFChars((jstring)object, 0);
            dbus_message_iter_append_basic(container, DBUS_TYPE_STRING, &valueNative);
            env->ReleaseStringUTFChars((jstring) object, valueNative);
            break;
        }
        case DBUS_TYPE_INT32:
        {
            jclass intClass = env->FindClass("java/lang/Integer");
            jint valueNative = env->CallIntMethod(object,env->GetMethodID(intClass, "intValue", "()I"));
            dbus_message_iter_append_basic(container, DBUS_TYPE_INT32, &valueNative);
            break;
        }
        case DBUS_TYPE_INVALID:
        {
            //ignore, we reached end of iterator
            break;
        }
        default:
        {
            std::string error = "Unsupported type detected : ";
            error += (char)dbus_type;
            env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),error.c_str());
            break;
        }
    }
}

/**
 * Transfer the serialized element into a JVM object
 */
jobject unserialize_element(context* ctx, DBusMessageIter* container){
    JNIEnv* env;
    get_env(ctx,&env);

    switch(dbus_message_iter_get_arg_type(container)){
        case DBUS_TYPE_STRING:
        {
            char* value;
            dbus_message_iter_get_basic(container, &value);
            return env->NewStringUTF(value);
            break;
        }
        case DBUS_TYPE_INT32:
        {
            int value;
            dbus_message_iter_get_basic(container, &value);
            jclass integerClass = find_class(ctx,"java/lang/Integer");
            jmethodID constructor = env->GetMethodID(integerClass, "<init>", "(I)V");
            return env->NewObject(integerClass,constructor,value);
        }
        case DBUS_TYPE_INVALID:
        {
            //ignore, we reached end of iterator
            return NULL;
        }
        default:
        {
            std::string error = "Unsupported type detected : ";
            error += (char)dbus_message_iter_get_arg_type(container);
            env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),error.c_str());
            return NULL;
        }
    }
}
