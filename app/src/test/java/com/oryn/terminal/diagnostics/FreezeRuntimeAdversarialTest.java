package com.oryn.terminal.diagnostics;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class FreezeRuntimeAdversarialTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 @Test(timeout=5000) public void stalledExportCannotBlockNewFreezePersistence()throws Exception{
  FreezeReportStore s=new FreezeReportStore(temp.getRoot());s.write("a","snapshot");
  CountDownLatch writing=new CountDownLatch(1),release=new CountDownLatch(1);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try{Future<?> export=pool.submit(()->{try{s.exportZip(new OutputStream(){public void write(int b)throws IOException{writing.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new IOException("test export timeout");}catch(InterruptedException e){throw new IOException(e);}}});}catch(IOException e){throw new RuntimeException(e);}});
   assertTrue(writing.await(1,TimeUnit.SECONDS));
   pool.submit(()->{try{s.write("b","freeze during export");}catch(IOException e){throw new RuntimeException(e);}}).get(1,TimeUnit.SECONDS);
   assertTrue(s.list().contains("b"));release.countDown();export.get();
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test public void failedAtomicCommitPreservesOtherReports()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());s.write("safe","original");Files.createDirectory(temp.getRoot().toPath().resolve("blocked.report"));try{s.write("blocked","new");fail();}catch(IOException expected){}assertTrue(s.read("safe").endsWith("original"));assertEquals(1,s.list().size());}
 @Test public void failedWriteDoesNotLeakPendingFiles()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());Files.createDirectory(temp.getRoot().toPath().resolve("blocked.report"));try{s.write("blocked","x");fail();}catch(IOException expected){}try(java.util.stream.Stream<Path> files=Files.list(temp.getRoot().toPath())){assertFalse(files.anyMatch(p->p.getFileName().toString().startsWith(".pending-")));}}
 @Test public void oversizedCommittedReportIsNeverLoaded()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());Files.write(temp.getRoot().toPath().resolve("x.report"),new byte[FreezeReportStore.MAX_BYTES+1]);assertTrue(s.list().isEmpty());}
 @Test public void unterminatedSystemTraceReadIsBounded()throws Exception{final int[] count={0};InputStream source=new InputStream(){public int read(){count[0]++;return 'a';}};FreezeReportStore.traceFrames(source);assertEquals(65536,count[0]);}
 @Test public void traceReadFailureIsNotAValidEmptyTrace()throws Exception{try{FreezeReportStore.traceFrames(new InputStream(){public int read()throws IOException{throw new IOException("unavailable");}});fail();}catch(IOException expected){}}
 @Test(timeout=5000) public void concurrentIncidentUpdatesRemainWhole()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());ExecutorService pool=Executors.newFixedThreadPool(2);try{Future<?>a=pool.submit(()->{try{s.write("x","a".repeat(50000));}catch(IOException e){throw new RuntimeException(e);}}),b=pool.submit(()->{try{s.write("x","b".repeat(50000));}catch(IOException e){throw new RuntimeException(e);}});a.get();b.get();String body=s.read("x").substring("KLYRO_FREEZE_V1\n".length());assertTrue(body.equals("a".repeat(50000))||body.equals("b".repeat(50000)));}finally{pool.shutdownNow();}}
}
