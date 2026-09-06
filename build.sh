#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
build_dir="$(mktemp -d /tmp/b2bj-build.XXXXXX)"
mkdir -p "$build_dir/classes/assets" "$build_dir/tests" build
javac --release 17 -Xlint:all -d "$build_dir/classes" src/*.java
for asset_dir in characters tilesets ui effects; do
    if [ -d "assets/$asset_dir" ]; then
        cp -R "assets/$asset_dir" "$build_dir/classes/assets/"
    fi
done
mkdir -p "$build_dir/classes/assets/props"
cp -R assets/props/outpost "$build_dir/classes/assets/props/"
javac --release 17 -Xlint:all -cp "$build_dir/classes" -d "$build_dir/tests" test/*.java
for test_file in test/*Test.java; do
    test_name="${test_file##*/}"
    java -ea -Djava.awt.headless=true -cp "$build_dir/classes:$build_dir/tests" "${test_name%.java}"
done
jar --create --file build/B2BJ.jar --main-class B2BJ -C "$build_dir/classes" .
(
    cd /tmp
    java -ea -Djava.awt.headless=true -cp "$build_dir/classes:$build_dir/tests" PackagedAssetLoadingTest
)
printf 'Verified build: %s/build/B2BJ.jar\n' "$PWD"
