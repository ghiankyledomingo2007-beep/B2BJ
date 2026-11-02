# Ready-to-paste Claude trailer voice prompt — 2026-10-28

Use our approved game voices for the trailer.

Project: `/home/ghiankylledomingo/Domynix/gamedev-vault/Projects/B2BJ`

Read these files first:
- `docs/design/trailer-voice-handoff.md`
- `docs/design/japanese-voice-cast.json`
- `docs/design/japanese-voice-lines.tsv`
- `docs/art-review/voice-auditions/runtime-voices.json`

Replace the temporary Sora/Hijiri picks with our approved Rainoray B and Raphael voices. The cast JSON has exact ElevenLabs voice IDs, settings, seeds, and Raphael's echo/chime processing. Reuse the selected profiles; do not create new clones or substitute other cast members.

Reuse finished recordings from `assets/audio/dialogue/` first. The TSV lists all 361 clips with Japanese speech and English subtitles. For higher-rate sources, use the installed manifest record's `source` path; avoid intermediate takes or superseded English sources. For new trailer lines, follow the existing generation tools and matching cast settings. Use the already configured API credential without displaying it.

The Warden's old narrator voice is superseded. Use the new stone-golem profile in the cast JSON and sources under `assets/source/audio/warden-golem-ja-2026-10-28/`. Saiseiki's downloaded stereo source is `/home/ghiankylledomingo/Downloads/B2BJ-Trailer-Audio/Saiseiki-full-source.wav`; read its provenance and scope in the handoff. Nameless Story was not downloaded because the available source is DRM-protected.

Keep Japanese speech and English subtitles. Preserve natural timing; fit the edit around dialogue. Keep trailer outputs separate from game assets because Codex is updating the game. Do not overwrite runtime WAVs, shared source files, or the game's build.

The adult office-worker voice and synchronized old/new voice transition remain a deferred plan, not a finished recording. Do not assume another adult voice is Rainoray's approved voice.
