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
    public void mark(long id) {
        if (closed) return;
        pending.add(id);
        if (!scheduled) { scheduled = true; schedule.accept(task); }
    }
    public void remove(long id) { pending.remove(id); }
    private void flush() {
        scheduled = false;
        if (closed || pending.isEmpty()) return;
        Set<Long> batch = new LinkedHashSet<>(pending);
        pending.clear();
        publish.accept(batch);
    }
    @Override public void close() {
        closed = true; pending.clear(); cancel.accept(task); scheduled = false;
    }
}
