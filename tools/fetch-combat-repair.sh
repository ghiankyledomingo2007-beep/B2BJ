#!/usr/bin/env bash
# Run only after get_image/list_jobs confirms selected jobs completed.
set -eu
cd "$(dirname "$0")/.."
if [[ $# == 0 || " $* " == *" guardian-walk "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-walk'
for i in {0..8}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-walk/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/b4551948-6780-4f59-89c1-5f6f56389867/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-walk/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-walk'
fi
if [[ $# == 0 || " $* " == *" guardian-charge "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-charge'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-charge/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/5de48427-5345-4464-886f-62dc3d87ae14/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-charge/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-charge'
fi
if [[ $# == 0 || " $* " == *" guardian-slam "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-slam'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-slam/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/402bf49a-c8ea-4ee8-bc0e-cc2973b4c333/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-slam/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-slam'
fi
if [[ $# == 0 || " $* " == *" guardian-sweep "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-sweep'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-sweep/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/2a22ad38-fe2d-4632-bccc-c5c2e3d12c95/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-sweep/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-sweep'
fi
if [[ $# == 0 || " $* " == *" guardian-shockwave "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-shockwave'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-shockwave/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/dd5e21e4-6dc7-47b1-a2de-06de4368a9f4/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-shockwave/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-shockwave'
fi
if [[ $# == 0 || " $* " == *" guardian-hurt "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-hurt'
for i in {0..8}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-hurt/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/a659e4cf-a620-40da-ba43-d372c4407d23/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-hurt/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-hurt'
fi
if [[ $# == 0 || " $* " == *" guardian-death "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/guardian-death'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/guardian-death/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/d3ae521e-2b05-42b1-84f0-4b994b7772a6/download?index='"$i" -o 'docs/art-review/combat-repair-40/guardian-death/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/guardian-death'
fi
if [[ $# == 0 || " $* " == *" blade-side-cut "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/blade-side-cut'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/blade-side-cut/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/d3a32cd2-c965-4e81-815a-bfa9381aee7f/download?index='"$i" -o 'docs/art-review/combat-repair-40/blade-side-cut/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/blade-side-cut'
fi
if [[ $# == 0 || " $* " == *" blade-side-thrust "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/blade-side-thrust'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/blade-side-thrust/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/f04c7cb0-6721-4bcb-88d2-d94bd3ecb396/download?index='"$i" -o 'docs/art-review/combat-repair-40/blade-side-thrust/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/blade-side-thrust'
fi
if [[ $# == 0 || " $* " == *" blade-side-return "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/blade-side-return'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/blade-side-return/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/83a0c487-513d-4537-9c08-0bc686d92adb/download?index='"$i" -o 'docs/art-review/combat-repair-40/blade-side-return/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/blade-side-return'
fi
if [[ $# == 0 || " $* " == *" slime-cast-south "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/slime-cast-south'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/slime-cast-south/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/b0867003-fe00-4a87-9bd0-50b76ba13f35/download?index='"$i" -o 'docs/art-review/combat-repair-40/slime-cast-south/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/slime-cast-south'
fi
if [[ $# == 0 || " $* " == *" slime-cast-east "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/slime-cast-east'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/slime-cast-east/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/4f1122a9-8c5c-44d0-be4f-b5d97aacb817/download?index='"$i" -o 'docs/art-review/combat-repair-40/slime-cast-east/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/slime-cast-east'
fi
if [[ $# == 0 || " $* " == *" slime-cast-north "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/slime-cast-north'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/slime-cast-north/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/006c1260-5d36-4864-b7e6-d2545f39e7f0/download?index='"$i" -o 'docs/art-review/combat-repair-40/slime-cast-north/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/slime-cast-north'
fi
if [[ $# == 0 || " $* " == *" remnant-attack-south "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/remnant-attack-south'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/remnant-attack-south/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/1bf5066b-0b57-4eb2-a6e0-0c5f1f0f653c/download?index='"$i" -o 'docs/art-review/combat-repair-40/remnant-attack-south/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/remnant-attack-south'
fi
if [[ $# == 0 || " $* " == *" remnant-attack-west "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/remnant-attack-west'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/remnant-attack-west/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/b334ad4e-21e3-4174-b447-7fbbe18b076e/download?index='"$i" -o 'docs/art-review/combat-repair-40/remnant-attack-west/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/remnant-attack-west'
fi
if [[ $# == 0 || " $* " == *" remnant-attack-north "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/remnant-attack-north'
for i in {0..16}; do
  [[ -s 'docs/art-review/combat-repair-40/remnant-attack-north/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/6bf6e731-fe95-409d-a9bb-848bbef7026d/download?index='"$i" -o 'docs/art-review/combat-repair-40/remnant-attack-north/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/remnant-attack-north'
fi
if [[ $# == 0 || " $* " == *" warden-impact "* ]]; then
mkdir -p 'docs/art-review/combat-repair-40/warden-impact'
for i in {0..0}; do
  [[ -s 'docs/art-review/combat-repair-40/warden-impact/frame-'"$i"'.png' ]] && continue
  curl --fail --silent --show-error --max-time 45 'https://api.pixellab.ai/mcp/images/9510042b-e422-4c57-9d20-22b4b8a01422/download?index='"$i" -o 'docs/art-review/combat-repair-40/warden-impact/frame-'"$i"'.png' &
  if ((i%4==3)); then wait; fi
done
wait
java tools/ReviewAnimation.java 'docs/art-review/combat-repair-40/warden-impact'
fi
