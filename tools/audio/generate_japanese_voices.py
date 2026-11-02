#!/usr/bin/env python3
"""Generate the reviewed Japanese TSV into staging; never installs runtime assets.

Usage: ELEVENLABS_API_KEY=... python3 tools/audio/generate_japanese_voices.py SCRIPT CAST OUTPUT
Existing matching requests are reused. The output records every request, take and transcript.
"""
import array
import concurrent.futures
import csv
import hashlib
import json
import math
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import wave

import requests
from warden_voice_fx import treatment


def ff(*args):
    return subprocess.run(['ffmpeg', '-hide_banner', '-nostdin', '-y', '-threads', '1', *args],
                          capture_output=True, text=True, check=True)


def request(url, **kwargs):
    for attempt in range(4):
        response = requests.post('https://api.elevenlabs.io/v1/' + url,
                                 headers={'xi-api-key': os.environ['ELEVENLABS_API_KEY']},
                                 timeout=120, **kwargs)
        if response.status_code not in (429, 500, 502, 503, 504):
            break
        if 'files' in kwargs:
            for _, stream, _ in kwargs['files'].values():
                stream.seek(0)
        time.sleep(min(30, 2 ** (attempt + 1)))
    if not response.ok:
        raise RuntimeError(f'ElevenLabs HTTP {response.status_code}: {response.text[:250]}')
    return response


def read(path):
    with wave.open(str(path)) as wav:
        assert (wav.getnchannels(), wav.getsampwidth(), wav.getframerate()) == (1, 2, 22050)
        return array.array('h', wav.readframes(wav.getnframes()))


def write(path, samples):
    with wave.open(str(path), 'wb') as wav:
        wav.setparams((1, 2, 22050, 0, 'NONE', 'not compressed'))
        wav.writeframes(array.array('h', samples).tobytes())


