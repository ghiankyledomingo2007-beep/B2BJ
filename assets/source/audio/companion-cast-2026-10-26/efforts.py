import hashlib,json,math,sys,time,wave,array
from pathlib import Path
import requests
from generate import HERE,CAST,PROFILES,ff,headers

TAKES={'effort_attack_4': 'Ha!', 'effort_attack_5': 'Hyah!', 'effort_attack_6': 'Take that!', 'effort_heavy_3': 'Here!', 'effort_heavy_4': 'Hah!', 'effort_hurt_3': 'Ow!', 'effort_hurt_4': 'That hurt!', 'effort_hurt_5': 'Easy!', 'effort_hurt_6': 'Ah!', 'effort_hurt_7': 'Still here.', 'effort_hurt_8': 'Ugh!'}
report=[]
for key,text in TAKES.items():
    raw=HERE/(key+'.mp3');meta=HERE/(key+'.json')
    if not raw.exists():
        payload={'text':text,'model_id':'eleven_multilingual_v2','voice_settings':PROFILES['RAINORAY'],'seed':84722+list(TAKES).index(key)+1}
        response=requests.post('https://api.elevenlabs.io/v1/text-to-speech/'+CAST['RAINORAY'],params={'output_format':'mp3_44100_192'},headers=headers(),json=payload,timeout=90)
        assert response.ok and response.headers.get('content-type','').startswith('audio/'),(key,response.status_code)
        raw.write_bytes(response.content)
        meta.write_text(json.dumps(dict(file=key+'.wav',speaker='RAINORAY',text=text,voice_id=CAST['RAINORAY'],model='eleven_multilingual_v2',settings=PROFILES['RAINORAY'],seed=84722+list(TAKES).index(key)+1,generated_at=time.time(),request_id=response.headers.get('request-id',response.headers.get('x-request-id',''))),indent=2)+'\n')
    record=json.loads(meta.read_text());assert record['text']==text and record['voice_id']==CAST['RAINORAY']
    final=HERE/(key+'.wav')
    ff(['-i',str(raw),'-af','highpass=f=65,silenceremove=start_periods=1:start_duration=0.015:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_duration=0.015:start_threshold=-45dB,areverse,alimiter=limit=0.5:level=false','-ar','22050','-ac','1','-c:a','pcm_s16le',str(final)])
    with wave.open(str(final)) as wav:
        samples=array.array('h',wav.readframes(wav.getnframes()));duration=len(samples)/wav.getframerate()
    assert .1<duration<2.0,(key,duration)
    peak=max(abs(n) for n in samples)/32768;assert peak>.001
    gain=min(8,.42/peak)
    samples=array.array('h',(int(n*gain) for n in samples))
    with wave.open(str(final),'wb') as out:
        out.setparams((1,2,22050,0,'NONE','not compressed'));out.writeframes(samples.tobytes())
    peak=max(abs(n) for n in samples)/32768;assert .02<peak<.51
    record.update(seconds=round(duration,4),peak_dbfs=round(20*math.log10(peak),2),sha256=hashlib.sha256(final.read_bytes()).hexdigest())
    meta.write_text(json.dumps(record,indent=2)+'\n');report.append(record);print(key,round(duration,2),flush=True)
(HERE/'combat-efforts.json').write_text(json.dumps(report,indent=2)+'\n')
