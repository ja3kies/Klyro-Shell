cmake_minimum_required(VERSION 3.22.1)
project(oryn_pty C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(oryn_pty SHARED
    pty_bridge.cpp
)

find_library(log-lib log)

# forkpty is provided by Android's libc; no separate libutil is needed.
target_link_libraries(oryn_pty ${log-lib})

# NDK r27 defaults to 4 KiB ELF PT_LOAD segment alignment. Android 15+
# may use 16 KiB pages; this applies only to the binaries built by this target.
# Third-party prebuilt libraries must be audited independently.
target_link_options(oryn_pty PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
)
