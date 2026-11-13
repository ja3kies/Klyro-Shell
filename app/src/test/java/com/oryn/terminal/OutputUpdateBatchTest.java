package com.oryn.terminal;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class OutputUpdateBatchTest {
    static final class Clock {
        long now;
        final TreeMap<Long,List<Runnable>> tasks = new TreeMap<>();
        final List<Set<Long>> batches = new ArrayList<>();
        Runnable duringPublish = () -> {};
        final OutputUpdateBatch batch = new OutputUpdateBatch(
                r -> tasks.computeIfAbsent(now + 120, k -> new ArrayList<>()).add(r),
                r -> tasks.values().forEach(list -> list.remove(r)),
                ids -> { batches.add(ids); duringPublish.run(); });
        void advance(long target) {
            while (!tasks.isEmpty() && tasks.firstKey() <= target) {
                now = tasks.firstKey();
                for (Runnable r : tasks.pollFirstEntry().getValue()) r.run();
            }
            now = target;
        }
        Set<Long> ids(Long... ids) { return new LinkedHashSet<>(Arrays.asList(ids)); }
    }
    @Test public void deadlineDoesNotPublishEarly() { Clock c=new Clock();c.batch.mark(1);c.advance(119);assertTrue(c.batches.isEmpty());c.advance(120);assertEquals(c.ids(1L),c.batches.get(0)); }
    @Test public void tenThousandChunksAreOneUiPublication() { Clock c=new Clock();for(int i=0;i<10000;i++)c.batch.mark(1);assertEquals(1,c.tasks.firstEntry().getValue().size());c.advance(120);assertEquals(1,c.batches.size()); }
    @Test public void finalChunkBeforeDeadlineIsIncluded() { Clock c=new Clock();c.batch.mark(1);c.advance(119);c.batch.mark(2);c.advance(120);assertEquals(c.ids(1L,2L),c.batches.get(0)); }
    @Test public void finalChunkAfterFlushGetsItsOwnTrailingUpdate() { Clock c=new Clock();c.batch.mark(1);c.advance(120);c.batch.mark(2);c.advance(240);assertEquals(2,c.batches.size());assertEquals(c.ids(2L),c.batches.get(1)); }
    @Test public void closedTabCannotPublish() { Clock c=new Clock();c.batch.mark(1);c.batch.remove(1);c.advance(120);assertTrue(c.batches.isEmpty()); }
    @Test public void removalDoesNotScheduleAnExtraTimerForNewTab() { Clock c=new Clock();c.batch.mark(1);c.advance(60);c.batch.remove(1);c.batch.mark(2);assertEquals(1,c.tasks.size());c.advance(180);assertEquals(1,c.batches.size()); }
    @Test public void tabsAreDeduplicatedAndIndependent() { Clock c=new Clock();for(long i=1;i<=8;i++){c.batch.mark(i);c.batch.mark(i);}c.batch.remove(4);c.advance(120);assertEquals(7,c.batches.get(0).size());assertFalse(c.batches.get(0).contains(4L)); }
    @Test public void disposeCancelsAndRejectsFutureUiWork() { Clock c=new Clock();c.batch.mark(1);c.batch.close();c.batch.mark(2);c.advance(1000);assertTrue(c.batches.isEmpty()); }
    @Test public void outputDuringPublicationIsNotLost() { Clock c=new Clock();c.duringPublish=()->{if(c.batches.size()==1)c.batch.mark(2);};c.batch.mark(1);c.advance(240);assertEquals(c.ids(2L),c.batches.get(1)); }
    @Test public void publicationFailureCannotWedgeNextBurst() { Clock c=new Clock();c.duringPublish=()->{throw new IllegalStateException();};c.batch.mark(1);try{c.advance(120);fail();}catch(IllegalStateException expected){}c.duringPublish=()->{};c.batch.mark(2);c.advance(240);assertEquals(c.ids(2L),c.batches.get(1)); }
}
