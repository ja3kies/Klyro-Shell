package com.oryn.terminal.diagnostics;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Deliberate stalls in a test process; never invoked by app startup. */
public class FreezeRecorderDeviceTest {
    private void verifyStall(long duration, boolean twoStacks) throws Exception {
        org.junit.Assume.assumeFalse("Deliberate stalls require no live user sessions",
            com.oryn.terminal.KlyroSessionRuntime.INSTANCE.getSessions().hasLiveSessions());
        FreezeRecorder recorder=FreezeRecorder.INSTANCE;
        recorder.start(InstrumentationRegistry.getInstrumentation().getTargetContext());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> recorder.transition(FreezeEvents.Code.RESUMED));
        SystemClock.sleep(1600);
        java.util.List<String> before=recorder.repository().list();
        CountDownLatch blocking=new CountDownLatch(1), recovered=new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> {
            blocking.countDown(); SystemClock.sleep(duration); recovered.countDown();
        });
        assertTrue(blocking.await(2,TimeUnit.SECONDS));
        String id=null;
        long deadline=SystemClock.elapsedRealtime()+5900;
        while(SystemClock.elapsedRealtime()<deadline) {
            for(String candidate:recorder.repository().list())
                if(!before.contains(candidate) && recorder.repository().read(candidate).contains("UI_FREEZE_DETECTED")) id=candidate;
            if(id!=null) break;
            SystemClock.sleep(50);
        }
        assertNotNull("snapshot must persist before main recovery",id);
        assertEquals(1,recovered.getCount());
        assertTrue(recovered.await(8,TimeUnit.SECONDS));
        SystemClock.sleep(800);
        String report=recorder.repository().read(id);
        assertTrue(report.contains("UI_RECOVERED"));
        if(twoStacks) assertTrue(report.split("JAVA_STACKS_DO_NOT_PROVE_NATIVE_ROOT_CAUSE",-1).length>=3);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> recorder.transition(FreezeEvents.Code.PAUSED));
    }
    @Test public void sixSecondMainStallPersistsBeforeRecovery() throws Exception { verifyStall(6500,false); }
    @Test public void persistentStallHasTwoSeparatedCaptures() throws Exception { verifyStall(11000,true); }
}
