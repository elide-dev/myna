"""Normalize metadata in newly built, unsigned Myna JARs; never published assets."""
import argparse
import os
from pathlib import Path
import time
import zipfile


def normalize(source, destination):
    epoch = max(315532800, int(os.environ.get("SOURCE_DATE_EPOCH", "315532800")))
    date = time.gmtime(epoch)[:6]
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(destination, "w") as output:
        for name in sorted(original.namelist(), key=lambda n: (n != "META-INF/MANIFEST.MF", n)):
            if name.upper().startswith("META-INF/") and name.upper().endswith((".SF", ".RSA", ".DSA", ".EC")):
                raise ValueError("Refusing to rewrite a signed JAR")
            info = zipfile.ZipInfo(name, date)
            info.create_system = 3
            info.external_attr = (0o40755 if name.endswith("/") else 0o100644) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            output.writestr(info, original.read(name), compresslevel=9)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    if args.source.resolve() == args.destination.resolve():
        parser.error("Source and destination must differ")
    normalize(args.source, args.destination)
