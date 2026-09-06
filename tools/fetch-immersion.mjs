// Download only after get_image confirms completion; never submits generation jobs.
import {readFile,writeFile,mkdir,stat} from 'node:fs/promises';
const args=process.argv.slice(2);
const batch=args[0]==='--rainoray'?'rainoray-rework':args[0]==='--hud-finish'?'hud-finish-40':args[0]==='--hud'?'hud-40':'immersion-40';
if(['--hud','--hud-finish','--rainoray'].includes(args[0]))args.shift();
const root='docs/art-review/'+batch;
const selected=new Set(args);
if(!selected.size)throw Error('Specify completed job names from jobs.json');
const jobs=JSON.parse(await readFile(root+'/jobs.json','utf8')).filter(j=>selected.has(j.name));
if(jobs.length!==selected.size)throw Error('Unknown job name');
for(const job of jobs) {
  if(!/^[a-z0-9-]+$/.test(job.name)||!/^[a-f0-9-]{36}$/.test(job.id)||!Number.isInteger(job.frames)||job.frames<1||job.frames>65)throw Error('Invalid job');
  await mkdir(root+'/'+job.name,{recursive:true});
  for(let start=0;start<job.frames;start+=4)await Promise.all(Array.from({length:Math.min(4,job.frames-start)},async(_,offset)=>{
    const index=start+offset,path=root+'/'+job.name+'/frame-'+index+'.png';
    try{if((await stat(path)).size>0)return;}catch(error){if(error.code!=='ENOENT')throw error;}
    const response=await fetch('https://api.pixellab.ai/mcp/images/'+job.id+'/download?index='+index,{signal:AbortSignal.timeout(45000)});
    if(!response.ok)throw Error(job.name+' frame '+index+': HTTP '+response.status);
    const bytes=Buffer.from(await response.arrayBuffer());
    if(bytes.length>8_000_000||!bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))throw Error('Invalid PNG download');
    await writeFile(path,bytes,{flag:'wx'});
  }));
  console.log(job.name+': '+job.frames+' frames downloaded');
}
