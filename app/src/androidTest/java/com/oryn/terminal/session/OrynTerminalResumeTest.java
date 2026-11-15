package com.oryn.terminal.session;

import android.content.Context;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.oryn.terminal.ui.OrynTerminalViewClient;
import com.termux.terminal.TerminalSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/**
 * Runs in Klyro's Android UID. Tests UI resume/backpressure without relying
 * on an installed Termux package or launching the PRoot runtime.
 */
@RunWith(AndroidJUnit4.class)
public class OrynTerminalResumeTest {

    private static OrynTerminalSessionClient newClient(AtomicInteger observed) {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new OrynTerminalSessionClient(app, new OrynTerminalSessionClient.Listener() {
            @Override public void onTitleChanged(String title) { }
            @Override public void onSessionFinished(TerminalSession session, int code) { }
            @Override public void onShellPid(int pid) { }
            @Override public void onNativePasteRequiresConfirmation(String text) { }
            @Override public void onOutputActivity() { observed.incrementAndGet(); }
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Test public void backgroundOutputIsCoalescedToOneForegroundRefresh() {
        AtomicInteger updates = new AtomicInteger();
        OrynTerminalSessionClient client = newClient(updates);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                client.setUiActive(false);
                // The callback ignores its session argument; a null avoids
                // spawning any shell process from this UI-only test.
                for (int i = 0; i < 4000; i++) client.onTextChanged(null);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("No view refresh while Activity is paused", 0, updates.get());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    client.setUiActive(true));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Entire backlog must become one UI refresh", 1, updates.get());
        } finally {
            client.dispose();
        }
    }

    @Test public void resumeWithoutOutputOrAttachedViewDoesNotWakeUi() {
        AtomicInteger updates = new AtomicInteger();
        OrynTerminalSessionClient client = newClient(updates);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                client.setUiActive(false);
                client.setUiActive(true);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(0, updates.get());
        } finally {
            client.dispose();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test public void disposePreventsDeferredOutputFromBeingDelivered() {
        AtomicInteger updates = new AtomicInteger();
        OrynTerminalSessionClient client = newClient(updates);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            client.setUiActive(false);
            client.onTextChanged(null);
            client.dispose();
            client.setUiActive(true);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(0, updates.get());
    }

    @Test public void releasedOldViewDoesNotDetachNewerInputView() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        OrynTerminalViewClient client = new OrynTerminalViewClient(app);
        View oldView = new View(app);
        View newView = new View(app);
        client.attachView(oldView);
        client.attachView(newView);
        client.detachViewIfCurrent(oldView);
        Field viewField = OrynTerminalViewClient.class.getDeclaredField("terminalView");
        viewField.setAccessible(true);
        assertSame("An old Compose pane cannot detach a replacement", newView,
                viewField.get(client));
        client.detachViewIfCurrent(newView);
        assertNull(viewField.get(client));
    }
}
