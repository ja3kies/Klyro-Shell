package com.oryn.terminal.core;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;
public class StorageFailureTest {
 @Test public void revokedPermissionHasConcreteRecoveryWithoutUri(){String s=KlyroDiagnostics.storageFailure(new SecurityException("content://private/token"));assertTrue(s.contains("Reconnect"));assertFalse(s.contains("content:"));assertFalse(s.contains("token"));}
 @Test public void unavailableDocumentDoesNotRevealPath(){String s=KlyroDiagnostics.storageFailure(new FileNotFoundException("/data/data/private/secret"));assertTrue(s.contains("Choose it again"));assertFalse(s.contains("secret"));}
 @Test public void noSpaceIsActionableWithoutEchoingProviderMessage(){String s=KlyroDiagnostics.storageFailure(new IOException("write failed: ENOSPC content://secret"));assertTrue(s.contains("Free space"));assertFalse(s.contains("secret"));}
 @Test public void sizeLimitIsExplicit(){assertTrue(KlyroDiagnostics.storageFailure(new IOException("File exceeds 32 MiB limit")).contains("32 MiB"));}
 @Test public void interruptedTransferDoesNotPromiseSuccess(){assertTrue(KlyroDiagnostics.storageFailure(new InterruptedIOException("secret")).contains("interrupted"));}
 @Test public void providerErrorDoesNotExposeCredentials(){String s=KlyroDiagnostics.storageFailure(new IOException("https://user:password@provider/path?token=secret"));assertFalse(s.contains("password"));assertFalse(s.contains("secret"));assertTrue(s.contains("retry"));}
 @Test public void unexpectedFailureRemainsVisibleByType(){String s=KlyroDiagnostics.storageFailure(new IllegalStateException("private"));assertTrue(s.contains("IllegalStateException"));assertFalse(s.contains("private"));}
 @Test public void importSeparatesEmptyPartialAndRevokedFolders(){
    assertTrue(KlyroDiagnostics.folderImportSummary(0,0,0,1,null).contains("No files found"));
    assertTrue(KlyroDiagnostics.folderImportSummary(0,0,1,0,null).contains("Reconnect"));
    String partial=KlyroDiagnostics.folderImportSummary(30,2,1,1,"File unavailable.");
    for(String expected:new String[]{"imports: 30","Files skipped: 2","incomplete: 1","limit of 30","File unavailable."})
        assertTrue(partial.contains(expected));
 }
 @Test public void exportFieldShowsActionableErrorsWithoutEchoingPrivatePaths(){
    assertEquals("Choose a relative path inside HOME",KlyroDiagnostics.exportPathFailure(new IOException("Choose a relative path inside HOME")));
    assertFalse(KlyroDiagnostics.exportPathFailure(new IOException("/private/token")).contains("token"));
 }
 @Test public void nullFailureIsExplicitlyUnknown(){assertTrue(KlyroDiagnostics.storageFailure(null).contains("unknown"));}
}
