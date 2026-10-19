#!/usr/bin/env bash
# Rebuilds the generated Outpost art (ground bake, props, decals) into assets/.
# Usage: tools/art/build-art.sh [all|ground|forest|props|ui] [--force]
# A generated PNG that was edited by hand is kept (see tools/art/generated.sha); --force regenerates it.
set -eu
cd "$(dirname "$0")/../.."
classes="$(mktemp -d /tmp/b2bj-art.XXXXXX)"
trap 'rm -rf "$classes"' EXIT
javac --release 17 -d "$classes" src/*.java tools/art/*.java
java -cp "$classes" BuildArt "$@"
