#include "./headers/fr_viveris_jnidbus_bindings_bus_EventLoop.h"
#include "./headers/event_loop_handlers.h"

#include <iostream>
#include <string>
#include <cstring>
#include <errno.h>

#include <sys/eventfd.h>

DBusObjectPathVTable dbus_handler_struct = {
    .unregister_function = handle_dispatch_unregister,
    .message_function = handle_dispatch
};

/**
 * This method setup epoll, the wakeup FD and register watch functions to DBus. Watch functions are an interface
 * that allows the developer to build an event loop with any technology it wants (select, epoll, kqueue, etc...).
 *
 * As we don't register any timeout functions, DBus will not be able to detect and manage timeout, this should be done
 * on the JVM side (we might change this at some point but it is not worth the effort right now).
 *
 * On the JVM side, any call made to the event loop while this function was not executed will block and wait.
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    setup
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_setup
  (JNIEnv * env, jobject target, jlong ctxPtr){
    //get contxet
    context* ctx = (context*) ctxPtr;

    //create epoll instance
    ctx->epollFD = epoll_create1(0);
    if(ctx->epollFD == -1){
      env->ThrowNew(find_class(ctx,"fr/viveris/jnidbus/exception/EventLoopSetupException"),"Could not create the epoll FD");
    }

    //create wakeup file descriptor
    ctx->wakeupFD = eventfd(0,EFD_NONBLOCK);
    if(ctx->wakeupFD == -1){
      env->ThrowNew(find_class(ctx,"fr/viveris/jnidbus/exception/EventLoopSetupException"),"Could not create the wakeup FD");
    }

    //create epoll struct in which it will put the selected file descriptors
    epoll_event wakeupStruct;
    ctx->epollStruct = (epoll_event*) calloc (EPOLL_MAX_EVENTS, sizeof(epoll_event));

    //add the wakeup fd to epoll, we only care about data to read
    wakeupStruct.data.fd = ctx->wakeupFD;
    wakeupStruct.events = EPOLLIN;
    int error = epoll_ctl(ctx->epollFD,EPOLL_CTL_ADD,ctx->wakeupFD,&wakeupStruct);
    if(error == -1){
        env->ThrowNew(find_class(ctx,"fr/viveris/jnidbus/exception/EventLoopSetupException"),"Could not register the wakeup FD to epoll");
    }

    //put event loop in context
    ctx->eventLoop = env->NewGlobalRef(target);

    //put lock object in context
    jobject wakekup_lock = env->GetObjectField(ctx->eventLoop,env->GetFieldID(find_class(ctx, "fr/viveris/jnidbus/bindings/bus/EventLoop"),"wakeupLock","Ljava/lang/Object;"));
    ctx->wakeup_lock = env->NewGlobalRef(wakekup_lock);

    //set field ID of the should wakeup flag
    ctx->should_wakeup_flag = env->GetFieldID(find_class(ctx, "fr/viveris/jnidbus/bindings/bus/EventLoop"),"shouldWakeup","Z");

    //add watch handlers
    dbus_connection_set_watch_functions(ctx->connection,add_watch,remove_watch,toggle_watch,ctx,NULL);

    return JNI_TRUE;
  }

/**
 *
 * Core method of the event loop. A tick will call epoll_wait and wait for events. It is possible to
 * wake epoll by using the wakeup file descriptor.
 *
 * When a wakeup call is made, we will simply empty the FD and proceed. If any other FD are selected,
 * get the DBus watch pointer corresponding and notify DBus a watch state has changed, which will
 * make DBus check for data and dispatch parsed message to the correct object path handler.
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    tick
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_tick
  (JNIEnv * env, jobject target,jlong ctxPtr, jint timeout){
    //get context
    context* ctx = (context*) ctxPtr;
    epoll_event* events = ctx->epollStruct;

    //wait for event
    int numberSelected = epoll_wait(ctx->epollFD,events,EPOLL_MAX_EVENTS,(int)timeout);

    //set the wakeup flag to false
    env->MonitorEnter(ctx->wakeup_lock);
    env->SetBooleanField(target,ctx->should_wakeup_flag,JNI_FALSE);
    env->MonitorExit(ctx->wakeup_lock);

    //iterate through what epoll selected
    for (int i = 0; i < numberSelected; i++){
      if(events[i].data.fd == ctx->wakeupFD){
        //wakeup call detected, empty the event FD and proceed
        uint64_t u;
        read(events[i].data.fd,&u,sizeof(uint64_t));
        continue;
      }else{
        //a change have been detected on a FD, dispatch to dbus
        unsigned int flags = 0;
        unsigned int epollFlages = events[i].events;
        DBusWatch* watch = (DBusWatch*) events[i].data.ptr;
        int fd = dbus_watch_get_unix_fd(watch);

        if (epollFlages & EPOLLIN)  flags |= DBUS_WATCH_READABLE;
        if (epollFlages & EPOLLOUT) flags |= DBUS_WATCH_WRITABLE;
        if (epollFlages & EPOLLHUP) flags |= DBUS_WATCH_HANGUP;
        if (epollFlages & EPOLLERR) flags |= DBUS_WATCH_ERROR;

        if (!dbus_watch_handle(watch, flags)) {
          env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"More memory is needed but none is available");
        }

        //dispatch all the detected events
        while (dbus_connection_dispatch(ctx->connection) == DBUS_DISPATCH_DATA_REMAINS);
      }
    }
  }

/**
 *
 * Write arbitrary data in the wakeup file descriptor, which will unblock the current (or next) epoll_wait call
 * As we use an eventfd we don't need to care about whether data is already in the FD or not, an eventFD will add
 * the written values to the one already stored. Fore more info, refer to the man page of "eventfd"
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    wakeup
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_wakeup
  (JNIEnv * env, jobject target, jlong ctxPtr){
    context* ctx = (context*) ctxPtr;
    uint64_t u = 1;
    size_t written = write(ctx->wakeupFD,&u,sizeof(uint64_t));
  }

/**
 *
 * Send the result of a call back to the caller
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    sendReply
 * Signature: (JLfr/viveris/jnidbus/message/Message;J)V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_sendReply
  (JNIEnv * env, jobject target, jlong ctxPtr, jobject messageJVM, jlong msgPointer){
    context* ctx = (context*) ctxPtr;
    DBusConnection* conn = ctx->connection;
    DBusMessage* callMsg = (DBusMessage*) msgPointer;
    DBusMessage* msg = dbus_message_new_method_return(callMsg);
    dbus_message_unref(callMsg);

    DBusMessageIter args;
    dbus_message_iter_init_append(msg,&args);
    serialize(ctx,messageJVM,&args);
    if(env->ExceptionOccurred()){
      //do nothing, free resources and let the JVM throw the exception in the java code
    }else{
      dbus_uint32_t msgSerial = 0;
      if (!dbus_connection_send(conn, msg, &msgSerial)) {
        env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"Sending failed (probably caused by an out of memory)");
      }
    }

    dbus_message_unref(msg);
  }

/**
 * Send an error to the caller, the error name and message are generated from the thrown exception in the java code
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    sendReplyError
 * Signature: (JJLjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_sendReplyError
  (JNIEnv * env, jobject target, jlong ctxPtr, jlong msgPointer, jstring errorCodeJVM, jstring errorMessageJVM){
    context* ctx = (context*) ctxPtr;
    DBusConnection* conn = ctx->connection;
    DBusMessage* callMsg = (DBusMessage*) msgPointer;

    const char* errorCodeNative = env->GetStringUTFChars(errorCodeJVM, 0);
    const char* messageNative = env->GetStringUTFChars(errorMessageJVM, 0);

    DBusMessage* msg = dbus_message_new_error(callMsg,errorCodeNative,messageNative);

    dbus_uint32_t msgSerial = 0;
    if (!dbus_connection_send(conn, msg, &msgSerial)) {
      env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"Sending failed (probably caused by an out of memory)");
    }

    env->ReleaseStringUTFChars(errorCodeJVM, errorCodeNative);
    env->ReleaseStringUTFChars(errorMessageJVM, messageNative);

}

/**
 *
 * Send a Signal on the Bus
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    sendSignal
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lfr/viveris/jnidbus/message/Message;)V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_sendSignal
  (JNIEnv * env, jobject target, jlong ctxPtr, jstring pathJVM, jstring interfaceJVM, jstring memberJVM, jobject messageJVM){

    context* ctx = (context*) ctxPtr;
    DBusConnection* conn = ctx->connection;

    //transform them into native types
    const char* pathNative = env->GetStringUTFChars(pathJVM, 0);
    const char* typeNative = env->GetStringUTFChars(interfaceJVM, 0);
    const char* nameNative = env->GetStringUTFChars(memberJVM, 0);

    //build message
    DBusMessage* msg = dbus_message_new_signal(pathNative,typeNative,nameNative);
    DBusMessageIter args;
    dbus_message_iter_init_append(msg,&args);
    serialize(ctx,messageJVM,&args);
    if(env->ExceptionOccurred()){
      //do nothing
    }else{
      dbus_uint32_t msgSerial = 0;
      if (!dbus_connection_send(conn, msg, &msgSerial)) {
        env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"Sending failed (probably caused by an out of memory)");
      }
    }

    env->ReleaseStringUTFChars(pathJVM, pathNative);
    env->ReleaseStringUTFChars(interfaceJVM, typeNative);
    env->ReleaseStringUTFChars(memberJVM, nameNative);
    dbus_message_unref(msg);
}

/**
 *
 * Asynchronously call a DBus method. The function will register the given JVM Promise to DBus which will notify
 * it when its state changes
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_Connection
 * Method:    sendEvent
 * Signature: (Lfr/viveris/jnidbus/bindings/message/Event;)Z
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_sendCall
  (JNIEnv * env, jobject target, jlong ctxPtr, jstring pathJVM, jstring interfaceJVM, jstring memberJVM, jobject messageJVM, jstring destJVM, jobject promise){
    context* ctx = (context*) ctxPtr;

    //get the dbus connection from pointer
    DBusConnection* conn = ctx->connection;

    //transform params into native types
    const char* pathNative = env->GetStringUTFChars(pathJVM, 0);
    const char* typeNative = env->GetStringUTFChars(interfaceJVM, 0);
    const char* nameNative = env->GetStringUTFChars(memberJVM, 0);
    const char* destNative = env->GetStringUTFChars(destJVM, 0);

    //build message
    DBusMessage* msg = dbus_message_new_method_call(destNative,pathNative,typeNative,nameNative);
    DBusMessageIter args;
    dbus_message_iter_init_append(msg,&args);
    serialize(ctx,messageJVM,&args);
    if(env->ExceptionOccurred()){
      //do nothing
    }else{
      DBusPendingCall* res;
      if (!dbus_connection_send_with_reply(conn,msg,&res,-1)) {
        env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"Sending failed (probably caused by an out of memory)");
      }else{
        //give the JVM Promise to DBus for later notification
        pending_call_context* callContext = (pending_call_context*) malloc(sizeof(pending_call_context));
        callContext->promise = env->NewGlobalRef(promise);
        callContext->ctx = ctx;
        dbus_pending_call_set_notify(res,handle_call_response,callContext,NULL);
      }
    }

    //free resources
    env->ReleaseStringUTFChars(pathJVM, pathNative);
    env->ReleaseStringUTFChars(interfaceJVM, typeNative);
    env->ReleaseStringUTFChars(memberJVM, nameNative);
    env->ReleaseStringUTFChars(destJVM, destNative);

    dbus_message_unref(msg);
}

/**
 *
 * Register a new Dispatcher to DBus and add a match to receive signal for this object path.
 * Please note that with the current implementation, a dispatcher can be notified for signals that
 * do not have handlers on the JVM side, this might change in the future but it's not worth the
 * effort right now.
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    addPathHandler
 * Signature: (JLjava/lang/String;Lfr/viveris/jnidbus/dispatching/Dispatcher;)V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_addPathHandler
  (JNIEnv * env, jobject target, jlong ctxPtr, jstring pathJVM, jobject dispatcher){
  context* ctx = (context*) ctxPtr;

  const char* pathNative = env->GetStringUTFChars(pathJVM, 0);

  handler_context* handler_data = (handler_context*) malloc(sizeof(handler_context));
  handler_data->ctx = ctx;
  handler_data->dispatcher = env->NewGlobalRef(dispatcher);
  dbus_connection_register_object_path(ctx->connection,pathNative,&dbus_handler_struct,handler_data);
  std::string match = std::string()+"path='"+pathNative+"'";
  dbus_bus_add_match(ctx->connection,match.c_str(),NULL);

  env->ReleaseStringUTFChars(pathJVM,pathNative);

}

/**
 *
 *
 *
 * Class:     fr_viveris_jnidbus_bindings_bus_EventLoop
 * Method:    addPathHandler
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_fr_viveris_jnidbus_bindings_bus_EventLoop_removePathHandler
  (JNIEnv * env, jobject target, jlong ctxPtr, jstring pathJVM){
  context* ctx = (context*) ctxPtr;

  const char* pathNative = env->GetStringUTFChars(pathJVM, 0);

  //unregister match
  std::string match = std::string()+"path='"+pathNative+"'";
  dbus_bus_remove_match(ctx->connection,match.c_str(),NULL);

  //unregister obj path
  dbus_connection_unregister_object_path(ctx->connection,pathNative);


  env->ReleaseStringUTFChars(pathJVM,pathNative);
}