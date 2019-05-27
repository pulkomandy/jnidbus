#include "./headers/serialization.h"

using namespace std;

void serialize(context* ctx, jobject serialized, DBusMessageIter* container){
    //get JVM env
    JNIEnv* env;
    get_env(ctx,&env);
    
    //get signature and values from the JVM object
    const char* dbus_object_class_name = "fr/viveris/jnidbus/serialization/DBusObject";
    jclass dbusObjectClass = find_class(ctx,dbus_object_class_name);
    jstring dbusTypesJVM = (jstring) env->GetObjectField(serialized, find_field(ctx,dbus_object_class_name,"signature", "Ljava/lang/String;"));
    jobjectArray dbusValues = (jobjectArray) env->GetObjectField(serialized, find_field(ctx,dbus_object_class_name,"values", "[Ljava/lang/Object;"));
    const char* dbusTypesNative = env->GetStringUTFChars(dbusTypesJVM, 0);
    
    //validate the signature
    if(!dbus_signature_validate(dbusTypesNative,NULL)){
        env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"The given signture is not a valid DBus signature");
    }else if(strlen(dbusTypesNative) > 0){
        //value to serialize and index in the array
        jobject valueJVM;
        int i = 0;

        //signature of the message
        DBusSignatureIter signatureIter;
        dbus_signature_iter_init(&signatureIter,dbusTypesNative);
        //iterate on every signature element
        do{
            //get value from the JVM
            valueJVM = env->GetObjectArrayElement(dbusValues,i++);
            int currentSignature = dbus_signature_iter_get_current_type(&signatureIter);
            switch(currentSignature){
                case DBUS_TYPE_ARRAY:
                {
                    //recurse the signature and container
                    DBusSignatureIter sub_signature;
                    dbus_signature_iter_recurse(&signatureIter,&sub_signature);
                    DBusMessageIter sub_container;
                    char* signature = dbus_signature_iter_get_signature(&sub_signature);
                    dbus_message_iter_open_container(container,DBUS_TYPE_ARRAY,signature,&sub_container);
                    dbus_free(signature);

                    //serialize the value and put it in the container
                    serialize_array(ctx,dbus_signature_iter_get_current_type(&sub_signature),(jobjectArray) valueJVM, &sub_container,&sub_signature);
                    
                    dbus_message_iter_close_container(container,&sub_container);
                    break;
                }
                case DBUS_TYPE_STRUCT:
                {
                    //recurse the container, don't recurse the signature as the serialize function  will create its own
                    //using the JVM object
                    DBusMessageIter sub_container;
                    dbus_message_iter_open_container(container,DBUS_TYPE_STRUCT,NULL,&sub_container);
                    serialize(ctx,valueJVM,&sub_container);
                    dbus_message_iter_close_container(container,&sub_container);
                    break;
                }
                case DBUS_TYPE_INVALID:
                {
                    //do nothing, it means the iterator is finished
                    break;
                }
                default:
                {
                    //primitive element serialization
                    serialize_element(ctx,currentSignature,valueJVM,container);
                    break;
                }
            }
            //remove the local ref, we must to this to avoid too many reference being managed when serializing a huge message
            env->DeleteLocalRef(valueJVM);
        }while(dbus_signature_iter_next(&signatureIter) && !env->ExceptionOccurred());
    }

    env->ReleaseStringUTFChars(dbusTypesJVM, dbusTypesNative);
}

jobject unserialize(context* ctx, DBusMessageIter* container){
    //get JVM env
    JNIEnv* env;
    get_env(ctx,&env);

    //as DBus can not tell us the size of the message we have to use a vector to temporarily store the unserialized objects
    vector<jobject> values;

    const char* dbus_object_class_name = "fr/viveris/jnidbus/serialization/DBusObject";
    jclass dbusObjectClass = find_class(ctx,dbus_object_class_name);


    do{
        switch(dbus_message_iter_get_arg_type(container)){
            case DBUS_TYPE_ARRAY:
            {
                //recurse, unserialize and push to vector
                DBusMessageIter sub_container;
                dbus_message_iter_recurse(container, &sub_container);
                values.push_back((jobject)unserialize_array(ctx,dbus_message_iter_get_element_type(container),&sub_container));
                break;
            }
            case DBUS_TYPE_STRUCT:
            {
                //recurse, unserialize and push to vector
                DBusMessageIter sub_container;
                dbus_message_iter_recurse(container, &sub_container);
                values.push_back((jobject) unserialize(ctx,&sub_container));
                break;
            }
            case DBUS_TYPE_INVALID:
            {
                //do nothing, it means the iterator is finished
                break;
            }
            default:
            {
                //unserialize the primitive type and push to vector
                values.push_back(unserialize_element(ctx,container));
                break;
            }
        }
    }while(dbus_message_iter_next(container));

    //get the JVM object constructor and create the object array that will contain the unserialized values
    jmethodID constructor = find_method(ctx,dbus_object_class_name,"<init>","(Ljava/lang/String;[Ljava/lang/Object;)V");
    jobjectArray objectArray = env->NewObjectArray(values.size(),find_class(ctx,"java/lang/Object"),NULL);
    
    //fill the JVM array
    for(int i = 0; i<values.size();i++){
        env->SetObjectArrayElement(objectArray,i,values.at(i));
    }

    //don't set the signature of the object, this will either be done by the handling function in the case of a root object
    //or it will be done by the JVM side for the child objects
    return env->NewObject(dbusObjectClass,constructor,env->NewStringUTF(""),objectArray);
}

