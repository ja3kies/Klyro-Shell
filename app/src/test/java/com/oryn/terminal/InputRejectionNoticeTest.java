package com.oryn.terminal;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class InputRejectionNoticeTest {
    private static final class Clock {
        volatile long now;
        int schedules;
        final ConcurrentSkipListMap<Long, List<Runnable>> tasks = new ConcurrentSkipListMap<>();
        final List<Long> notices = Collections.synchronizedList(new ArrayList<>());
        Runnable duringNotice = () -> {};
        final InputRejectionNotice notice = new InputRejectionNotice(() -> now,
                (task, at) -> { schedules++; tasks.computeIfAbsent(at, k -> new CopyOnWriteArrayList<>()).add(task); },
                task -> tasks.values().removeIf(list -> { list.remove(task); return list.isEmpty(); }),
                () -> { notices.add(now); duringNotice.run(); });
        void advance(long target) {
            Map.Entry<Long,List<Runnable>> first;
            while ((first = tasks.firstEntry()) != null && first.getKey() <= target) {
                if (!tasks.remove(first.getKey(), first.getValue())) continue;
                now = Math.max(now, first.getKey());
                for (Runnable task : first.getValue()) task.run();
            }
            now = target;
        }
    }
    @Test public void firstRejectionNotifiesWithoutCooldownDelay() {
        Clock c = new Clock(); c.notice.reject(); c.advance(0);
        assertEquals(Collections.singletonList(0L), c.notices);
    }
    @Test public void tenThousandRejectionsScheduleOnlyOneCallback() {
        Clock c = new Clock();
        for (int i = 0; i < 10000; i++) c.notice.reject();
        assertEquals(1, c.schedules); c.advance(0);
        assertEquals(1, c.notices.size());
    }
    @Test public void burstDuringCooldownHasOneTrailingNotice() {
        Clock c = new Clock(); c.notice.reject(); c.advance(0);
        for (int i = 0; i < 10000; i++) c.notice.reject();
        assertEquals(2, c.schedules);
        c.advance(InputRejectionNotice.COOLDOWN_MS - 1); assertEquals(1, c.notices.size());
        c.advance(InputRejectionNotice.COOLDOWN_MS); assertEquals(2, c.notices.size());
        c.advance(100000); assertEquals(2, c.notices.size()); assertTrue(c.tasks.isEmpty());
    }
    @Test public void rejectionDuringNotificationCannotBypassCooldown() {
        Clock c = new Clock(); c.duringNotice = () -> { if (c.notices.size() == 1) c.notice.reject(); };
        c.notice.reject(); c.advance(0); assertEquals(1, c.notices.size());
        c.advance(InputRejectionNotice.COOLDOWN_MS); assertEquals(2, c.notices.size());
    }
    @Test public void disposeCancelsPendingAndFutureNotices() {
        Clock c = new Clock(); c.notice.reject(); c.notice.close(); c.notice.reject();
        c.advance(100000); assertTrue(c.notices.isEmpty()); assertTrue(c.tasks.isEmpty());
    }
    @Test public void callbackAlreadyDequeuedCannotNotifyAfterDispose() {
        Clock c = new Clock(); c.notice.reject(); Runnable stale = c.tasks.firstEntry().getValue().get(0);
        c.notice.close(); stale.run(); assertTrue(c.notices.isEmpty());
    }
    @Test public void failedNotificationDoesNotWedgeSubsequentRejections() {
        Clock c = new Clock(); c.duringNotice = () -> { throw new IllegalStateException("fixture"); };
        c.notice.reject(); try { c.advance(0); fail(); } catch (IllegalStateException expected) { }
        c.duringNotice = () -> {}; c.notice.reject(); c.advance(InputRejectionNotice.COOLDOWN_MS);
        assertEquals(2, c.notices.size());
    }
    @Test(timeout = 10000) public void concurrentProducersAndDispatchKeepOnePendingCallback() throws Exception {
        Clock c = new Clock(); ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> producers = new ArrayList<>();
            for (int t = 0; t < 8; t++) producers.add(pool.submit(() -> {
                try { start.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
                for (int i = 0; i < 10000; i++) c.notice.reject();
            }));
            start.countDown();
            while (producers.stream().anyMatch(f -> !f.isDone())) {
                c.advance(c.now + 100);
                assertTrue(c.tasks.size() <= 1);
            }
            for (Future<?> producer : producers) producer.get();
            c.advance(c.now + InputRejectionNotice.COOLDOWN_MS);
            for (int n = 1; n < c.notices.size(); n++)
                assertTrue(c.notices.get(n) - c.notices.get(n - 1) >= InputRejectionNotice.COOLDOWN_MS);
            assertTrue(c.tasks.isEmpty());
        } finally { c.notice.close(); pool.shutdownNow(); }
    }
    @Test(timeout = 10000) public void concurrentCloseRejectsAllRemainingWork() throws Exception {
        Clock c = new Clock(); ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> producers = new ArrayList<>();
            for (int t = 0; t < 4; t++) producers.add(pool.submit(() -> {
                for (int i = 0; i < 10000; i++) c.notice.reject();
            }));
            c.advance(0); c.notice.close(); int shown = c.notices.size();
            for (Future<?> producer : producers) producer.get();
            c.advance(100000); assertEquals(shown, c.notices.size()); assertTrue(c.tasks.isEmpty());
        } finally { c.notice.close(); pool.shutdownNow(); }
    }
}
