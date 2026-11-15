package com.oryn.terminal.diagnostics;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
public class FreezeReportStoreTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 FreezeReportStore store()throws Exception{return new FreezeReportStore(temp.getRoot());}
 @Test public void restartRetainsReport()throws Exception{store().write("100-a","state=UI_FREEZE_DETECTED");assertTrue(store().read("100-a").contains("UI_FREEZE"));}
 @Test public void interruptedTemporaryWriteIsNotAnIncident()throws Exception{Files.write(temp.getRoot().toPath().resolve(".pending-a.tmp"),new byte[10]);assertTrue(store().list().isEmpty());}
 @Test public void malformedCommittedFileIsNotPresented()throws Exception{Files.write(temp.getRoot().toPath().resolve("a.report"),"bad".getBytes());assertTrue(store().list().isEmpty());}
 @Test public void storageRotatesOldestFirst()throws Exception{FreezeReportStore s=store();for(int i=0;i<15;i++)s.write(String.format("%013d-a",i),"x");assertEquals(10,s.list().size());assertTrue(s.list().get(0).startsWith("0000000000005"));}
 @Test public void overLimitBodyIsExplicitlyTruncated()throws Exception{FreezeReportStore s=store();s.write("x","界".repeat(300000));assertTrue(s.read("x").contains("TRUNCATED"));assertTrue(Files.size(temp.getRoot().toPath().resolve("x.report"))<=FreezeReportStore.MAX_BYTES);}
 @Test public void truncationDoesNotBreakUtf8()throws Exception{FreezeReportStore s=store();s.write("x","😀".repeat(300000));assertFalse(s.read("x").contains("\ufffd"));}
 @Test public void secondSnapshotUpdatesSameIncident()throws Exception{FreezeReportStore s=store();s.write("x","one");s.write("x","two");assertEquals(1,s.list().size());assertTrue(s.read("x").endsWith("two"));}
 @Test public void recoveryIsDurable()throws Exception{store().write("x","state=UI_RECOVERED duration_ms=6000");assertTrue(store().read("x").contains("6000"));}
 @Test public void deleteOnlyChosenReport()throws Exception{FreezeReportStore s=store();s.write("a","a");s.write("b","b");s.delete("a");assertEquals(Arrays.asList("b"),s.list());}
 @Test public void traversalIsRejected()throws Exception{try{store().write("../outside","bad");fail();}catch(IllegalArgumentException expected){}}
 @Test public void symlinkReportsAreNotFollowed()throws Exception{Path external=Files.createTempFile(temp.getRoot().toPath(),"user-",".txt");Files.write(external,"secret".getBytes());Files.createSymbolicLink(temp.getRoot().toPath().resolve("x.report"),external);assertTrue(store().list().isEmpty());}
 @Test public void exportContainsReportsOnly()throws Exception{FreezeReportStore s=store();s.write("a","safe");Files.write(temp.getRoot().toPath().resolve("user-file"),"secret".getBytes());ByteArrayOutputStream b=new ByteArrayOutputStream();s.exportZip(b);try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(b.toByteArray()))){assertEquals("a.txt",z.getNextEntry().getName());assertTrue(new String(z.readAllBytes()).endsWith("safe"));assertNull(z.getNextEntry());}}
 @Test public void missingSystemTraceIsExplicit()throws Exception{assertTrue(FreezeReportStore.traceFrames(null).contains("UNAVAILABLE"));}
 @Test public void systemTraceExcludesHeadersSecretsAndNativeArguments()throws Exception{String trace="password=secret\nCmd line: bash -c token\n at com.klyro.Main.run(Main.java:42)\n native args secret\n";String safe=FreezeReportStore.traceFrames(new ByteArrayInputStream(trace.getBytes()));assertTrue(safe.contains("Main.run"));assertFalse(safe.contains("secret"));assertFalse(safe.contains("token"));}
 @Test public void stalledTraceStreamFailsWithoutSpin()throws Exception{try{FreezeReportStore.traceFrames(new InputStream(){public int read(){return 0;}public int read(byte[]b,int o,int n){return 0;}});fail();}catch(IOException expected){}}
}
