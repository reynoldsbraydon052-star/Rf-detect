cmake_minimum_required(VERSION 3.22.1)

project("llama_cpp_bindings")

# Define the shared library for the llama.cpp bindings
add_library(
    llama_bridge
    SHARED
    llama_bridge.cpp
)

# Find necessary Android libraries for logging and system calls
find_library(
    log-lib
    log
)

# Link bindings against necessary dependencies
target_link_libraries(
    llama_bridge
    ${log-lib}
)
