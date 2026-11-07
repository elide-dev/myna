# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Invoke the pinned Native Image driver while preserving native build flags."""
import os
from pathlib import Path
import shlex
import subprocess
import sys


def native_flags(environ):
    flags = []
    for name, option in (("CPPFLAGS", "CCompilerOption"),
                         ("CFLAGS", "CCompilerOption"),
                         ("LDFLAGS", "NativeLinkerOption")):
        flags.extend(f"-H:{option}={value}" for value in shlex.split(environ.get(name, "")))
    return flags


if __name__ == "__main__":
    driver = Path(os.environ["GRAALVM_HOME"]) / "bin/native-image"
    subprocess.run([str(driver), "-H:+UnlockExperimentalVMOptions", *native_flags(os.environ),
                    *sys.argv[1:]], check=True)
