# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
# Preserve the pinned sysroot/linker setup, then honor explicit compiler choices.
include("$ENV{CMAKE_TOOLCHAIN_FILE}")
string(APPEND CMAKE_C_FLAGS_INIT " $ENV{CPPFLAGS}")
string(APPEND CMAKE_CXX_FLAGS_INIT " $ENV{CPPFLAGS}")
if(DEFINED ENV{CC} AND NOT "$ENV{CC}" STREQUAL "")
  set(CMAKE_C_COMPILER "$ENV{CC}")
endif()
if(DEFINED ENV{CXX} AND NOT "$ENV{CXX}" STREQUAL "")
  set(CMAKE_CXX_COMPILER "$ENV{CXX}")
endif()
