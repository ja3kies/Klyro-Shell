#include <dirent.h>
#include <fcntl.h>
#include <jni.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#define TERMUX_UNUSED(x) x __attribute__((__unused__))
#ifdef __APPLE__
# define LACKS_PTSNAME_R
#endif

static int throw_runtime_exception(JNIEnv* env, char const* message)
{
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, exClass, message);
    return -1;
}

static int create_subprocess(JNIEnv* env,
        char const* cmd,
        char const* cwd,
        char* const argv[],
        char** envp,
        int* pProcessId,
        jint rows,
        jint columns)
{
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) return throw_runtime_exception(env, "Cannot open /dev/ptmx");

#ifdef LACKS_PTSNAME_R
    char* devname;
#else
    char devname[64];
#endif
    if (grantpt(ptm) || unlockpt(ptm) ||
#ifdef LACKS_PTSNAME_R
            (devname = ptsname(ptm)) == NULL
#else
            ptsname_r(ptm, devname, sizeof(devname))
#endif
       ) {
        return throw_runtime_exception(env, "Cannot grantpt()/unlockpt()/ptsname_r() on /dev/ptmx");
    }

    // Enable UTF-8 mode and disable flow control to prevent Ctrl+S from locking up the display.
    struct termios tios;
    tcgetattr(ptm, &tios);
    tios.c_iflag |= IUTF8;
    tios.c_iflag &= ~(IXON | IXOFF);
    tcsetattr(ptm, TCSANOW, &tios);

    /** Set initial winsize. */
    struct winsize sz = { .ws_row = (unsigned short) rows, .ws_col = (unsigned short) columns };
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) {
        return throw_runtime_exception(env, "Fork failed");
    } else if (pid > 0) {
        *pProcessId = (int) pid;
        return ptm;
    } else {
        // Clear signals which the Android java process may have blocked:
        sigset_t signals_to_unblock;
        sigfillset(&signals_to_unblock);
        sigprocmask(SIG_UNBLOCK, &signals_to_unblock, 0);

        close(ptm);
        setsid();

        int pts = open(devname, O_RDWR);
        if (pts < 0) exit(-1);

        dup2(pts, 0);
        dup2(pts, 1);
        dup2(pts, 2);

        DIR* self_dir = opendir("/proc/self/fd");
        if (self_dir != NULL) {
            int self_dir_fd = dirfd(self_dir);
            struct dirent* entry;
            while ((entry = readdir(self_dir)) != NULL) {
                int fd = atoi(entry->d_name);
                if (fd > 2 && fd != self_dir_fd) close(fd);
            }
            closedir(self_dir);
        }

        clearenv();
        if (envp) for (; *envp; ++envp) putenv(*envp);

        if (chdir(cwd) != 0) {
            char* error_message;
            // No need to free asprintf()-allocated memory since doing execvp() or exit() below.
            if (asprintf(&error_message, "chdir(\"%s\")", cwd) == -1) error_message = "chdir()";
            perror(error_message);
            fflush(stderr);
        }
        execvp(cmd, argv);
        // Show terminal output about failing exec() call:
        char* error_message;
        if (asprintf(&error_message, "exec(\"%s\")", cmd) == -1) error_message = "exec()";
        perror(error_message);
        _exit(1);
    }
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_createSubprocess(
        JNIEnv* env,
        jclass TERMUX_UNUSED(clazz),
        jstring cmd,
        jstring cwd,
        jobjectArray args,
        jobjectArray envVars,
        jintArray processIdArray,
        jint rows,
        jint columns)
{
    if (!cmd || !cwd || !processIdArray || (*env)->GetArrayLength(env, processIdArray) < 1) {
        return throw_runtime_exception(env, "Invalid command, working directory, or process ID array");
    }

    jsize arg_count = args ? (*env)->GetArrayLength(env, args) : 0;
    jsize env_count = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char** argv = (char**) calloc((size_t) arg_count + 1, sizeof(char*));
    char** envp = NULL;
    char const* cmd_cwd = NULL;
    char const* cmd_utf8 = NULL;
    int procId = 0;
    int ptm = -1;
    int result = -1;

    if (!argv) return throw_runtime_exception(env, "Couldn't allocate argv array");

    envp = (char**) calloc((size_t) env_count + 1, sizeof(char*));
    if (!envp) {
        throw_runtime_exception(env, "Couldn't allocate envp array");
        goto cleanup;
    }

    for (jsize i = 0; i < arg_count; ++i) {
        jstring arg_java_string = (jstring) (*env)->GetObjectArrayElement(env, args, i);
        if (!arg_java_string) {
            throw_runtime_exception(env, "Null argument in argv");
            goto cleanup;
        }

        char const* arg_utf8 = (*env)->GetStringUTFChars(env, arg_java_string, NULL);
        if (!arg_utf8) {
            (*env)->DeleteLocalRef(env, arg_java_string);
            goto cleanup;
        }

        argv[i] = strdup(arg_utf8);
        (*env)->ReleaseStringUTFChars(env, arg_java_string, arg_utf8);
        (*env)->DeleteLocalRef(env, arg_java_string);

        if (!argv[i]) {
            throw_runtime_exception(env, "Couldn't allocate argv entry");
            goto cleanup;
        }
    }

    for (jsize i = 0; i < env_count; ++i) {
        jstring env_java_string = (jstring) (*env)->GetObjectArrayElement(env, envVars, i);
        if (!env_java_string) {
            throw_runtime_exception(env, "Null environment entry");
            goto cleanup;
        }

        char const* env_utf8 = (*env)->GetStringUTFChars(env, env_java_string, NULL);
        if (!env_utf8) {
            (*env)->DeleteLocalRef(env, env_java_string);
            goto cleanup;
        }

        envp[i] = strdup(env_utf8);
        (*env)->ReleaseStringUTFChars(env, env_java_string, env_utf8);
        (*env)->DeleteLocalRef(env, env_java_string);

        if (!envp[i]) {
            throw_runtime_exception(env, "Couldn't allocate environment entry");
            goto cleanup;
        }
    }

    cmd_cwd = (*env)->GetStringUTFChars(env, cwd, NULL);
    if (!cmd_cwd) goto cleanup;

    cmd_utf8 = (*env)->GetStringUTFChars(env, cmd, NULL);
    if (!cmd_utf8) goto cleanup;

    ptm = create_subprocess(env, cmd_utf8, cmd_cwd, argv, envp, &procId, rows, columns);

    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
    cmd_utf8 = NULL;
    (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
    cmd_cwd = NULL;

    if (ptm < 0) goto cleanup;

    jint process_id = (jint) procId;
    (*env)->SetIntArrayRegion(env, processIdArray, 0, 1, &process_id);
    if ((*env)->ExceptionCheck(env)) {
        kill(procId, SIGKILL);
        waitpid(procId, NULL, 0);
        close(ptm);
        ptm = -1;
        goto cleanup;
    }

    result = ptm;

cleanup:
    if (cmd_utf8) (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf8);
    if (cmd_cwd) (*env)->ReleaseStringUTFChars(env, cwd, cmd_cwd);
    if (argv) {
        for (jsize i = 0; i < arg_count; ++i) free(argv[i]);
        free(argv);
    }
    if (envp) {
        for (jsize i = 0; i < env_count; ++i) free(envp[i]);
        free(envp);
    }

    return result;
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyWindowSize(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint fd, jint rows, jint cols)
{
    struct winsize sz = { .ws_row = (unsigned short) rows, .ws_col = (unsigned short) cols };
    ioctl(fd, TIOCSWINSZ, &sz);
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_setPtyUTF8Mode(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint fd)
{
    struct termios tios;
    tcgetattr(fd, &tios);
    if ((tios.c_iflag & IUTF8) == 0) {
        tios.c_iflag |= IUTF8;
        tcsetattr(fd, TCSANOW, &tios);
    }
}

JNIEXPORT jint JNICALL Java_com_termux_terminal_JNI_waitFor(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint pid)
{
    int status;
    waitpid(pid, &status, 0);
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else if (WIFSIGNALED(status)) {
        return -WTERMSIG(status);
    } else {
        // Should never happen - waitpid(2) says "One of the first three macros will evaluate to a non-zero (true) value".
        return 0;
    }
}

JNIEXPORT void JNICALL Java_com_termux_terminal_JNI_close(JNIEnv* TERMUX_UNUSED(env), jclass TERMUX_UNUSED(clazz), jint fileDescriptor)
{
    close(fileDescriptor);
}
