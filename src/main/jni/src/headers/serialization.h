/* Copyright 2019, Viveris Technologies <opensource@toulouse.viveris.fr>
 * Distributed under the terms of the Academic Free License.
 */
#include <dbus/dbus.h>
#include <jni.h>
#include <vector>
#include <string>
#include <cstring>
#include "./context.h"

#ifndef _serialization_
#define _serialization_

/**
 * Serialize a JVM Message object and transfer it to the Dbus message iterator.
 */
void serialize(context* ctx, jobject message, DBusMessageIter* container);

/**
 * deserialize a DBus message iterator into a JVM Message object
 */ 
jobject deserialize(context* ctx, DBusMessageIter* container);



/**
 * Transfer the JVM array into the container
 */
void serialize_array(context* ctx, int dbus_type, jobjectArray array, DBusMessageIter* container, DBusSignatureIter* signatureIter);

/**
 * Transfer the JVM primitive array into the container
 */
void serialize_primitive_array(context* ctx, int dbus_type, jarray object, int length,  DBusMessageIter* container);

/**
 * Transfer the container array into a JVM array
 */
jobjectArray deserialize_array(context* ctx, int dbus_type, DBusMessageIter* container);



/**
 * Transfer the JVM object into the container
 */
void serialize_element(context* ctx, int dbus_type, jobject object, DBusMessageIter* container);

/**
 * Transfer the serialized element into a JVM object
 */
jobject deserialize_element(context* ctx, DBusMessageIter* container);

#endif