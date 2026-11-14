package com.oryn.terminal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Thread-safe notice backpressure; rejected bytes are never retained or retried here. */
public final class InputRejectionNotice implements AutoCloseable {
    public static final long COOLDOWN_MS = 4000L;
    private long nextNotice;
    private final OutputUpdateBatch pending;
    private Runnable flush;
    private final Runnable dispatch = () -> {
        synchronized (InputRejectionNotice.this) {
            if (flush != null) flush.run();
        }
    };

    public InputRejectionNotice(LongSupplier uptime, BiConsumer<Runnable, Long> scheduleAt,
                                Consumer<Runnable> cancel, Runnable notify) {
        pending = new OutputUpdateBatch(
                task -> {
                    flush = task;
                    scheduleAt.accept(dispatch, Math.max(uptime.getAsLong(), nextNotice));
                },
                task -> cancel.accept(dispatch),
                ids -> {
                    nextNotice = uptime.getAsLong() + COOLDOWN_MS;
                    notify.run();
                });
    }

    public synchronized void reject() { pending.mark(0L); }
    @Override public synchronized void close() { pending.close(); }
}