void serialize_array(context* ctx, int dbus_type, jobjectArray array, DBusMessageIter* container, DBusSignatureIter* signature){
    //get JVM env
    JNIEnv* env;
    get_env(ctx,&env);
    int array_length = array_length = env->GetArrayLength(array);

    if(dbus_type == DBUS_TYPE_ARRAY){
        //open a container with the correct signature.
        DBusSignatureIter sub_signature;
        dbus_signature_iter_recurse(signature,&sub_signature);
        char* charSignature = dbus_signature_iter_get_signature(&sub_signature);
        DBusMessageIter sub_container;
        dbus_message_iter_open_container(container,DBUS_TYPE_ARRAY,charSignature,&sub_container);
        dbus_free(charSignature);

        //iterate through the array
        int i = 0;
        jobject valueJVM;
        while(i < array_length){
            //get object to serialize
            valueJVM = env->GetObjectArrayElement(array,i++);
            //as DBus does not give the possibility to reset an iterator, the sub signature will become invalid
            //on the next iteration, to prevent this we must memcopy the orignal each time
            DBusSignatureIter sub_signature_copy;
            memcpy(&sub_signature_copy,&sub_signature,sizeof(DBusSignatureIter));
            serialize_array(ctx,dbus_signature_iter_get_current_type(&sub_signature),(jobjectArray) valueJVM, &sub_container,&sub_signature_copy);
            //advise the JVM we don't need this object anymore
            env->DeleteLocalRef(valueJVM);
        }

        dbus_message_iter_close_container(container,&sub_container);
        
    }else if(dbus_type == DBUS_TYPE_STRUCT){
        //iterate through the array
        int i = 0;
        jobject valueJVM;
        while(i < array_length){
            //open a  container without signature, there is no need to recurse into the current signature as the JVM
            //object will contain its own signature and we will get it from here. We might change this behavior later
            //as its not optimal
            DBusMessageIter sub_container;
            dbus_message_iter_open_container(container,DBUS_TYPE_STRUCT,NULL,&sub_container);
            //get object to serialize
            valueJVM = env->GetObjectArrayElement(array,i++);
            //recursive call
            serialize(ctx,valueJVM,&sub_container);
            dbus_message_iter_close_container(container,&sub_container);
            env->DeleteLocalRef(valueJVM);
        }
    }else{
        //iterate through the array
        int i = 0;
        jobject valueJVM;
        while(i < array_length){
            //get object to serialize
            valueJVM = env->GetObjectArrayElement(array,i++);
            serialize_element(ctx,dbus_type,valueJVM,container);
            env->DeleteLocalRef(valueJVM);
        }
    }
}

jobjectArray unserialize_array(context* ctx, int dbus_type, DBusMessageIter* container){
    //get JVM env
    JNIEnv* env;
    get_env(ctx,&env);

    //as DBus can not tell us the size of the message we have to use a vector to temporarily store the unserialized objects
    vector<jobject> values;

    if(dbus_type == DBUS_TYPE_ARRAY){
        do {
            //recurse, unserialize and push to vector
            DBusMessageIter sub_container;
            dbus_message_iter_recurse(container, &sub_container);
            if(dbus_message_iter_get_arg_type(&sub_container) != DBUS_TYPE_INVALID){
                values.push_back((jobject)unserialize_array(ctx,dbus_message_iter_get_element_type(container),&sub_container));
            }
        }while (dbus_message_iter_next(container));
    }else if(dbus_type == DBUS_TYPE_STRUCT){
        do {
            //recurse, unserialize and push to vector
            DBusMessageIter sub_container;
            dbus_message_iter_recurse(container, &sub_container);
            if(dbus_message_iter_get_arg_type(&sub_container) != DBUS_TYPE_INVALID){
                values.push_back(unserialize(ctx,&sub_container));
            }
        }while (dbus_message_iter_next(container));
    }else{
        do {
            //unserialize and push to vector
            jobject val = unserialize_element(ctx,container);
            if(val != NULL){
                values.push_back(val);
            }
        }while (dbus_message_iter_next(container));
    }

    //create container JVM array
    jobjectArray objectArray = env->NewObjectArray(values.size(),find_class(ctx,"java/lang/Object"),NULL);

    //fill array
    for(int i = 0; i<values.size();i++){
        env->SetObjectArrayElement(objectArray,i,values.at(i));
    }
    return objectArray;
}

