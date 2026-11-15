package com.oryn.terminal.diagnostics;

import java.util.ArrayList;
import java.util.List;

/** Monotonic state machine. No I/O or Android calls under its short state lock. */
public final class FreezeWatchdog {
    public enum Kind { PING, UI_STALL_DETECTED, UI_FREEZE_DETECTED, SECOND_SNAPSHOT, UI_RECOVERED }
    public static final class Event {
        public final Kind kind;
        public final long generation, started, duration;
        Event(Kind k, long g, long s, long d) { kind=k; generation=g; started=s; duration=d; }
    }
    private long generation, eligibleAt, sentAt, firstCaptureAt;
    private boolean resumed, pending, stalled, captured, second;
    public synchronized List<Event> lifecycle(boolean active, long now) {
        List<Event> events = acknowledge(generation, now);
        generation++;
        resumed=active; pending=false; stalled=false; captured=false; second=false;
        eligibleAt=now+1000; // First foreground frame gets a warm-up; later pings do not.
        return events;
    }
    public synchronized List<Event> tick(long now) {
        List<Event> events=new ArrayList<>();
        if (!resumed || now<eligibleAt) return events;
        if (!pending) {
            pending=true; sentAt=now; stalled=false; captured=false; second=false;
            events.add(new Event(Kind.PING,generation,sentAt,0));
            return events;
        }
        long elapsed=Math.max(0,now-sentAt);
        if (elapsed>=2000 && !stalled) { stalled=true; events.add(new Event(Kind.UI_STALL_DETECTED,generation,sentAt,elapsed)); }
        if (elapsed>=5000 && !captured) { captured=true; firstCaptureAt=now; events.add(new Event(Kind.UI_FREEZE_DETECTED,generation,sentAt,elapsed)); }
        if (captured && now-firstCaptureAt>=5000 && !second) { second=true; events.add(new Event(Kind.SECOND_SNAPSHOT,generation,sentAt,elapsed)); }
        return events;
    }
    public synchronized boolean isPending(long token, long started) {
        return resumed && pending && generation==token && sentAt==started;
    }
    public synchronized List<Event> acknowledge(long token, long now) {
        List<Event> events=new ArrayList<>();
        if (!resumed || !pending || generation!=token) return events;
        if (stalled) events.add(new Event(Kind.UI_RECOVERED,generation,sentAt,Math.max(0,now-sentAt)));
        pending=false; stalled=false; captured=false; second=false;
        return events;
    }
}
