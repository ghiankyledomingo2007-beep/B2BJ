import concurrent.futures,difflib,json,re,sys,wave
from pathlib import Path
import requests
from generate import HERE,headers

def normalize(s):return re.findall(r"[a-z0-9]+",s.lower().replace('’',"'"))
def check(speaker):
    records=json.loads((HERE/(speaker+'-recordings.json')).read_text())
    path=HERE/(speaker+'-check.wav');intervals=[];position=0
    with wave.open(str(path),'wb') as out:
        out.setparams((1,2,22050,0,'NONE','not compressed'))
        for record in records:
            with wave.open(str(HERE/record['file'])) as src:
                assert src.getframerate()==22050 and src.getnchannels()==1
                frames=src.readframes(src.getnframes());duration=len(frames)/44100
            intervals.append((position,position+duration,record));out.writeframes(frames);out.writeframes(b'\0'*22050);position+=duration+.5
    transcript=path.with_suffix('.transcript.json')
    if not transcript.exists():
        with path.open('rb') as stream:
            response=requests.post('https://api.elevenlabs.io/v1/speech-to-text',headers=headers(),files={'file':(path.name,stream,'audio/wav')},data={'model_id':'scribe_v2','language_code':'eng','diarize':'false','tag_audio_events':'false'},timeout=180)
        assert response.ok,(speaker,response.status_code)
        transcript.write_text(json.dumps(response.json(),indent=2)+'\n')
    words=json.loads(transcript.read_text()).get('words',[]);results=[]
    for start,end,record in intervals:
        actual=''.join(w['text'] for w in words if start-.08<=(w['start']+w['end'])/2<=end+.08)
        ratio=difflib.SequenceMatcher(None,normalize(record['text']),normalize(actual)).ratio()
        results.append(dict(file=record['file'],expected=record['text'],recognized=actual,match=round(ratio,3)))
    (HERE/(speaker+'-transcript-check.json')).write_text(json.dumps(results,indent=2)+'\n')
    print(speaker,len(results),'checked; flags:',json.dumps([r for r in results if r['match']<.8]),flush=True)
    return results

if __name__=='__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:list(pool.map(check,sys.argv[1:] or ['rainoray','inner_voice','sedge']))
