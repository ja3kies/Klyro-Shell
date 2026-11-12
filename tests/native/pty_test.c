#define _GNU_SOURCE
#include <assert.h>
#include <poll.h>
#include <sys/time.h>
#include "../../terminal-emulator/src/main/jni/termux.c"

#ifndef KLYRO_TEST_SHELL
#define KLYRO_TEST_SHELL "/bin/sh"
#endif

static void interrupted(int signo) { (void) signo; }

static int spawn(const char *script, int *pid) {
    char *argv[] = {KLYRO_TEST_SHELL, "-c", (char *) script, NULL};
    char *env[] = {"PATH=/system/bin:/usr/bin:/bin", "TERM=xterm-256color", NULL};
    return create_subprocess(NULL, argv[0], "/", argv, env, pid, 24, 80);
}

int main(void) {
    for (int i = 0; i < 20; i++) {
        int pid;
        int fd = spawn("test -t 0 && test -t 1 && test -t 2 && printf 'PTY-OK'", &pid);
        assert(fd >= 0 && pid > 0);
        struct pollfd ready = {.fd = fd, .events = POLLIN};
        assert(poll(&ready, 1, 3000) > 0);
        char text[128] = {0};
        assert(read(fd, text, sizeof(text) - 1) > 0);
        assert(strstr(text, "PTY-OK") != NULL);
        assert(Java_com_termux_terminal_JNI_waitFor(NULL, NULL, pid) == 0);
        close(fd);
    }
    int pid;
    int fd = spawn("sleep 1; exit 37", &pid);
    struct sigaction action = {.sa_handler = interrupted};
    sigemptyset(&action.sa_mask);
    sigaction(SIGALRM, &action, NULL);
    struct itimerval timer = {.it_value = {.tv_usec = 10000}};
    setitimer(ITIMER_REAL, &timer, NULL);
    assert(Java_com_termux_terminal_JNI_waitFor(NULL, NULL, pid) == 37);
    close(fd);

    fd = spawn("sleep 30", &pid);
    assert(kill(-pid, SIGTERM) == 0 || kill(pid, SIGTERM) == 0);
    assert(Java_com_termux_terminal_JNI_waitFor(NULL, NULL, pid) == -SIGTERM);
    close(fd);
    puts("PASS: native PTY x20, tty stdin/out/err, exit status, EINTR wait and signal");
    return 0;
}
