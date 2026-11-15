package com.oryn.terminal.diagnostics;
import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class FreezeEventsTest {
 @Test public void ringRetainsLatest128Events(){FreezeEvents e=new FreezeEvents();for(int i=0;i<1000;i++)e.add(FreezeEvents.Code.RESIZE,i,i,0);String s=e.snapshot();assertFalse(s.contains("871 RESIZE 871"));assertTrue(s.contains("872 RESIZE 872"));assertTrue(s.contains("999 RESIZE 999"));}
 @Test public void totalsSurviveRotationOfRing(){FreezeEvents e=new FreezeEvents();for(int i=0;i<1000;i++)e.add(FreezeEvents.Code.PTY_CALLBACK,i,0,0);assertTrue(e.snapshot().contains("PTY_CALLBACK_count=1000"));}
 @Test public void kindsHaveIndependentCounters(){FreezeEvents e=new FreezeEvents();e.add(FreezeEvents.Code.PAUSED,1,0,0);e.add(FreezeEvents.Code.RESUMED,2,0,0);assertTrue(e.snapshot().contains("PAUSED_count=1"));assertTrue(e.snapshot().contains("STOPPED_count=0"));}
 @Test public void idsAndMonotonicTimesRemainLong(){FreezeEvents e=new FreezeEvents();e.add(FreezeEvents.Code.SESSION,1L<<40,1L<<41,1L<<42);assertTrue(e.snapshot().contains((1L<<40)+" SESSION "+(1L<<41)+" "+(1L<<42)));}
 @Test public void snapshotsAreDetached(){FreezeEvents e=new FreezeEvents();String before=e.snapshot();e.add(FreezeEvents.Code.BATCH,1,1,0);assertTrue(before.contains("BATCH_count=0"));assertTrue(e.snapshot().contains("BATCH_count=1"));}
 @Test(timeout=5000) public void concurrentWorkerAndUiEventsDoNotLoseCounts()throws Exception{FreezeEvents e=new FreezeEvents();ExecutorService pool=Executors.newFixedThreadPool(4);try{Future<?>[] f=new Future[4];for(int i=0;i<4;i++)f[i]=pool.submit(()->{for(int j=0;j<10000;j++)e.add(FreezeEvents.Code.PTY_CALLBACK,j,0,0);});for(Future<?> v:f)v.get();assertTrue(e.snapshot().contains("PTY_CALLBACK_count=40000"));}finally{pool.shutdownNow();}}
 @Test(timeout=5000) public void snapshotWhileProducingStaysBounded()throws Exception{FreezeEvents e=new FreezeEvents();ExecutorService pool=Executors.newSingleThreadExecutor();try{Future<?> f=pool.submit(()->{for(int j=0;j<100000;j++)e.add(FreezeEvents.Code.BATCH,j,0,0);});for(int i=0;i<100;i++)assertTrue(e.snapshot().length()<20000);f.get();}finally{pool.shutdownNow();}}
 @Test public void lifecycleOrderSurvivesForegroundRoundTrip(){FreezeEvents e=new FreezeEvents();e.add(FreezeEvents.Code.PAUSED,1,0,0);e.add(FreezeEvents.Code.STOPPED,2,0,0);e.add(FreezeEvents.Code.STARTED,3,0,0);e.add(FreezeEvents.Code.RESUMED,4,0,0);String s=e.snapshot();assertTrue(s.indexOf("1 PAUSED")<s.indexOf("2 STOPPED"));assertTrue(s.indexOf("3 STARTED")<s.indexOf("4 RESUMED"));}
 @Test public void touchFloodCannotEvictModalLifetimeEvidence(){
  FreezeEvents e=new FreezeEvents();
  e.add(FreezeEvents.Code.MODAL_OPEN,1,42,0);
  e.add(FreezeEvents.Code.WINDOW_FOCUS,2,0,0);
  e.add(FreezeEvents.Code.MODAL_DISPOSE,3,42,0);
  for(int i=0;i<1000;i++)e.add(FreezeEvents.Code.TOUCH_DISPATCH,i+4,0,0);
  String snapshot=e.snapshot();
  assertTrue(snapshot.contains("1 MODAL_OPEN 42 0"));
  assertTrue(snapshot.contains("3 MODAL_DISPOSE 42 0"));
  assertTrue(snapshot.contains("TOUCH_DISPATCH_count=1000"));
  assertTrue(snapshot.length()<20000);
 }
}
