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
      verifyCarriers(binding, callbacks, args.length > 1);
    }
    if (expired.scope().isAlive()) throw new AssertionError("upcall arena lifetime");
    System.out.println("Scoped callbacks, native threads and exception status passed");
  }

  private static void verifyCarriers(CallbackApiFFM.LibraryBinding binding, Arena arena,
                                     boolean jni) throws Throwable {
    var bytes = CallbackApiFFM.ByteResultUpcalls.create(fail -> {
      if (fail) throw new IllegalStateException("byte sentinel");
      return (byte) 200;
    }, (byte) 255, arena);
    var shorts = CallbackApiFFM.ShortResultUpcalls.create(fail -> {
      if (fail) throw new IllegalStateException("short sentinel");
      return (short) 60000;
    }, (short) 65535, arena);
    if (binding.invokeByte(bytes, false) != 200 || binding.invokeByte(bytes, true) != 255
        || binding.invokeShort(shorts, false) != 60000 || binding.invokeShort(shorts, true) != 65535)
      throw new AssertionError("unsigned callback return extension");
    var notices = new AtomicInteger();
    var notify = CallbackApiFFM.NotifyUpcalls.create(value -> {
      notices.addAndGet(value);
      throw new IllegalStateException("void callback exception is discarded");
    }, arena);
    binding.callNotify(notify, 7);
    if (notices.get() != 7) throw new AssertionError("void callback");
    var memory = arena.allocate(1);
    var pointers = CallbackApiFFM.PointerResultUpcalls.create(value -> {
      if (value.address() == 0) throw new IllegalStateException("pointer sentinel");
      return value;
    }, MemorySegment.NULL, arena);
    if (binding.invokePointer(pointers, memory).address() != memory.address()
        || binding.invokePointer(pointers, MemorySegment.NULL).address() != 0)
      throw new AssertionError("pointer callback sentinel");
    var floats = CallbackApiFFM.FloatResultUpcalls.create(value -> {
      if (value < 0) throw new IllegalStateException("float sentinel");
      return value * 2;
    }, -7.5f, arena);
    var doubles = CallbackApiFFM.DoubleResultUpcalls.create(value -> {
      if (value < 0) throw new IllegalStateException("double sentinel");
      return value * 2;
    }, -8.5, arena);
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