def render(line, cast, root):
    name = line['file']; voice = cast[line['speaker']]
    raw = root / name.replace('.wav', '.mp3')
    metadata = raw.with_suffix('.json')
    payload = {'text': line['spoken_text'], 'model_id': voice['model_id'], 'language_code': 'ja',
               'voice_settings': voice['voice_settings'], 'seed': voice['audition_seed']}
    if raw.exists():
        old = json.loads(metadata.read_text())
        assert old['request'] == payload and old['voice_id'] == voice['voice_id'], f'Stale take: {name}'
    else:
        response = request('text-to-speech/' + voice['voice_id'],
                           params={'output_format': 'mp3_44100_192'}, json=payload)
        assert len(response.content) > 1000
        metadata.write_text(json.dumps({'request': payload, 'voice_id': voice['voice_id'],
            'request_id': response.headers.get('request-id'), 'generated_at': time.time()},
            ensure_ascii=False, indent=2))
        raw.write_bytes(response.content)
    output = root / 'rendered' / name
    if not output.exists():
        processing = raw
        if line['speaker'] == 'INNER_VOICE':
            processing = root / 'processed' / name
            ff('-v', 'error', '-i', str(raw), '-f', 'lavfi', '-i', voice['chime_source'],
               '-filter_complex', voice['effect'], '-map', '[fx]', '-ar', '44100', '-ac', '1',
               '-c:a', 'pcm_f32le', str(processing))
        elif line['speaker'] == 'WARDEN':
            dry = root / 'processed' / ('dry-' + name)
            ff('-v', 'error', '-i', str(raw), '-af', 'highpass=f=65', '-ar', '22050', '-ac', '1',
               '-c:a', 'pcm_s16le', str(dry))
            samples = treatment([v / 32768 for v in read(dry)], name.endswith('_order.wav'))
            gain = min(1, .9 / max(map(abs, samples)))
            processing = root / 'processed' / name
            write(processing, (round(v * gain * 32767) for v in samples))
        measured = ff('-i', str(processing), '-af', 'loudnorm=I=-18:TP=-4:LRA=11:print_format=json',
                      '-f', 'null', '-').stderr
        m = json.loads(re.search(r'\{\s*"input_i".*?\}', measured, re.S).group())
        norm = ('loudnorm=I=-18:TP=-4:LRA=11:measured_I={input_i}:measured_TP={input_tp}:'
                'measured_LRA={input_lra}:measured_thresh={input_thresh}:offset={target_offset}:linear=true').format(**m)
        temporary = output.with_suffix('.partial.wav')
        ff('-v', 'error', '-i', str(processing), '-af', norm, '-ar', '22050', '-ac', '1',
           '-c:a', 'pcm_s16le', str(temporary))
        if line['kind'] == 'effort':
            samples = read(temporary)
            # Trim only edge silence; keep internal breaths and the original performance speed.
            active = [i for i in range(0, len(samples), 220)
                      if max(map(abs, samples[i:i+220]), default=0) > 160]
            assert active, f'Silent effort: {name}'
            samples = samples[max(0, active[0]-660):min(len(samples), active[-1]+1100)]
            gain = 13800 / max(map(abs, samples))
            write(temporary, (round(v * gain) for v in samples))
        temporary.replace(output)
    samples = read(output); seconds = len(samples) / 22050; peak = max(map(abs, samples)) / 32768
    assert .10 < seconds < 30 and .03 < peak < .8, name
    transcript = raw.with_suffix('.transcript.json')
    if not transcript.exists():
        with output.open('rb') as stream:
            response = request('speech-to-text', files={'file': (name, stream, 'audio/wav')},
                               data={'model_id': 'scribe_v2', 'diarize': 'false', 'tag_audio_events': 'false'})
        transcript.write_text(json.dumps(response.json(), ensure_ascii=False, indent=2))
    speech = json.loads(transcript.read_text())
    record = {**line, 'voice_id': voice['voice_id'], 'model': voice['model_id'],
              'settings': voice['voice_settings'], 'seed': voice['audition_seed'],
              'request_id': json.loads(metadata.read_text())['request_id'], 'seconds': seconds,
              'peak_dbfs': 20 * math.log10(peak), 'tempo': 1, 'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
              'transcript': speech.get('text', ''), 'detected_language': speech.get('language_code'),
              'status': 'generated; transcript review pending'}
    (root / 'records' / name.replace('.wav', '.json')).write_text(json.dumps(record, ensure_ascii=False, indent=2))
    return record


def main():
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    script, cast_path, root = map(Path, sys.argv[1:])
    lines = list(csv.DictReader(script.open(), delimiter='\t')); cast = json.loads(cast_path.read_text())
    assert len({x['file'] for x in lines}) == len(lines)
    for line in lines:
        assert re.fullmatch(r'[a-z0-9_]+\.wav', line['file']) and line['speaker'] in cast
        assert line['language'] == 'ja' and re.search('[\u3040-\u30ff\u4e00-\u9fff]', line['spoken_text'])
    for folder in ['rendered', 'processed', 'records']:
        (root / folder).mkdir(parents=True, exist_ok=True)
    failures = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        jobs = {pool.submit(render, line, cast, root): line['file'] for line in lines}
        for i, job in enumerate(concurrent.futures.as_completed(jobs), 1):
            try:
                record = job.result()
                print(json.dumps({'done': i, 'total': len(lines), 'file': record['file'],
                    'seconds': round(record['seconds'], 2), 'heard': record['transcript'],
                    'language': record['detected_language']}, ensure_ascii=False), flush=True)
            except Exception as error:
                failures.append({'file': jobs[job], 'error': str(error)})
                print(json.dumps(failures[-1]), flush=True)
    (root / 'failures.json').write_text(json.dumps(failures, indent=2))
    if failures:
        raise SystemExit(f'{len(failures)} clips failed; matching completed requests are cached.')


if __name__ == '__main__':
    main()
