package com.oryn.terminal.diagnostics;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
public class FreezePreviewTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 @Test public void fullReportDoesNotEnterComposeState()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());s.write("x","a".repeat(100000));assertTrue(s.preview("x").length()<8300);assertTrue(s.preview("x").contains("PREVIEW TRUNCATED"));assertTrue(s.read("x").length()>100000);}
 @Test public void smallReportIsUnchanged()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());s.write("x","state=UI_RECOVERED");assertEquals(s.read("x"),s.preview("x"));}
 @Test public void previewDoesNotSplitSurrogate()throws Exception{FreezeReportStore s=new FreezeReportStore(temp.getRoot());s.write("x","a".repeat(8176)+"😀".repeat(1000));String p=s.preview("x");int cut=p.indexOf("\n[PREVIEW");assertFalse(Character.isHighSurrogate(p.charAt(cut-1)));}
}
