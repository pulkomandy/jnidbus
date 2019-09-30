![](https://travis-ci.com/viveris/jnidbus.svg?branch=master) ![](https://img.shields.io/badge/Kotlin-JVM-green.svg) ![](https://img.shields.io/badge/-Viveris%20Technologies-critical?style=flat-square)



# JNIDBus - A JNI wrapper around libdbus

JNIDbus is a Java JNI wrapper around the native library libdbus allowing an easy and straightforward use of Dbus from Java code in an object oriented way. Keep in mind that this library is a wrapper around the official library and not a protocol implementation like [this one](https://github.com/hypfvieh/dbus-java).

JNIDbus is built around a single-threaded event loop, making it lightweight and easy to use but the developer must keep in mind he must avoid having long running code in its handlers or else the event loop will stall.

This library is distributed under the [AFL licence](https://opensource.org/licenses/AFL-3.0).

## Message serialization

JNIdbus uses annotations and reflection to serialize POJO into DBus messages. The following types are supported:

- Boolean, Byte, Short, Integer, Long, Double and their primitive equivalent
- String
- Nested serializable objects
- Lists

### The basics

In order to make an object serializable for JNIDBus, it must extends the `Message` class and have the `DBusType` annotation. The library will try to cache the POJO reflection information and check that it respect the signature given in the `signature` property of the annotation so the developer can quickly detect errors in the object mappings.

*<u>example for a basic string message:</u>*

```java
@DBusType(
    /* please refer to the DBus documentation for more information
     * about signature format
     */
    signature = "s",
    /* fields mapped to the signature, if your signature contains multiple elements
     * the fields will be mapped from left to right to the first element found
     */
    fields = "string"
)
public class StringMessage extends Message {
    /* fields exposed to JNIDBus must have setters and getters respecting the classic
     * getXXX and setXXX format.
     */
    private String string;

    public String getString() {
        return string;
    }
    public void setString(String string) {
        this.string = string;
    }
}
```

### Nested objects

When dealing with nested objects, the signature of the parent object must include the signature of its child object. This choice was made to avoid implicit signature modification when modifying the child object. If your child object is an inner class, don't forget to make it public and static so JNIDBus can instantiate it.

In the example below if you change the signature of `SubObject` you will have to explicitely update the signature of `NestedObject` too or else JNIDBus will throw en exception.

*<u>example for a nested object message:</u>*

```java
@DBusType(
    /* The following signature means we have a root object containing an integer
     * and another object. The child object contain a single string. Please note that
     * you have to specify the child object signature in the parent object and in the
     * child object
     */
    signature = "i(s)",
    fields = {"integer","object"}
)
public class NestedObject extends Message {
    private int integer;
    private SubObject object;

    public int getInteger() ...
    public void setInteger(int integer) ...

    public SubObject getObject() ...
    public void setObject(SubObject object) ...
}

@DBusType(
    signature = "s",
    fields = {"string"}
)
public class SubObject extends Message{
    private String string;

    public String getString() ...
    public void setString(String string) ...
}

```

### Arrays

The DBus array type is mapped to either the `List` class or to native arrays . A List/array can contain anything serializable, including other lists/arrays. As JNIDBus uses reflection to know the type of the List items, the generic type must always be explicitly used in the getters and setters

*<u>example for a nested list message:</u>*

```java
@DBusType(
    /* This signature correspond to an array containing other arrays of integers */
    signature = "aai",
    fields = "array"
)
public class CollectionOfCollectionArray extends Message {
    private List<List<Integer>> array;

    //always explicitly give the precise generic type
    public List<List<Integer>> getArray() ...
        
	//for setters too
    public void setArray(List<List<Integer>> array) ...
}
```

### Dictionaries (`Maps`)

The DBus arrays of `dict_entries` are mapped to the `Map` class, the key must be a primitive DBus type (refer to the DBus documentation for a definition of primitive) and its value can contain nested objects, arrays or maps.  As for the `List`, the generic types must be explicitly used in the getters/setters.

*<u>example for a map message:</u>*

```java
@DBusType(
    /* This signature correspond to an array containing dict_entries of a string and an  
     * integer. In the java world, it's a Map with a string as key and integers as values
     */
    signature = "a{si}",
    fields = "map"
)
public class MapMessage extends Message {
    private Map<String,Integer> map;

    //always explicitly give the precise generic type
    public Map<String,Integer> getMap() ...
        
	//for setters too
    public void setMap(Map<String,Integer> map) ...
}
```

### Enums

You can use `Enums` in your messages, they will be transferred by name or by ordinal according to the signature you gave to the `DBusType` annotation.

<u>*example of an `Enum` message:*</u>

```java
@DBusType(
    //"byName" will be transferred as a string and "byOrdinal" as an integer
    signature = "si",
    fields = {"byName","byOrdinal"}
)
public class EnumMessage extends Message {
    private Enum byName;
    private Enum byOrdinal;
   
    public Enum getByName() { ... }
    public void setByName(Enum byName) { ... }
    public Enum getByOrdinal() { ... }
    public void setByOrdinal(Enum byOrdinal) { ... }

    public static enum Enum{ ... }
}
```



### The EmptyMessage

if your message does not contain any data, the `Message` class contains a static `EMPTY` property which contains an empty Message that can be used. Using this object allows some internal optimizations and it is recommended to use it whenever you can.

## How to use the DBus object

### Handlers

Handlers are classes that will be able to receive signals and expose method calls to DBus. Those classes must extends the `GenericHandler` class and be annotated with the `Handler` annotation which will give JNIDBus informations about the object path and DBus interface you want to match.

Your handler methods must be annotated with the `HandlerMethod` annotation which will define the member (method or signal name) and type of handler (Signal or Call). Please note that non-annotated methods will be ignored and left unprocessed.

Signal handler methods must return void and Call handler methods must return an object extending the  `Message` class. The signatures of the handler method will be inferred from their parameter types and return type. You can have multiple methods bound to the same member as long as they have different input signature.

Handlers will be executed on the same thread as the event loop which means that the kind of code you write in your handler will have a great impact on the overall latency of the event loop. You must avoid blocking code at all cost.

*<u>example of a Signal and Call handler:</u>*

```java
@Handler(
    /* please make sure tor espect the Dbus object path format (which is pretty much
     * the same as Java namespace format)
     */
    path = "/some/object/path",
    interfaceName = "some.dbus.interface"
)
public class SomeHandler extends GenericHandler {
    @HandlerMethod(
        //the member can be different from the java method name
        member = "someSignal",
        type = HandlerType.SIGNAL
    )
    //Here our signal do not have any data so we can use the EmptyMessage
    public void someSignal(Message.EmptyMessage emptyMessage) ...

    @HandlerMethod(
        member = "stringSignal",
        type = HandlerType.METHOD
    )
    public SomeOutput someCall(SomeInput input) ...
}
```

*<u>how to use register the handler to DBus:</u>*

```java
//connect to the DBus deamon
Dbus receiver = new Dbus(BusType.SESSION,"my.bus.name");
//instantiate your handler
SomeHandler handler = new SomeHandler();
//add it to DBus, JNIDBus will automatically check your bindings and throw if something is wrong.
this.receiver.addHandler(handler);
```

### Asynchronous handler

As we have seen above, you must avoid blocking task on the event loop, but how to do long running task then? JNIDBus support asynchronous computation using its own `Promise` class. A handler method can return a `Promise` that will be resolved by another thread at a later time when the computation is done

*<u>example of an asynchronous Call handler:</u>*

```java
@Handler(
    ...
)
public class CallHandler extends GenericHandler {
    @HandlerMethod(
        member = "blockingCall",
        type = HandlerType.METHOD
    )
    
    public Promise<Message.EmptyMessage> blockingCall(Message.EmptyMessage emptyMessage){
        final Promise<Message.EmptyMessage> promise = new Promise<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //a very long computation
                ...
                //we are done, resolve the promise
                promise.resolve(Message.EMPTY);
            }
        }).start();
        return promise;
    }
}
```

### Exception handling

Exception handling is made through the `DBusException` class, which contains two fields: `code` and `message` which correspond to the DBus error fields of the same name. If an error happens in your handler method call, you should catch it and throw a new `DBusException` with tis field correctly set so the caller can process the error.

If an unexpected exception happens in a handler, a `DBusException` will be automatically created with its code being the name of the exception class and its message the message of the exception. Beware, if an exception happen outside of the event loop (ie. asynchronous handler), uncaught exception won't be automatically sent back to the caller and will be lost in the current thread exception handler.

### Send a call

JNIDBus provide a way to represent your DBus distant objects through `Java proxies`. You describe your DBus object as an interface with a `@RemoteInterface` annotation and annotate the methods with `@RemoteMember`. All of the interface methods must be annotated.

There must be only one parameter on each method which must be serializable. If your call do not have any parameter you can omit it instead of using the `EMptyMessage` class. Each method must return a `PendingCall` with its generic type being the expected return type.

The`PendingCall` class allows you to bind a listener that will be notified when a result or an error is received. A listener will be notified once and further results/errors will be ignored. you can cancel a call by manually calling the `fail()` method on the `PendingCall` which will notify the listener and block any incoming result. The Listener will be executed on same thread as the event loop so you must avoid blocking code at all cost.

Result or errors ignored will be stored in the `PendingCall` object for debug purposes.

*<u>example of a call without input that returns a string:</u>*

```java
@RemoteInterface("call.interface.name")
public interface MyRemoteObject{
    
    @RemoteMember("someCall")
    PendingCall<StringMessage> call();
    //equivalent to: 
    //PendingCall<StringMessage> call(Message.EmptyMessage msg)
}
```

*<u>example of a listener for the above call:</u>*

```java
public class StringListener implements PendingCall.Listener<StringMessage>{
    @Override
    public void notify(StringMessage value) {
        //do something
    }

    @Override
    public void error(DBusException t) {
        //do something
    }
}

```

*<u>how to use all of the above:</u>*

```java
//connect to the DBus deamon
Dbus sender = new Dbus(BusType.SESSION,"my.bus.name");

//create the proxy instance of your remote object
MyRemoteObject remote = sender.createRemoteObject("receiver.bus.name",
                                                  "/remote/object/path",
                                                  MyRemoteObject.class);
//call the method on your remote object
PendingCall<StringMessage> pending = remote.call();

//create the listener
StringListener l = new StringListener();

//bind it, now the listener will be executed when a result arrives
pending.setListener(l);
```

### Send a Signal

Signals are represented with inner classes of a `RemoteInterface` extending the `Signal` class. The generic type of the `Signal` class will be the data attached to this signal

*<u>example of a string signal and of an empty signal:</u>*

```java

@RemoteInterface("call.interface.name")
public interface MyRemoteObject{
    
    @RemoteMember("emptySignal")
    class EmptySignal extends Signal<Message.EmptyMessage>{
        public EmptySignal() {
            //the super constructor takes as argument an instance of the Message to send
            super(Message.EMPTY);
        }
    }

    @RemoteMember("stringSignal")
    class StringSignal extends Signal<StringMessage>{
        public StringSignal(StringMessage msg) {
            super(msg);
        }
    }
}
```

*<u>how to send the signals:</u>*

```java
//connect to the DBus deamon
Dbus sender = new Dbus(BusType.SESSION,"my.bus.name");

//create and fill your message
StringMessage msg = new StringMessage();
msg.setString("A string");

//simply send it
sender.sendSignal("/remote/object/path",new MyRemoteObject.StringSignal(msg));
```

## Kotlin

A support library for Kotlin is available under the artifact `jnidbus-kotlin`, it provides basic support for coroutines through the `await()` extension on the `PendingCall` class and it also allows for DBus handlers to declare suspending methods. In order to do so, your handler class must extends the `KotlinGenericHandler` class instead of the `GenericHandler` one.

Please note that you must explicitly register the kotlin extension to jnidbus by using the `KotlinMethodInvocator.registerKotlinInvocator()` before registering any kotlin handler

*<u>how to use jnidbus-kotlin:</u>*

```kotlin
/// How to write a suspending dbus handler ///
KotlinMethodInvocator.registerKotlinInvocator()

@Handler(path = "...", interfaceName = "...")
class CallHandler : KotlinGenericHandler() {

    @HandlerMethod(member = "suspendingCall", type = MemberType.METHOD)
    suspend fun suspendingCall(emptyMessage: Message.EmptyMessage): SingleStringMessage {
        //coroutines are awesome
        delay(2000)
        return SingleStringMessage().apply { string = "test" }
    }
}

/// How to do a suspending dbus call ///
suspend fun suspendingDBusCall() : SingleStringMessage{
    val remoteObj = dbus.createRemoteObject("...", "...",SomeRemoteInterface::class.java)
    val pending = remoteObj.suspendingCall()
    return pending.await()
}

```



## Planned tasks

- Support `OBJECT_PATH` type
- Refactor `serialization.cpp` to use proper OOP and cleanup the code
- Add the capability to create downcasted typed array in JNI code instead of plain `Object[]` objects
- Use direct `ByteBuffer` instead of plain `Object` arrays to avoid copies.

## FAQ

##### Which java versions are compatible?

Java 8 to Java 11 are tested by the CI, Java 7 should run but is not tested. You will also need `libc` and `libdbus-1` to run the native code 

##### How fast is this library

I was able to get around 40k/s "complex" signals sent and received on a single event loop on my modest i3-4130 work machine. I was able to get around 95k/s empty signals with the same setup. This should satisfy most of the use cases so unless you really need to push DBus to its limit it's enough.

##### I found a bug

Feel free to post an issue describing precisely what behavior you got and whet result you expected. If you can provide a small reproducer we will be able to get rid of this bug even faster.

##### How to contribute

Fork this project and send us a merge request, we will make sure to get back to you as soon as possible. You should also open an issue beforehand so we can give you feedback before you write any code.