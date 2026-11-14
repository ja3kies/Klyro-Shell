package com.oryn.terminal.ui;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ModalDismissalTest {
    @Test public void externalDismissClosesOwnerExactlyOnce() {
        ModalDismissal sheet = new ModalDismissal(); AtomicInteger closes = new AtomicInteger();
        sheet.dismiss(true, true, closes::incrementAndGet); sheet.dismiss(true, true, closes::incrementAndGet);
        assertEquals(1, closes.get());
    }
    @Test public void protectionArrivingAfterHiddenCannotStrandTheWindow() {
        ModalDismissal sheet = new ModalDismissal(); AtomicInteger closes = new AtomicInteger();
        assertTrue(sheet.canDismiss(true));
        sheet.dismiss(false, true, closes::incrementAndGet);
        assertEquals(1, closes.get());
    }
    @Test public void protectedDraftCannotReachHidden() {
        ModalDismissal sheet = new ModalDismissal(); assertFalse(sheet.canDismiss(false));
        sheet.dismiss(false, false, () -> fail("draft lost")); assertTrue(sheet.canDismiss(true));
    }
    @Test public void cancelProtectionAllowsSubsequentDismiss() {
        ModalDismissal sheet = new ModalDismissal(); AtomicInteger closes = new AtomicInteger();
        sheet.dismiss(false, false, closes::incrementAndGet); sheet.dismiss(true, true, closes::incrementAndGet);
        assertEquals(1, closes.get());
    }
    @Test public void lateCompletionAfterDisposeCannotReopenParent() {
        ModalDismissal sheet = new ModalDismissal(); sheet.dispose();
        sheet.dismiss(true, true, () -> fail("stale callback")); assertFalse(sheet.canDismiss(true));
    }
    @Test public void reentrantDismissDoesNotDispatchTwice() {
        ModalDismissal sheet = new ModalDismissal(); AtomicInteger closes = new AtomicInteger();
        sheet.dismiss(true, true, () -> { closes.incrementAndGet(); sheet.dismiss(true, true, closes::incrementAndGet); });
        assertEquals(1, closes.get());
    }
    @Test public void throwingOwnerDoesNotReplayAnAction() {
        ModalDismissal sheet = new ModalDismissal();
        try { sheet.dismiss(true, true, () -> { throw new IllegalStateException("test"); }); fail(); }
        catch (IllegalStateException expected) { }
        sheet.dismiss(true, true, () -> fail("replayed"));
    }
    @Test public void newSheetHasIndependentLifetime() {
        ModalDismissal old = new ModalDismissal(); old.dispose();
        ModalDismissal next = new ModalDismissal(); assertTrue(next.canDismiss(true));
    }
    @Test public void interruptedGestureDoesNotConsumeDismissal() {
        ModalDismissal sheet = new ModalDismissal();
        assertTrue(sheet.canDismiss(true)); assertTrue(sheet.canDismiss(true));
        AtomicInteger closes = new AtomicInteger(); sheet.dismiss(true, true, closes::incrementAndGet);
        assertEquals(1, closes.get());
    }
}
