package com.oryn.terminal.termux;

import com.oryn.terminal.core.OrynPaths;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class BootstrapAdversarialTest {
    @Rule public TemporaryFolder tmp=new TemporaryFolder();
    File root() throws Exception{return tmp.newFolder();}
    byte[] zip(String name) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(bytes)){z.putNextEntry(new ZipEntry(name));byte[] data=new byte[1024];new java.util.Random(1).nextBytes(data);z.write(data);z.closeEntry();}
        return bytes.toByteArray();
    }
    void extract(byte[] bytes,File destination)throws Exception {
        File archive=tmp.newFile();Files.write(archive.toPath(),bytes);
        Method m=TermuxBootstrapInstaller.class.getDeclaredMethod("extractArchive",File.class,File.class);m.setAccessible(true);
        try{m.invoke(new TermuxBootstrapInstaller(new OrynPaths(root())),archive,destination);}catch(InvocationTargetException e){throw (Exception)e.getCause();}
    }
    interface Work {void run()throws Exception;}
    void refused(Work work)throws Exception {try{work.run();fail("Unsafe operation accepted");}catch(IOException expected){}}
    void badZip(String name)throws Exception {File destination=root();refused(()->extract(zip(name),destination));assertEquals(0,destination.list().length);}
    @Test public void absoluteArchivePath()throws Exception{badZip("/outside");}
    @Test public void parentTraversalArchivePath()throws Exception{badZip("x/../../outside");}
    @Test public void windowsAbsoluteArchivePath()throws Exception{badZip("C:/outside");}
    @Test public void backslashTraversalArchivePath()throws Exception{badZip("x\\..\\..\\outside");}
    @Test public void nulArchiveName()throws Exception{badZip("invalid\0name");}
    @Test public void existingSymlinkParentCannotEscapeExtraction()throws Exception {File dest=root(),outside=root();Files.createSymbolicLink(dest.toPath().resolve("alias"),outside.toPath());refused(()->extract(zip("alias/secret"),dest));assertEquals(0,outside.list().length);}
    @Test public void interruptedArchiveFailsWithoutActivatingPrefix()throws Exception {byte[] archive=zip("bin/bash");File dest=root();refused(()->extract(java.util.Arrays.copyOf(archive,120),dest));assertFalse(new File(dest,".oryn-bootstrap-version").exists());}
    @Test public void unicodeExecutableRetainsNameAndMode()throws Exception {File dest=root();extract(zip("bin/café 😀"),dest);assertTrue(new File(dest,"bin/café 😀").canExecute());}
    @Test public void ordinaryDataDoesNotBecomeExecutable()throws Exception {File dest=root();extract(zip("etc/config"),dest);assertFalse(new File(dest,"etc/config").canExecute());}
    @Test public void stageFileIsRejectedBeforeMovingUserData()throws Exception {File prefix=root(),stage=tmp.newFile(),previous=new File(tmp.getRoot(),"backup");refused(()->TermuxBootstrapInstaller.activatePrefix(stage,prefix,previous));assertTrue(prefix.isDirectory());assertFalse(previous.exists());}
    @Test public void linkedStagingCannotBeActivated()throws Exception {File stage=new File(tmp.getRoot(),"stage");Files.createSymbolicLink(stage.toPath(),root().toPath());File prefix=root();refused(()->TermuxBootstrapInstaller.activatePrefix(stage,prefix,new File(tmp.getRoot(),"backup")));assertTrue(prefix.isDirectory());}
    @Test public void linkedPrefixCannotBeReplaced()throws Exception {File prefix=new File(tmp.getRoot(),"prefix"),outside=root();Files.createSymbolicLink(prefix.toPath(),outside.toPath());refused(()->TermuxBootstrapInstaller.activatePrefix(root(),prefix,new File(tmp.getRoot(),"backup")));assertTrue(Files.isSymbolicLink(prefix.toPath()));}
    @Test public void danglingBackupCannotBeUsedForRecovery()throws Exception {File backup=new File(tmp.getRoot(),"backup");Files.createSymbolicLink(backup.toPath(),tmp.getRoot().toPath().resolve("absent"));refused(()->TermuxBootstrapInstaller.recoverPrefix(new File(tmp.getRoot(),"prefix"),backup));assertTrue(Files.isSymbolicLink(backup.toPath()));}
    @Test public void previousBackupIsNeverOverwritten()throws Exception {File prefix=root(),backup=root();Files.write(new File(backup,"project").toPath(),new byte[]{42});refused(()->TermuxBootstrapInstaller.activatePrefix(root(),prefix,backup));assertArrayEquals(new byte[]{42},Files.readAllBytes(new File(backup,"project").toPath()));assertTrue(prefix.exists());}
    @Test public void missingStagingLeavesOriginalAndNoBackupRename()throws Exception {File prefix=root(),backup=new File(tmp.getRoot(),"backup");Files.write(new File(prefix,"project").toPath(),new byte[]{7});refused(()->TermuxBootstrapInstaller.activatePrefix(new File(tmp.getRoot(),"absent"),prefix,backup));assertFalse(backup.exists());assertArrayEquals(new byte[]{7},Files.readAllBytes(new File(prefix,"project").toPath()));}
}
