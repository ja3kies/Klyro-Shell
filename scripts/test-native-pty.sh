#!/bin/sh
# Exercises the production JNI on a Linux/Android build host, not the Android UI.
set -eu
: "${JAVA_HOME:?Point JAVA_HOME at a JDK with JNI headers}"
cd "$(dirname "$0")/.."
pty_test_dir=$(mktemp -d)
trap 'rm -rf "$pty_test_dir"' EXIT HUP INT TERM
pty_test_shell=/bin/sh
if [ -x /system/bin/sh ]; then pty_test_shell=/system/bin/sh; fi
"${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
    -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
    "-DKLYRO_TEST_SHELL=\"$pty_test_shell\"" tests/native/pty_test.c -o "$pty_test_dir/pty-test"
"$pty_test_dir/pty-test"
"${CC:-cc}" -shared -fPIC -std=c11 -D_GNU_SOURCE -Wall -Wextra -Werror \
    -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/linux" \
    terminal-emulator/src/main/jni/termux.c -o "$pty_test_dir/libtermux.so"
javac -d "$pty_test_dir" terminal-emulator/src/main/java/com/termux/terminal/JNI.java \
    tests/native/JNIStringTest.java
java -Djava.library.path="$pty_test_dir" -cp "$pty_test_dir" com.termux.terminal.JNIStringTest "$pty_test_shell"
