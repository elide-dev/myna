"""Run a Python script under Elide with explicitly supplied tool environment."""
import argparse
import os
from pathlib import Path
import runpy
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--env", nargs=2, action="append", default=[])
parser.add_argument("script")
parser.add_argument("args", nargs=argparse.REMAINDER)
options = parser.parse_args()
os.environ.update(options.env)
sys.argv = [options.script, *options.args]
sys.path.insert(0, str(Path(options.script).resolve().parent))
runpy.run_path(options.script, run_name="__main__")
