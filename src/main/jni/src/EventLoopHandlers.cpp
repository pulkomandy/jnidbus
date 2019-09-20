/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
#include "./headers/event_loop_handlers.h"


DBusHandlerResult handle_dispatch(DBusConnection* connection, DBusMessage* msg, void* ctxPtr){
  dbus_message_ref(msg);

  //get the JVM environment
  handler_context* handlerCtx = (handler_context*) ctxPtr;
  context* ctx = (context*) handlerCtx->ctx;
  JNIEnv* env;
  get_env(ctx,&env);
  
  DBusMessageIter rootIter;
  dbus_message_iter_init(msg, &rootIter);
  jobject jvmObject = deserialize(ctx,&rootIter);
  //set the object signature
  env->SetObjectField(
      jvmObject,
      find_field(ctx,"fr/viveris/jnidbus/serialization/DBusObject","signature","Ljava/lang/String;"),
      env->NewStringUTF(dbus_message_get_signature(msg)));

  //message metadata
  const char * interface = dbus_message_get_interface(msg);
  const char * member = dbus_message_get_member(msg);

  //if the message is a call, prepare the response
  uintptr_t msgPointer = 0;
  if(dbus_message_get_type(msg) == DBUS_MESSAGE_TYPE_METHOD_CALL){
    msgPointer = (uintptr_t) msg;
  }

  //Call the JVM handler method, the method will return false if the message was a called and that no handler were found, true instead
  jboolean wasHandled = env->CallBooleanMethod(
    handlerCtx->dispatcher,
    find_method(
      ctx,
      "fr/viveris/jnidbus/dispatching/Dispatcher", 
      "dispatch", 
      "(Lfr/viveris/jnidbus/serialization/DBusObject;Ljava/lang/String;Ljava/lang/String;J)Z"
    ),
    jvmObject,
    env->NewStringUTF(interface),
    env->NewStringUTF(member),
    msgPointer
  );

  //if the message was a signal or a call not dispatched, free it
  if(msgPointer == 0 || wasHandled == JNI_FALSE){
    dbus_message_unref(msg);
  }

  if(wasHandled == JNI_FALSE){
    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
  }else{
    return DBUS_HANDLER_RESULT_HANDLED;
  }
}

void handle_dispatch_unregister(DBusConnection* connection, void* ctxPtr){
  handler_context* handlerCtx = (handler_context*) ctxPtr;
  context* ctx = (context*) handlerCtx->ctx;

  JNIEnv* env;
  get_env(ctx,&env);

  env->DeleteGlobalRef(handlerCtx->dispatcher);
  free(handlerCtx);
}

void handle_call_response(DBusPendingCall* pending, void* ctxPtr){
  //get JVM environment
  pending_call_context* pCtx = (pending_call_context*) ctxPtr;
  context* ctx = pCtx->ctx;
  JNIEnv* env;
  get_env(ctx,&env);

  //get the reply from the pending call
  DBusMessage* msg = dbus_pending_call_steal_reply(pending);

  if(msg == NULL){
    env->ThrowNew(find_class(ctx,"java/lang/IllegalStateException"),"Pending call notified but message was null");
  }

  //if the message is an error, call the fail() method of the object
  if(dbus_message_get_type(msg) == DBUS_MESSAGE_TYPE_ERROR){
    DBusError err;
    dbus_error_init(&err);
    dbus_set_error_from_message(&err,msg);
    env->CallVoidMethod(
      pCtx->pending_call,
      find_method(
        ctx,
        "fr/viveris/jnidbus/message/PendingCall", 
        "fail", 
        "(Ljava/lang/String;Ljava/lang/String;)V"
      ),
      env->NewStringUTF(err.name),
      env->NewStringUTF(err.message)
    );
    dbus_error_free(&err);
  //else, deserialize and call the notify() method
  }else{
    DBusMessageIter rootIter;
    dbus_message_iter_init(msg, &rootIter);
    jobject jvmObject = deserialize(ctx,&rootIter);
    env->SetObjectField(
      jvmObject,
      find_field(ctx,"fr/viveris/jnidbus/serialization/DBusObject","signature","Ljava/lang/String;"),
      env->NewStringUTF(dbus_message_get_signature(msg)));

    env->CallVoidMethod(
      pCtx->pending_call,
      find_method(
        ctx,
        "fr/viveris/jnidbus/message/PendingCall", 
        "notify", 
        "(Lfr/viveris/jnidbus/serialization/DBusObject;)V"
      ),
      jvmObject
    );
  }

  //free resources
  dbus_message_unref(msg);
  dbus_pending_call_unref(pending);
  env->DeleteGlobalRef(pCtx->pending_call);
  free(pCtx);
}

/**
 * Called by DBus when it wants to add a file descriptor to the event loop. DBus might also call this method when
 * it wants to update the event flag.
 */ 
dbus_bool_t add_watch(DBusWatch *w, void *data){
    //get the JVM environment
    context* ctx = (context*) data;

    // we always want to know if the socket was closed
    short cond = EPOLLHUP | EPOLLERR;
    int fd = dbus_watch_get_unix_fd(w);
    unsigned int flags = dbus_watch_get_flags(w);

    if(flags & DBUS_WATCH_READABLE) cond |= EPOLLIN;
    if(flags & DBUS_WATCH_WRITABLE) cond |= EPOLLOUT;

    //register to epoll and store a pointer to the watch in the epoll struct. Beware, event.data is a union struct
    //so only one field can be used at a time, meaning we are storing only the watch pointer (which contains the
    //file descriptor, so we are fine)
    epoll_event event;
    event.events = cond;
    event.data.ptr = w;

    int error = epoll_ctl(ctx->epollFD,EPOLL_CTL_ADD,fd,&event);
    if(error == -1){
        //if the error is that we already have a FD registered, try to update instead
        if(errno == EEXIST){
            error = epoll_ctl(ctx->epollFD,EPOLL_CTL_MOD,fd,&event);
            return 1;
        }else{
          return -1;
        }
    }

    return 1;
}

/**
 * Called when DBus closed a file descriptor and want to unregister it from the event loop
 */
void remove_watch(DBusWatch *w, void *data){
  context* ctx = (context*) data;

  epoll_ctl(ctx->epollFD,EPOLL_CTL_DEL,dbus_watch_get_unix_fd(w),NULL);
}

/**
 * Sometimes DBus wants to temporarily disable a file descriptor, there is two ways to handle this case, 
 * either call dbus_watch_get_enabled on every watch at every tick (not very efficient) or use the toggle
 * watch function to suspend the file descriptor. In our case we set the event flag to 0 when the watch is
 * disabled
 */
void toggle_watch(DBusWatch *w, void *data){
  short cond;
  //if the watch is enabled, get the events we want to register
  if(dbus_watch_get_enabled(w)){
    cond = EPOLLHUP | EPOLLERR;
    unsigned int flags = dbus_watch_get_flags(w);
    
    if(flags & DBUS_WATCH_READABLE) cond |= EPOLLIN;
    if(flags & DBUS_WATCH_WRITABLE) cond |= EPOLLOUT;
  //if not enabled, set the event flag to 0, which will disable the FD to epoll  
  }else{
    cond = 0;
  }

  //get the JVM environment
  context* ctx = (context*) data;
  int fd = dbus_watch_get_unix_fd(w);

  epoll_event event;
  event.events = cond;
  event.data.ptr = w;

  epoll_ctl(ctx->epollFD,EPOLL_CTL_MOD,fd,&event);
}