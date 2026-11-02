#!/usr/bin/env python3
"""Offline integrity/cast/language audit of the installed Japanese voice bank."""
import array
import csv
import hashlib
import json
from pathlib import Path
import re
import unicodedata
import wave

ROOT = Path(__file__).resolve().parents[2]


def main():
    manifest = json.loads((ROOT / 'docs/art-review/voice-auditions/runtime-voices.json').read_text())
    cast = json.loads((ROOT / 'docs/design/japanese-voice-cast.json').read_text())
    scripts = list(csv.DictReader((ROOT / 'docs/design/japanese-voice-lines.tsv').open(), delimiter='\t'))
    records = manifest['lines'] + manifest['combat_efforts']
    assert len(records) == len(scripts) == 361
    by_file = {row['file']: row for row in records}
    assert len(by_file) == 361
    runtime = ROOT / 'assets/audio/dialogue'
    assert set(by_file) == {row['file'] for row in scripts} == {p.name for p in runtime.glob('*.wav')}
    total = 0
    for row in scripts:
        name = row['file']; record = by_file[name]; path = runtime / name
        assert record['speaker'] == row['speaker'], name
        assert record.get('language') == row['language'] == 'ja', name
        assert record['spoken_text'] == row['spoken_text'], name
        assert re.search('[\u3040-\u30ff\u4e00-\u9fff]', row['spoken_text']), name
        profile = cast[row['speaker']]
        assert record['voice_id'] == profile['voice_id'], f'Cast mismatch: {name}'
        assert record['model'] == profile['model_id'] and record['settings'] == profile['voice_settings'], name
        assert hashlib.sha256(path.read_bytes()).hexdigest() == record['sha256'], name
        if row['kind'] != 'effort':
            heard = re.sub(r'\W', '', unicodedata.normalize('NFKC', record.get('transcript', '')))
            expected = re.sub(r'\W', '', unicodedata.normalize('NFKC', row['spoken_text']))
            # Short Japanese phrases can receive an incorrect automatic language label.
            assert record.get('detected_language') == 'jpn' or (heard == expected
                and re.search('[\u3040-\u30ff]', heard)), f'ASR language needs review: {name}'
        with wave.open(str(path)) as audio:
            assert (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) == (1, 2, 22050), name
            samples = array.array('h', audio.readframes(audio.getnframes()))
        seconds = len(samples) / 22050
        assert abs(seconds - record['seconds']) < .002, name
        assert .1 < seconds < (2 if row['kind'] == 'effort' else 30), name
        assert 1000 < max(map(abs, samples)) < 26500, name
        total += seconds
    print(f'Japanese bank verified: {len(manifest["lines"])} dialogue clips, '
          f'{len(manifest["combat_efforts"])} efforts, {total / 60:.1f} minutes, eight fixed cast profiles.')


if __name__ == '__main__':
    main()
