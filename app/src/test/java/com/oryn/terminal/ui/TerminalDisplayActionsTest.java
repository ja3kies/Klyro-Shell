package com.oryn.terminal.ui;

import com.termux.terminal.*;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Exercises the actual emulator; no inferred Readline state or source-text assertions. */
public class TerminalDisplayActionsTest {
    static class Output extends TerminalOutput {
        final ByteArrayOutputStream input=new ByteArrayOutputStream(); String title;
        public void write(byte[] b,int off,int len){input.write(b,off,len);}
        public void titleChanged(String old,String next){title=next;}
        public void onCopyTextToClipboard(String text){} public void onPasteTextFromClipboard(){}
        public void onBell(){} public void onColorsChanged(){}
    }
    final Output output=new Output();
    final TerminalEmulator e=new TerminalEmulator(output,40,5,100,null);
    void feed(String text){byte[] b=text.getBytes(StandardCharsets.UTF_8);e.append(b,b.length);}
    String screen(){return e.getScreen().getSelectedText(0,0,39,4,false,false);}
    void preserves(String text){feed(text);String before=screen();int row=e.getCursorRow(),col=e.getCursorCol();TerminalDisplayActions.clearEmulator(e);assertEquals(before,screen());assertEquals(row,e.getCursorRow());assertEquals(col,e.getCursorCol());assertEquals(0,output.input.size());}
    @Test public void missingSessionIsUnavailable(){assertFalse(TerminalDisplayActions.clear(null));}
    @Test public void missingEmulatorIsUnavailable(){assertEquals(TerminalDisplayActions.ClearResult.UNAVAILABLE,TerminalDisplayActions.clearEmulator(null));}
    @Test public void emptyPromptRetainsItsCursor(){preserves("klyro ~ $ ");}
    @Test public void partialCommandNeverDisappears(){preserves("klyro ~ $ git status");}
    @Test public void multilineInputIsNotReconstructed(){preserves("klyro ~ $ echo 'first\r\n> second");}
    @Test public void cursorInsideEditBufferIsRetained(){preserves("klyro ~ $ command\033[5D");}
    @Test public void wrappedLongCommandKeepsAllRows(){preserves("klyro ~ $ "+"x".repeat(170));}
    @Test public void programOutputThatImitatesPromptCannotTriggerReplay(){preserves("private output\r\nklyro ~ $ ");}
    @Test public void alternateScreenIsRefused(){feed("\033[?1049hprogram");String before=screen();assertEquals(TerminalDisplayActions.ClearResult.UNAVAILABLE,TerminalDisplayActions.clearEmulator(e));assertEquals(before,screen());}
    @Test public void pendingCsiRemainsTheProgramsSequence(){feed("\033[38;2;");TerminalDisplayActions.clearEmulator(e);feed("1;2;3mX");assertEquals(0xff010203,TextStyle.decodeForeColor(e.getScreen().getStyleAt(0,0)));}
    @Test public void pendingOscTitleSurvives(){feed("\033]2;part");TerminalDisplayActions.clearEmulator(e);feed("ial\007");assertEquals("partial",output.title);}
    @Test public void splitUtf8Survives(){byte[] b="😀".getBytes(StandardCharsets.UTF_8);e.append(new byte[]{b[0],b[1]},2);TerminalDisplayActions.clearEmulator(e);e.append(new byte[]{b[2],b[3]},2);assertTrue(screen().startsWith("😀"));}
    @Test public void bracketedPasteModeAndStdinAreUntouched(){feed("\033[?2004h");TerminalDisplayActions.clearEmulator(e);e.paste("hello");assertEquals("\033[200~hello\033[201~",new String(output.input.toByteArray(),StandardCharsets.UTF_8));}
    @Test public void saturatedHistoryPurgesOnlyHistory(){for(int i=0;i<300;i++)feed("line\r\n");assertTrue(e.getScreen().getActiveTranscriptRows()>0);String before=screen();TerminalDisplayActions.clearEmulator(e);assertEquals(0,e.getScreen().getActiveTranscriptRows());assertEquals(before,screen());}
    @Test public void clearIsIsolatedAndResizeStillWorks(){TerminalEmulator other=new TerminalEmulator(new Output(),40,5,100,null);byte[] b="other".getBytes(StandardCharsets.UTF_8);other.append(b,b.length);TerminalDisplayActions.clearEmulator(e);e.resize(20,8);assertEquals(20,e.mColumns);assertEquals("other",other.getScreen().getTranscriptText());}
}
