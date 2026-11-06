/** Consumer-owned thread and disposal guards shared by the FFM and direct SVM fixtures. */
final class ConfinedOwner<T> {
  @FunctionalInterface interface Destructor<T> { void destroy(T address) throws Throwable; }
  @FunctionalInterface interface Action { void run() throws Throwable; }

  private final T address;
  private final Destructor<T> destructor;
  private final Thread requiredThread;
  private boolean closed;

  ConfinedOwner(T address, Destructor<T> destructor, Thread requiredThread) {
    if (requiredThread != Thread.currentThread()) throw new AssertionError("missing creator thread");
    this.address = address;
    this.destructor = destructor;
    this.requiredThread = requiredThread;
  }

  T address() {
    checkThread();
    if (closed) throw new IllegalStateException("released");
    return address;
  }

  void close() throws Throwable {
    checkThread();
    if (!closed) {
      destructor.destroy(address);
      closed = true;
    }
  }

  private void checkThread() {
    if (Thread.currentThread() != requiredThread) throw new WrongThreadException();
  }

  void rejectsOtherThread() throws Throwable {
    var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    var thread = new Thread(() -> {
      try {
        rejects(WrongThreadException.class, this::address);
        rejects(WrongThreadException.class, this::close);
      } catch (Throwable error) { failure.set(error); }
    });
    thread.start();
    thread.join();
    if (failure.get() != null) throw failure.get();
  }

  static void rejects(Class<? extends Throwable> type, Action action) throws Throwable {
    try { action.run(); } catch (Throwable failure) {
      if (type.isInstance(failure)) return;
      throw failure;
    }
    throw new AssertionError("expected " + type.getName());
  }
}
