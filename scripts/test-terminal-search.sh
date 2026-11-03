#!/bin/sh
# Run the real buffer/search regression tests without an Android SDK.
set -eu
: "${JUNIT_JAR:?Set JUNIT_JAR to junit-4.13.2.jar}"
: "${HAMCREST_JAR:?Set HAMCREST_JAR to hamcrest-core-1.3.jar}"
cd "$(dirname "$0")/.."
search_test_dir=$(mktemp -d)
trap 'rm -rf "$search_test_dir"' EXIT HUP INT TERM
javac -cp "$JUNIT_JAR" -d "$search_test_dir" \
    terminal-emulator/src/main/java/com/termux/terminal/TerminalBuffer.java \
    terminal-emulator/src/main/java/com/termux/terminal/TerminalRow.java \
    terminal-emulator/src/main/java/com/termux/terminal/TextStyle.java \
    terminal-emulator/src/main/java/com/termux/terminal/WcWidth.java \
    app/src/main/java/com/oryn/terminal/ui/TranscriptSearch.java \
    app/src/test/java/com/oryn/terminal/ui/TranscriptSearchTest.java
java -cp "$search_test_dir:$JUNIT_JAR:$HAMCREST_JAR" \
    org.junit.runner.JUnitCore com.oryn.terminal.ui.TranscriptSearchTest
