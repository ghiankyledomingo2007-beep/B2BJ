# Slime remaster inputs

Original runtime recordings preserved before the 2026-10-25 player-slime remaster. These are processed source takes, not untouched field recordings. See `assets/audio/CREDITS.md` for their existing provenance: rubberduck CC0 water/slime recordings and the pre-existing generated `hurt` recordings.

`tools/audio/remaster_slime.py` reads these immutable inputs on each rerun, avoiding repeated processing of already remastered output. It uses local ffmpeg and the existing WAV finishing helper, no network or credentials. Sources stay outside the packaged runtime assets.
