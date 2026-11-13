#define _GNU_SOURCE
#include <assert.h>
#include <poll.h>
#include <sys/time.h>
#include <time.h>
#include <sys/resource.h>
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

static int fd_count(void) {
    DIR *directory = opendir("/proc/self/fd");
    assert(directory);
    int count = 0;
    struct dirent *entry;
    while ((entry = readdir(directory))) if (entry->d_name[0] != '.') count++;
    closedir(directory);
    return count;
}

int main(void) {
    int descriptors_before = fd_count();
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
    signal(SIGHUP, SIG_IGN);
    fd = spawn("printf READY; exec sleep 30", &pid);
    struct pollfd ready = {.fd = fd, .events = POLLIN};
    assert(poll(&ready, 1, 3000) > 0);
    char text[128];
    assert(read(fd, text, sizeof(text)) > 0);
    assert(kill(-pid, SIGHUP) == 0);
    int status = 0;
    pid_t exited = 0;
    for (int i = 0; i < 100 && !exited; i++) {
        exited = waitpid(pid, &status, WNOHANG);
        if (!exited) usleep(10000);
    }
    if (!exited) { kill(-pid, SIGKILL); waitpid(pid, &status, 0); }
    close(fd);
    signal(SIGHUP, SIG_DFL);
    fprintf(stderr, "HUP_RESULT exited=%d expected=%d status=%d signaled=%d signal=%d exit=%d\n", (int)exited, pid, status, WIFSIGNALED(status), WIFSIGNALED(status)?WTERMSIG(status):0, WIFEXITED(status)?WEXITSTATUS(status):-1);
    assert(exited == pid && ((WIFSIGNALED(status) && WTERMSIG(status) == SIGHUP) ||
                            (WIFEXITED(status) && WEXITSTATUS(status) == 128 + SIGHUP)));

    struct timespec before, after;
    struct rusage usage;
    clock_gettime(CLOCK_MONOTONIC, &before);
    fd = spawn("seq 1 20000", &pid);
    long bytes = 0, lines = 0;
    char chunk[4096];
    for (;;) {
        ssize_t count = read(fd, chunk, sizeof(chunk));
        if (count < 0 && errno == EINTR) continue;
        if (count < 0 && errno == EIO) break;
        assert(count >= 0);
        if (!count) break;
        bytes += count;
        for (ssize_t i = 0; i < count; i++) if (chunk[i] == '\n') lines++;
    }
    assert(Java_com_termux_terminal_JNI_waitFor(NULL, NULL, pid) == 0);
    close(fd);
    assert(lines == 20000);
    clock_gettime(CLOCK_MONOTONIC, &after);
    getrusage(RUSAGE_SELF, &usage);
    double elapsed = (after.tv_sec-before.tv_sec) + (after.tv_nsec-before.tv_nsec)/1e9;
    printf("MEASURE native_stdout_lines=%ld bytes=%ld elapsed_s=%.6f maxrss_kb=%ld\n", lines, bytes, elapsed, usage.ru_maxrss);
    int descriptors_after = fd_count();
    assert(descriptors_after == descriptors_before);
    printf("MEASURE fd_before=%d fd_after=%d\n", descriptors_before, descriptors_after);
    puts("PASS: native PTY x20, tty stdin/out/err, exit status, EINTR wait and signal");
    return 0;
}