void serialize_element(context* ctx, int dbus_type, jobject object, DBusMessageIter* container){
    //get JVM env
    JNIEnv* env;
    get_env(ctx,&env);
    
    switch(dbus_type){
        case DBUS_TYPE_STRING:
        {
            //get native string, add and release
            const char* valueNative = env->GetStringUTFChars((jstring)object, 0);
            dbus_message_iter_append_basic(container, DBUS_TYPE_STRING, &valueNative);
            env->ReleaseStringUTFChars((jstring) object, valueNative);
            break;
        }
        case DBUS_TYPE_INT32:
        {
            jint valueNative = env->CallIntMethod(object,find_method(ctx,"java/lang/Integer","intValue","()I"));
            dbus_message_iter_append_basic(container, DBUS_TYPE_INT32, &valueNative);
            break;
        }
        case DBUS_TYPE_BOOLEAN:
        {
            int valueNative = (int) env->CallBooleanMethod(object,find_method(ctx,"java/lang/Boolean","booleanValue","()Z"));
            dbus_message_iter_append_basic(container,DBUS_TYPE_BOOLEAN,&valueNative);
            break;
        }
        case DBUS_TYPE_BYTE:
        {
            jbyte valueNative = env->CallByteMethod(object,find_method(ctx,"java/lang/Byte","byteValue","()B"));
            dbus_message_iter_append_basic(container,DBUS_TYPE_BYTE,&valueNative);
            break;
        }
        case DBUS_TYPE_INT16:
        {
            jshort valueNative = env->CallShortMethod(object,find_method(ctx,"java/lang/Short","shortValue","()S"));
            dbus_message_iter_append_basic(container,DBUS_TYPE_INT16,&valueNative);
            break;
        }
        case DBUS_TYPE_INT64:
        {
            jlong valueNative = env->CallLongMethod(object,find_method(ctx,"java/lang/Long","longValue","()J"));
            dbus_message_iter_append_basic(container,DBUS_TYPE_INT64,&valueNative);
            break;
        }
        case DBUS_TYPE_DOUBLE:
        {
            jdouble valueNative = env->CallDoubleMethod(object,find_method(ctx,"java/lang/Double","doubleValue","()D"));
            dbus_message_iter_append_basic(container,DBUS_TYPE_DOUBLE,&valueNative);
            break;
        }
        case DBUS_TYPE_INVALID:
        {
            //ignore, we reached end of iterator
            break;
        }
        default:
        {
            std::string error = std::string()+"Unsupported type detected : "+(char)dbus_type;
            env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),error.c_str());
            break;
        }
    }
}

jobject unserialize_element(context* ctx, DBusMessageIter* container){
    //get JVM env
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
            jmethodID constructor = find_method(ctx,"java/lang/Integer","<init>","(I)V");
            return env->NewObject(integerClass,constructor,value);
            break;
        }
        case DBUS_TYPE_BOOLEAN:
        {
            bool value;
            dbus_message_iter_get_basic(container, &value);
            jclass booleanClass = find_class(ctx,"java/lang/Boolean");
            jmethodID constructor = find_method(ctx,"java/lang/Boolean","<init>","(Z)V");
            return env->NewObject(booleanClass,constructor,value);
            break;
        }
        case DBUS_TYPE_BYTE:
        {
            signed char value;
            dbus_message_iter_get_basic(container, &value);
            jclass byteClass = find_class(ctx,"java/lang/Byte");
            jmethodID constructor = find_method(ctx,"java/lang/Byte","<init>","(B)V");
            return env->NewObject(byteClass,constructor,value);
            break;
        }
        case DBUS_TYPE_INT16:
        {
            short value;
            dbus_message_iter_get_basic(container, &value);
            jclass shortClass = find_class(ctx,"java/lang/Short");
            jmethodID constructor = find_method(ctx,"java/lang/Short","<init>","(S)V");
            return env->NewObject(shortClass,constructor,value);
            break;
        }
        case DBUS_TYPE_INT64:
        {
            long value;
            dbus_message_iter_get_basic(container, &value);
            jclass longClass = find_class(ctx,"java/lang/Long");
            jmethodID constructor = find_method(ctx,"java/lang/Long","<init>","(J)V");
            return env->NewObject(longClass,constructor,value);
            break;
        }
        case DBUS_TYPE_DOUBLE:
        {
            double value;
            dbus_message_iter_get_basic(container, &value);
            jclass doubleClass = find_class(ctx,"java/lang/Double");
            jmethodID constructor = find_method(ctx,"java/lang/Double","<init>","(D)V");
            return env->NewObject(doubleClass,constructor,value);
            break;
        }
        case DBUS_TYPE_INVALID:
        {
            //ignore, we reached end of iterator
            return NULL;
        }
        default:
        {
            std::string error = std::string()+"Unsupported type detected : "+(char)dbus_message_iter_get_arg_type(container);
            env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),error.c_str());
            return NULL;
        }
    }
}
