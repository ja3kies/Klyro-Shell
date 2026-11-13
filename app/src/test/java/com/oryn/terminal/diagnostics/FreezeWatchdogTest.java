package com.oryn.terminal.diagnostics;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class FreezeWatchdogTest {
    FreezeWatchdog w=new FreezeWatchdog();
    List<FreezeWatchdog.Event> at(long n){return w.tick(n);}
    void resume(){w.lifecycle(true,0);}
    void ping(){resume();assertEquals(FreezeWatchdog.Kind.PING,at(1000).get(0).kind);}
    boolean has(List<FreezeWatchdog.Event> events,FreezeWatchdog.Kind k){return events.stream().anyMatch(e->e.kind==k);}
    @Test public void backgroundHasNoHeartbeat(){assertTrue(at(90000).isEmpty());}
    @Test public void firstResumeGetsWarmup(){resume();assertTrue(at(999).isEmpty());assertEquals(1,at(1000).size());}
    @Test public void onlyOneOutstandingPing(){ping();for(int i=1001;i<3000;i++)assertTrue(at(i).isEmpty());}
    @Test public void stallAtTwoSeconds(){ping();assertTrue(has(at(3000),FreezeWatchdog.Kind.UI_STALL_DETECTED));}
    @Test public void noFreezeBeforeFiveSeconds(){ping();assertFalse(has(at(5999),FreezeWatchdog.Kind.UI_FREEZE_DETECTED));}
    @Test public void sixSecondBlockCapturedBeforeRecovery(){ping();assertTrue(has(at(6000),FreezeWatchdog.Kind.UI_FREEZE_DETECTED));assertTrue(has(w.acknowledge(1,7000),FreezeWatchdog.Kind.UI_RECOVERED));}
    @Test public void persistentBlockHasTwoSnapshots(){ping();assertTrue(has(at(6000),FreezeWatchdog.Kind.UI_FREEZE_DETECTED));assertTrue(has(at(11000),FreezeWatchdog.Kind.SECOND_SNAPSHOT));}
    @Test public void noSnapshotFlood(){ping();at(11000);for(int i=0;i<1000;i++)assertTrue(at(12000+i).isEmpty());}
    @Test public void recoveryDurationIsMonotonic(){ping();at(6000);assertEquals(6500,w.acknowledge(1,7500).get(0).duration);}
    @Test public void ackWithoutStallDoesNotCreateIncident(){ping();assertTrue(w.acknowledge(1,1001).isEmpty());}
    @Test public void staleGenerationCannotRecoverNewPing(){ping();w.lifecycle(false,2000);w.lifecycle(true,3000);at(4000);assertTrue(w.acknowledge(1,5000).isEmpty());assertTrue(has(at(9000),FreezeWatchdog.Kind.UI_FREEZE_DETECTED));}
    @Test public void pauseInvalidatesPendingPing(){ping();w.lifecycle(false,2000);assertTrue(at(99999).isEmpty());}
    @Test public void stopDoesNotCountBackgroundTime(){ping();w.lifecycle(false,2000);w.lifecycle(false,3000);w.lifecycle(true,99999);assertTrue(at(100000).isEmpty());}
    @Test public void resumedMainCanEndStallViaPause(){ping();at(6000);assertTrue(has(w.lifecycle(false,7000),FreezeWatchdog.Kind.UI_RECOVERED));}
    @Test public void multipleIncidentsRemainIndependent(){ping();at(6000);w.acknowledge(1,7000);assertTrue(has(at(7001),FreezeWatchdog.Kind.PING));assertTrue(has(at(12001),FreezeWatchdog.Kind.UI_FREEZE_DETECTED));}
}
