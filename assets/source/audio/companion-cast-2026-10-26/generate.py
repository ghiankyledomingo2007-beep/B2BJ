import array, concurrent.futures, csv, hashlib, json, math, re, subprocess, sys, time, wave
from pathlib import Path
import requests

sys.path.insert(0,'/home/ghiankylledomingo/tmp/b2bj-voices')
from eleven_client import headers
ROOT=Path('/home/ghiankylledomingo/Domynix/gamedev-vault/Projects/B2BJ')
HERE=Path(__file__).resolve().parent
CAST={'RAINORAY': 'lOMvbhhkKRw1KRXcYQCn', 'INNER_VOICE': '5BqS4S2L9130EY4gngwS', 'SEDGE': 'cjVigY5qzO86Huf0OWal', 'MARA': 'EXAVITQu4vr4xnSDxMaL', 'TOBIN': 'pqHfZKP75CvOlQylNhV4', 'ILSE': 'cgSgspJ2msm6clMCkdW9', 'VENN': 'JBFqnCBsd6RMkjVDRZzb', 'WARDEN': 'N2lVS1w4EtoT3dr4eOWO'}
PROFILES={'RAINORAY': {'stability': 0.6, 'similarity_boost': 0.55, 'style': 0, 'use_speaker_boost': False}, 'INNER_VOICE': {'stability': 0.7, 'similarity_boost': 0.75, 'style': 0, 'use_speaker_boost': True}, 'SEDGE': {'stability': 0.65, 'similarity_boost': 0.75, 'style': 0, 'use_speaker_boost': True}, 'MARA': {'stability': 0.5, 'similarity_boost': 0.75, 'style': 0.15, 'use_speaker_boost': True}, 'TOBIN': {'stability': 0.5, 'similarity_boost': 0.75, 'style': 0.15, 'use_speaker_boost': True}, 'ILSE': {'stability': 0.5, 'similarity_boost': 0.75, 'style': 0.15, 'use_speaker_boost': True}, 'VENN': {'stability': 0.5, 'similarity_boost': 0.75, 'style': 0.15, 'use_speaker_boost': True}, 'WARDEN': {'stability': 0.65, 'similarity_boost': 0.75, 'style': 0, 'use_speaker_boost': True}}
PROFILES['TOBIN'].update(stability=.6,style=.1,speed=.93)
PROFILES['ILSE'].update(stability=.55,style=.1)
PROFILES['SEDGE'].update(stability=.68,style=.05,speed=.96)
PROFILES['WARDEN'].update(stability=.55,style=.2,speed=.95)
FX=json.loads((ROOT/'docs/art-review/voice-auditions/polish-settings.json').read_text())
LINES=list(csv.DictReader((HERE/'voice-lines.tsv').open(),delimiter='\t'))


def ff(args):
    return subprocess.run(['ffmpeg','-hide_banner','-nostdin','-y',*args],check=True,capture_output=True,text=True)

def seconds(path):
    with wave.open(str(path)) as wav:return wav.getnframes()/wav.getframerate()

def generate(speaker,pilot=False):
    lines=[line for line in LINES if line['speaker']==speaker]
    if pilot:lines=lines[:4]
    context=[];report=[]
    for index,line in enumerate(lines):
        key=Path(line['file']).stem;raw=HERE/(key+'.mp3');meta=HERE/(key+'.json')
        if raw.exists():
            record=json.loads(meta.read_text())
            assert record['text']==line['text'] and record['settings']==PROFILES[speaker] and record['voice_id']==CAST[speaker], 'Cached generation differs from current script/profile'
        else:
            recent=[r['request_id'] for r in context if time.time()-r['generated_at']<7000][-3:]
            payload={'text':line.get('spoken_text') or line['text'],'model_id':'eleven_multilingual_v2','voice_settings':PROFILES[speaker],'seed':84722}
            if recent:payload['previous_request_ids']=recent
            # Each recording is a complete utterance; do not suggest an unrelated next line.
            response=requests.post('https://api.elevenlabs.io/v1/text-to-speech/'+CAST[speaker],
                    params={'output_format':'mp3_44100_192'},headers=headers(),json=payload,timeout=90)
            if not response.ok:raise RuntimeError('TTS HTTP '+str(response.status_code)+' for '+key)
            assert response.headers.get('content-type','').startswith('audio/') and len(response.content)>1000
            raw.write_bytes(response.content)
            record={**line,'voice_id':CAST[speaker],'model':'eleven_multilingual_v2','settings':PROFILES[speaker],
                    'seed':84722,'request_id':response.headers.get('request-id',response.headers.get('x-request-id','')),
                    'context_request_ids':recent,'generated_at':time.time(),'format':'mp3_44100_192'}
            assert record['request_id'],'No generation request ID'
            meta.write_text(json.dumps(record,indent=2)+'\n')
        context.append(record)
        effected=HERE/(key+'-fx.wav')
        if speaker=='INNER_VOICE':
            fast=line['file'].startswith(('bark_forest_root_','bark_forest_seed_','bark_forest_cage_'))
            filt=FX['inner_voice_filter'].replace('adelay=190|190','adelay=0|0') if fast else FX['inner_voice_filter']
            ff(['-i',str(raw),'-f','lavfi','-i',FX['chime_source'],'-filter_complex',filt,'-map','[fx]','-ar','22050','-ac','1','-c:a','pcm_s16le',str(effected)])
        elif speaker=='WARDEN':
            ff(['-i',str(raw),'-af','highpass=f=65','-ar','22050','-ac','1','-c:a','pcm_s16le',str(effected)])
        else:
            ff(['-i',str(raw),'-af','highpass=f=65','-ar','22050','-ac','1','-c:a','pcm_s16le',str(effected)])
        # Timing follows each complete recording; never speed up or cut speech.
        tempo=1;prefix=''
        analysis=ff(['-i',str(effected),'-af',prefix+'loudnorm=I=-18:TP=-4:LRA=9:print_format=json','-f','null','-']).stderr
        data=json.loads(re.search(r'\{[^{}]+"input_i"[^{}]+\}',analysis,re.S).group())
        assert all(math.isfinite(float(data[k])) for k in ['input_i','input_tp','input_lra','input_thresh','target_offset'])
        filt=prefix+'loudnorm=I=-18:TP=-4:LRA=9:measured_I={input_i}:measured_TP={input_tp}:measured_LRA={input_lra}:measured_thresh={input_thresh}:offset={target_offset}:linear=true'.format(**data)
        final=HERE/line['file'];ff(['-i',str(effected),'-af',filt,'-ar','22050','-ac','1','-c:a','pcm_s16le',str(final)])
        with wave.open(str(final)) as wav:
            duration=wav.getnframes()/wav.getframerate();samples=array.array('h',wav.readframes(wav.getnframes()));peak=max(abs(n) for n in samples)/32768
        assert .5<duration<30 and .05<peak<.72,(key,duration,peak)
        record.update(seconds=round(duration,3),peak_dbfs=round(20*math.log10(peak),2),tempo=round(tempo,5),sha256=hashlib.sha256(final.read_bytes()).hexdigest())
        meta.write_text(json.dumps(record,indent=2)+'\n');report.append(record)
        (HERE/(speaker.lower()+'-recordings.json')).write_text(json.dumps(report,indent=2)+'\n')
        if True:print(speaker,str(index+1)+'/'+str(len(lines)),key,round(duration,2),flush=True)
    return report

if __name__=='__main__':
    speakers=list(dict.fromkeys(line['speaker'] for line in LINES))
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        list(pool.map(generate,speakers))
