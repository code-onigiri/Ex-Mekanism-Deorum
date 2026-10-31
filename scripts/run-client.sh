#!/usr/bin/env bash
# Launch the dev client with the graphics environment from flake.nix.
# --no-daemon makes the build JVM inherit this environment directly, so a
# Gradle daemon started before the flake change cannot hide LD_LIBRARY_PATH.
set -euo pipefail
cd "$(dirname "$0")/.."
exec nix develop --command gradle --no-daemon runClient "$@"
