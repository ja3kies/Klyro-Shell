package com.oryn.terminal.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oryn.terminal.TerminalSessionViewModel
import com.oryn.terminal.diagnostics.FreezeRecorder
import com.oryn.terminal.ui.OrynTerminalViewClient
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real model/listener/renderer objects, with no JNI process launch or user-session mutation. */
@RunWith(AndroidJUnit4::class)
class FinishedSessionRetentionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun withModel(test: (TerminalSessionViewModel) -> Unit) {
        instrumentation.runOnMainSync {
            val active = FreezeRecorder.activeSession
            val count = FreezeRecorder.sessionCount
            val terminal = FreezeRecorder.terminal
            val model = TerminalSessionViewModel()
            try { test(model) } finally {
                model.closeAllSessions()
                FreezeRecorder.activeSession = active
                FreezeRecorder.sessionCount = count
                FreezeRecorder.terminal = terminal
            }
        }
    }

    private fun attach(model: TerminalSessionViewModel): Pair<Long, TerminalSession> {
        val context = instrumentation.targetContext
        val id = model.reserveTabId()
        val client = OrynTerminalSessionClient(context, model.listenerFor(id))
        val session = TerminalSession("/system/bin/sh", context.filesDir.absolutePath,
            arrayOf("sh"), emptyArray(), 100, client)
        val emulator = TerminalEmulator(session, 80, 24, 100, client)
        val field = TerminalSession::class.java.getDeclaredField("mEmulator")
        field.isAccessible = true
        field.set(session, emulator)
        val marker = "retained-fixture-$id".toByteArray(Charsets.UTF_8)
        emulator.append(marker, marker.size)
        model.attachSession(session, client, OrynTerminalViewClient(context), id)
        return id to session
    }

    @Test fun unexpectedExitRetainsIdentityTranscriptAndExitCode() = withModel { model ->
        val (id, session) = attach(model)
        val transcript = session.emulator.screen.transcriptText
        assertTrue(model.finishedTabIds.isEmpty())
        assertEquals(1, model.liveSessionCount)
        model.listenerFor(id).onSessionFinished(session, 17)
        assertSame(session, model.activeSession)
        assertEquals(id, model.activeTabId)
        assertEquals(17, model.sessions.single().exitStatus)
        assertEquals(transcript, session.emulator.screen.transcriptText)
        assertFalse(model.hasLiveSessions())
        assertEquals(setOf(id), model.finishedTabIds)
        assertFalse(session.tryWrite(byteArrayOf(65), 0, 1))
        assertTrue(model.sessionFailureMessage!!.contains("17"))
    }

    @Test fun successfulExitAlsoKeepsOutputUntilExplicitClose() = withModel { model ->
        val (id, session) = attach(model)
        model.listenerFor(id).onSessionFinished(session, 0)
        assertSame(session, model.activeSession)
        assertNull(model.sessionFailureMessage)
        assertEquals(0, model.liveSessionCount)
        model.closeTab(id)
        assertTrue(model.sessions.isEmpty())
        assertTrue(model.finishedTabIds.isEmpty())
        assertNull(model.activeTabId)
    }

    @Test fun backgroundExitDoesNotSwitchOrRemoveTheActiveSibling() = withModel { model ->
        val (endedId, ended) = attach(model)
        val (liveId, live) = attach(model)
        model.listenerFor(endedId).onSessionFinished(ended, 9)
        assertSame(live, model.activeSession)
        assertEquals(liveId, model.activeTabId)
        assertEquals(2, model.sessions.size)
        assertEquals(setOf(endedId), model.finishedTabIds)
        assertEquals(1, model.liveSessionCount)
        assertTrue(model.hasLiveSessions())
        assertTrue(endedId in model.tabsWithUnreadOutput)
        model.selectTab(endedId)
        assertSame(ended, model.activeSession)
        assertTrue(model.sessionFailureMessage!!.contains("9"))
    }

    @Test fun duplicateAndClosedSessionCallbacksCannotChangeAnotherTab() = withModel { model ->
        val (oldId, old) = attach(model)
        model.listenerFor(oldId).onSessionFinished(old, 7)
        model.listenerFor(oldId).onSessionFinished(old, 99)
        assertEquals(7, model.sessions.single().exitStatus)
        model.closeTab(oldId)
        val (newId, newSession) = attach(model)
        model.listenerFor(oldId).onSessionFinished(old, 33)
        assertEquals(newId, model.activeTabId)
        assertSame(newSession, model.activeSession)
        assertNull(model.sessions.single().exitStatus)
    }

    @Test fun finishedTranscriptCanResizeWithoutCallingNativePty() = withModel { model ->
        val (id, session) = attach(model)
        model.listenerFor(id).onSessionFinished(session, 0)
        session.updateSize(40, 12)
        assertEquals(40, session.emulator.mColumns)
        assertEquals(12, session.emulator.mRows)
        assertTrue(session.emulator.screen.transcriptText.contains("retained-fixture-$id"))
    }
}
