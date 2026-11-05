package com.oryn.terminal.termux;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;
public class TermuxInstallerBoundariesTest {
  @Test public void deadline() throws Exception {
    TermuxBootstrapInstaller.ensureWorkAllowed(System.nanoTime(), TimeUnit.MINUTES.toNanos(4));
    try {
      TermuxBootstrapInstaller.ensureWorkAllowed(System.nanoTime()-TimeUnit.SECONDS.toNanos(2),TimeUnit.SECONDS.toNanos(1));
      fail("timeout expected");
    } catch (IOException expected) { assertTrue(expected.getMessage().contains("time limit")); }
  }
  @Test public void interruption() throws Exception {
    Thread.currentThread().interrupt();
    try {
      TermuxBootstrapInstaller.ensureWorkAllowed(System.nanoTime(),TimeUnit.MINUTES.toNanos(4));
      fail("interrupt expected");
    } catch (IOException expected) { assertTrue(Thread.currentThread().isInterrupted()); }
    finally { Thread.interrupted(); }
  }
  @Test public void allowedManifestPaths() {
    assertTrue(ProotRuntimeInstaller.isAllowedAssetPath("bin/proot"));
    assertTrue(ProotRuntimeInstaller.isAllowedAssetPath("loader"));
    assertTrue(ProotRuntimeInstaller.isAllowedAssetPath("lib/libtalloc.so"));
    assertFalse(ProotRuntimeInstaller.isAllowedAssetPath("lib/../../bad"));
    assertFalse(ProotRuntimeInstaller.isAllowedAssetPath("/data/data/secret"));
    assertFalse(ProotRuntimeInstaller.isAllowedAssetPath("lib/name:bad"));
  }
}
