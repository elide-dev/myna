# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
# Bind a Mach-O image's LC_UUID to the debug information of the objects it links.
#
# A linked Mach-O image contains no DWARF: it stays in the object files and dsymutil
# collects it afterwards. ld64.lld derives LC_UUID from the image, so a source change
# that moves lines without changing machine code (formatting, comments, licence
# headers) yields the same executable and UUID but a different dSYM. Symbol servers
# rightly refuse different bytes under one identity. Embedding a digest of the linked
# objects, which do contain the DWARF, makes the image and hence its UUID differ
# whenever the debug content can. ELF build IDs already cover the linked .debug_*
# sections, so nothing is added elsewhere.
if(CMAKE_SCRIPT_MODE_FILE)
  # cmake -DOBJECTS=<a.o|b.o> -DOUTPUT=<file> -P debug-identity.cmake
  string(REPLACE "|" ";" objects "${OBJECTS}")
  if(NOT objects OR NOT OUTPUT)
    message(FATAL_ERROR "debug identity requires object files and an output path")
  endif()
  set(digests "")
  foreach(object IN LISTS objects)
    file(SHA256 "${object}" digest)
    string(APPEND digests "${digest}\n")
  endforeach()
  string(SHA256 identity "${digests}")
  file(WRITE "${OUTPUT}" "${identity}\n")
  return()
endif()

function(myna_bind_debug_identity target)
  if(NOT APPLE)
    return()
  endif()
  set(identity "${CMAKE_CURRENT_BINARY_DIR}/${target}.debug-identity")
  add_custom_command(TARGET ${target} PRE_LINK
    COMMAND "${CMAKE_COMMAND}" "-DOBJECTS=$<JOIN:$<TARGET_OBJECTS:${target}>,|>" "-DOUTPUT=${identity}"
            -P "${CMAKE_CURRENT_FUNCTION_LIST_FILE}"
    BYPRODUCTS "${identity}"
    COMMENT "Binding ${target} identity to its debug information"
    VERBATIM)
  target_link_options(${target} PRIVATE "LINKER:-sectcreate,__TEXT,__myna_debug_id,${identity}")
endfunction()
