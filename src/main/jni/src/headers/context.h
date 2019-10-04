/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
#include <jni.h>
#include <dbus/dbus.h>
#include <map>
#include <sys/epoll.h>
#include <unistd.h>

#ifndef _context_headers_
#define _context_headers_

    #ifndef EPOLL_MAX_EVENTS
    #define EPOLL_MAX_EVENTS (int) 64
    #endif

    /**
     * When using the JNI, we have to code in a oldish C style (for retro-compatibility purposes), this means that we can't have
     * proper objects and must instead use stateless functions. In order to make our code statefull we have to passs around a context
     * which will contain everything our code needs to run (connection, file descriptors, JVM env and objects, etc...)
     * 
     * The following structure represent this context and a pointer to it is stored in the JVM DBus object. We must free this context when
     * closing the conection. This context also allows us to store the JVM meta-data, which are not cached by default by the JVM.
     */
    struct context{
        //the JNIEnv variable passed to JNI functions is only available in the current thread and its pointer can move around
        //in order to always have a valid pointer to it, we store the VM, which never changes and call attachThread to get a pointer
        //to the JNIEnv
        JavaVM* vm;
        //DBus connection, used for anything DBus-related
        DBusConnection* connection;
        //name of our bus, we need to store it as it is needed when closing
        char* bus_name;
        //epoll file descriptor, for more information about epoll see the man page.
        int epollFD;
        //file descriptor of the eventfd used to wake the event loop when we have data coming from the JVM application
        int wakeupFD;
        //allocated struct used by the epoll_wait call to store modified FDs
        epoll_event* epollStruct;
        //JVM event loop object
        jobject eventLoop;
        //wakeup lock object
        jobject wakeup_lock;
        //should wakeup flag
        jfieldID should_wakeup_flag;
        //meta-class cache
        std::map<std::string,jclass> class_cache;
        //method ID cache
        std::map<std::string,jmethodID> method_cache;
        //field ID cache
        std::map<std::string,jfieldID> field_cache;
    };

    /**
     * Context that DBus will pass to the handler function which will call the given JVM dispatcher
     */
    struct handler_context{
        context* ctx;
        jobject dispatcher;
    };

    /**
     * Context that DBus will pass to any PendingCall when its state changes
     */
    struct pending_call_context{
        context* ctx;
        jobject promise;
    };

    /**
     * The JVM have a mechanism to manage objects referenced in JNI code to avoid concurrent garbage collection.
     * by default, when retreiving anything from the JVM, it will give us a Local reference, the JVM will try
     * to detect when a local reference is expired by itslef when the function returns. When working with asynchronous
     * callbacks we don't want the JVM to do this, and instead we will manage references by ourslef. To do this we use
     * GlobalReferences, which creates an unmanaged reference to the object.
     * 
     * This function is used to find meta-classes in the JVM and cache it for later use. When retreiving a class
     * for the first time, the function will call env->FindClass() and make a global reference for it, any call made
     * from there will return the cached global reference.
     * 
     */
    jclass find_class(context* context, const char* name);
    jclass find_array_class(context* context, const char* name);

    /**
     * Like the meta-class cache, this one takes care of method IDs
     */
    jmethodID find_method(context* context, const char* class_name, const char* name, const char* signature);

    /**
     * Same thing but for fields IDs
     */
    jfieldID find_field(context* context, const char* class_name, const char* name, const char* signature);

    /**
     * Retreive a pointer to the JNIEnv from the VM object
     */
    void get_env(context* context, JNIEnv** env);

    /**
     * Free all the resources allocated by the context
     */
    void close_context(context* ctx);
#endif