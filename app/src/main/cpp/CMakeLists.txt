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
