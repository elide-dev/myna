// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.callbacks;

import java.lang.foreign.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public final class CallbackMain {
  public static void main(String[] args) throws Throwable {
    MemorySegment expired;
    try (var library = Arena.ofShared(); var callbacks = Arena.ofShared()) {
      var binding = new CallbackApiFFM.LibraryBinding(Path.of(args[0]), library);
      var calls = new AtomicInteger();
      var dynamic = CallbackApiFFM.ReducerUpcalls.create((left, right) -> {
        calls.incrementAndGet();
        if (left < 0) throw new IllegalStateException("sentinel case");
        return left + right;
      }, -42L, callbacks);
      var fixed = CallbackApiFFM.ReducerUpcalls.create(callbacks);
      expired = dynamic;
      for (boolean threaded : new boolean[] {false, true}) {
        if (binding.invoke(dynamic, threaded, 20, 22) != 42) throw new AssertionError("dynamic upcall");
        if (binding.invoke(dynamic, threaded, -1, 22) != -42) throw new AssertionError("dynamic exception");
        if (binding.invoke(fixed, threaded, 20, 22) != 42) throw new AssertionError("static upcall");
        if (binding.invoke(fixed, threaded, -1, 22) != -1) throw new AssertionError("static exception");
        if (args.length > 1) {
          if (CallbackApiJNI.invoke(dynamic.address(), threaded, 20, 22) != 42)
            throw new AssertionError("JNI to callback");
          if (CallbackApiJNI.invoke(fixed.address(), threaded, -1, 22) != -1)
            throw new AssertionError("JNI callback exception");
        }
      }
      if (calls.get() != (args.length > 1 ? 6 : 4)) throw new AssertionError("callback count");
      verifyCarriers(binding, callbacks, args.length > 1, false);
      verifyCarriers(binding, callbacks, args.length > 1, true);
      verifyDistinctRegistrations(binding, callbacks);
    }
    if (expired.scope().isAlive()) throw new AssertionError("upcall arena lifetime");
    System.out.println("Scoped callbacks, native threads and exception status passed");
  }

  private static MemorySegment offsetReducer(long offset, long sentinel, Arena arena) {
    return CallbackApiFFM.ReducerUpcalls.createDirect((left, right) -> {
      if (left < 0) throw new IllegalArgumentException("negative input");
      return offset + left + right;
    }, sentinel, arena);
  }

  private static void verifyDistinctRegistrations(CallbackApiFFM.LibraryBinding binding,
                                                  Arena outer) throws Throwable {
    var first = offsetReducer(10, -11, outer);
    try (var inner = Arena.ofShared()) {
      var second = offsetReducer(20, -22, inner);
      System.gc();
      for (boolean worker : new boolean[] {false, true}) {
        if (binding.invoke(first, worker, 2, 3) != 15 || binding.invoke(second, worker, 2, 3) != 25
            || binding.invoke(first, worker, -1, 0) != -11 || binding.invoke(second, worker, -1, 0) != -22)
          throw new AssertionError("callback target or sentinel crossed registrations");
      }
    }
    System.gc();
    if (binding.invoke(first, true, 2, 3) != 15)
      throw new AssertionError("closing a second registration invalidated the first");
  }

  private static void verifyCarriers(CallbackApiFFM.LibraryBinding binding, Arena arena,
                                     boolean jni, boolean direct) throws Throwable {
    CallbackApiFFM.ByteResultCallback byteTarget = fail -> {
      if (fail) throw new IllegalStateException("byte sentinel");
      return (byte) 200;
    };
    var bytes = direct ? CallbackApiFFM.ByteResultUpcalls.createDirect(byteTarget, (byte) 255, arena)
                       : CallbackApiFFM.ByteResultUpcalls.create(byteTarget, (byte) 255, arena);
    CallbackApiFFM.ShortResultCallback shortTarget = fail -> {
      if (fail) throw new IllegalStateException("short sentinel");
      return (short) 60000;
    };
    var shorts = direct ? CallbackApiFFM.ShortResultUpcalls.createDirect(shortTarget, (short) 65535, arena)
                        : CallbackApiFFM.ShortResultUpcalls.create(shortTarget, (short) 65535, arena);
    if (binding.invokeByte(bytes, false) != 200 || binding.invokeByte(bytes, true) != 255
        || binding.invokeShort(shorts, false) != 60000 || binding.invokeShort(shorts, true) != 65535)
      throw new AssertionError("unsigned callback return extension");
    var notices = new AtomicInteger();
    CallbackApiFFM.NotifyCallback notifyTarget = value -> {
      notices.addAndGet(value);
      throw new IllegalStateException("void callback exception is discarded");
    };
    var notify = direct ? CallbackApiFFM.NotifyUpcalls.createDirect(notifyTarget, arena)
                        : CallbackApiFFM.NotifyUpcalls.create(notifyTarget, arena);
    binding.callNotify(CallbackApiFFM.NotifyUpcalls.create(arena), 17);
    if (CallbackOps.lastNotice != 17) throw new AssertionError("static void callback");
    binding.callNotify(notify, 7);
    if (notices.get() != 7) throw new AssertionError("void callback");
    var memory = arena.allocate(1);
    CallbackApiFFM.PointerResultCallback pointerTarget = value -> {
      if (value.address() == 0) throw new IllegalStateException("pointer sentinel");
      return value;
    };
    var pointers = direct ? CallbackApiFFM.PointerResultUpcalls.createDirect(pointerTarget, MemorySegment.NULL, arena)
                          : CallbackApiFFM.PointerResultUpcalls.create(pointerTarget, MemorySegment.NULL, arena);
    if (binding.invokePointer(pointers, memory).address() != memory.address()
        || binding.invokePointer(pointers, MemorySegment.NULL).address() != 0)
      throw new AssertionError("pointer callback sentinel");
    CallbackApiFFM.FloatResultCallback floatTarget = value -> {
      if (value < 0) throw new IllegalStateException("float sentinel");
      return value * 2;
    };
    var floats = direct ? CallbackApiFFM.FloatResultUpcalls.createDirect(floatTarget, -7.5f, arena)
                        : CallbackApiFFM.FloatResultUpcalls.create(floatTarget, -7.5f, arena);
    CallbackApiFFM.DoubleResultCallback doubleTarget = value -> {
      if (value < 0) throw new IllegalStateException("double sentinel");
      return value * 2;
    };
    var doubles = direct ? CallbackApiFFM.DoubleResultUpcalls.createDirect(doubleTarget, -8.5, arena)
                         : CallbackApiFFM.DoubleResultUpcalls.create(doubleTarget, -8.5, arena);
    if (binding.invokeFloat(floats, 1.25f) != 2.5f || binding.invokeFloat(floats, -1) != -7.5f
        || binding.invokeDouble(doubles, 1.25) != 2.5 || binding.invokeDouble(doubles, -1) != -8.5)
      throw new AssertionError("floating point callback carriers");
    if (jni) {
      if (CallbackApiJNI.invokeByte(bytes.address(), false) != 200
          || CallbackApiJNI.invokeByte(bytes.address(), true) != 255
          || CallbackApiJNI.invokeShort(shorts.address(), false) != 60000
          || CallbackApiJNI.invokeShort(shorts.address(), true) != 65535)
        throw new AssertionError("JNI unsigned callback returns");
      CallbackApiJNI.callNotify(notify.address(), 11);
      if (notices.get() != 18
          || CallbackApiJNI.invokePointer(pointers.address(), memory.address()) != memory.address()
          || CallbackApiJNI.invokePointer(pointers.address(), 0) != 0)
        throw new AssertionError("JNI void and pointer callbacks");
      if (CallbackApiJNI.invokeFloat(floats.address(), 1.25f) != 2.5f
          || CallbackApiJNI.invokeFloat(floats.address(), -1) != -7.5f
          || CallbackApiJNI.invokeDouble(doubles.address(), 1.25) != 2.5
          || CallbackApiJNI.invokeDouble(doubles.address(), -1) != -8.5)
        throw new AssertionError("JNI floating point carriers");
    }
  }
}
