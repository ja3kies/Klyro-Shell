package com.oryn.terminal;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Main-thread batch ownership; scheduling never depends on whether a tab was removed. */
public final class OutputUpdateBatch implements AutoCloseable {
    private final Set<Long> pending = new LinkedHashSet<>();
    private final Consumer<Runnable> schedule, cancel;
    private final Consumer<Set<Long>> publish;
    private boolean scheduled, closed;
    private final Runnable task = this::flush;

    public OutputUpdateBatch(Consumer<Runnable> schedule, Consumer<Runnable> cancel,
                             Consumer<Set<Long>> publish) {
        this.schedule = schedule; this.cancel = cancel; this.publish = publish;
    }
    public synchronized void mark(long id) {
        if (closed) return;
        pending.add(id);
        if (!scheduled) { scheduled = true; schedule.accept(task); }
    }
    public synchronized void remove(long id) { pending.remove(id); }
    private void flush() {
        final Set<Long> batch;
        synchronized (this) {
            scheduled = false;
            if (closed || pending.isEmpty()) return;
            batch = new LinkedHashSet<>(pending);
            pending.clear();
        }
        // Publish outside the lock: a callback may enqueue the next burst.
        publish.accept(batch);
    }
    @Override public synchronized void close() {
        closed = true; pending.clear(); cancel.accept(task); scheduled = false;
    }
}
