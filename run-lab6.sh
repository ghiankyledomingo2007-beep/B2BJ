#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")"
exec java -Xms16m -Xmx96m -XX:+UseSerialGC -jar build/B2BJ.jar --lab6 --borderless "$@"
