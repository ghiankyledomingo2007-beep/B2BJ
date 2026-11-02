"use strict";
// Animation study only. No combat or save code runs in this page.
const $=id=>document.getElementById(id), canvas=$("scene"), ctx=canvas.getContext("2d");
// Violet, gold and cyan sampled from the installed Warden sprite.
const C={stone:"#4a2757",edge:"#9265a0",dark:"#17121f",dust:"#4a2757",deep:"#2a1836",cyan:"#2ed1cd",white:"#d4fff3",gold:"#ffd08a"};
// Core centers measured from cyan pixels in the authored sprite frames.
const coreAnchors={motion:[[[59,69],[59,69],[59,67],[58,65],[59,65],[59,65],[59,65],[59,67]],[[59,69],[59,67],[59,65],[59,63],[59,61],[60,63],[59,65],[60,67]],[[60,71],[61,69],[61,65],[60,62],[60,60],[59,59],[60,58],[60,60]],[[65,61],[62,66],[59,76],[59,78],[59,77],[59,78],[59,80],[61,77]],[[61,75],[61,73],[59,73],[58,69],[60,67],[60,64],[60,66],[59,69]]],sweep:[[[59,67],[59,63],[48,64],[55,65],[70,64],[82,61],[68,63],[60,65]]]};
const concepts={
 fight:{name:"Moving encounter",kicker:"Pressure without constant tracking",summary:"The Warden pursues a moving player, closes gaps with a committed shoulder rush, and steps into close attacks. Phase two adds detached fists and a turning core beam.",read:"He follows briefly during preparation, then locks direction. The core spells have longer tells. This demo runs continuously; Take control lets you test kiting.",response:"WASD or arrows to move. Space to dash. Hold left mouse to shoot toward the cursor. Dash cooldown is 1.2 seconds. Backhand leaves his rear safe; rush cannot steer after launch.",punish:"Every attack ends with an exposed core. Missing a rush into the arena edge gives an extra opening. Phase two trims recovery by only 0.15s and alternates physical attacks with core spells.",hint:"Try circling and shooting. Watch him lead your path, then change direction after he commits."},
 hammer:{name:"Hammerfall",kicker:"A committed, close-range slam",summary:"Both arms rise. Cyan pressure gathers between the armor plates. The Warden plants its weight into the ground and has to pull itself free.",read:"Raised arms, grinding shoulders and small violet plates lifting around its fists. The last part of the wind-up stops tracking.",response:"Dodge to either side as the arms fall. The damage stays around the actual contact, not a remote circle under the player.",punish:"Its fists stay buried for roughly 1.5 seconds. The core opens while it struggles back up.",hint:"Dodge the falling arms, then return during recovery."},
 sweep:{name:"Core Sweep",kicker:"A front arc with a safe back",summary:"The shoulder winds across the body, then one arm pulls a violet energy edge through the front of the arena, traced by the cyan core.",read:"The turning shoulder and scraping hand show the swing direction. A thin cyan scrape marks the beginning.",response:"Move around its back or dash through the late swing. Replace the current full-circle hit with a front arc.",punish:"The arm finishes wide and the torso takes time to settle. Reward a close flank instead of always running away.",hint:"Move behind the shoulder. The back stays safe."},
 fault:{name:"Faultline",kicker:"Ground pressure that travels",summary:"A planted strike sends a narrow fracture forward. Violet armor fins erupt along the locked line, split by cyan energy and capped with horn gold.",read:"The hand presses down, the core tightens, and a small crack forms at the source. The path commits before release.",response:"Sidestep the line. Do not use a screen-wide instant fissure or retarget the player after the ground starts breaking.",punish:"The Warden stays planted while the fault runs out. Phase two burns brighter inside the same narrow path; the dodge window stays readable.",hint:"Sidestep the travelling fracture; it will not follow."},
 core:{name:"Core Unbound",kicker:"One transition at half health",summary:"The core strains against its violet casing. Gold seams separate, small armor plates lift away, and one cyan pressure pulse releases phase two.",read:"A different pose and a rising core tone. Give this moment space instead of stacking it on an active attack.",response:"A short transition, not unavoidable damage. The player can reposition while the armor settles.",punish:"Fight flow adds the shoulder rush, detached fists and chest beam. Each attack still commits and leaves a recovery window.",hint:"Phase change: one strong pulse, then the fight breathes."}
};
let mode="fight", bossPhase=1,energy=0,t=0,playing=true,slow=false,reduced=matchMedia("(prefers-reduced-motion: reduce)").matches, sound=false,last=performance.now(),audioContext;
const images={},buffers={},duration=4.8,impact=1.65;
const clamp=v=>Math.max(0,Math.min(1,v)),smooth=v=>{v=clamp(v);return v*v*(3-2*v);},mix=(a,b,p)=>a+(b-a)*p;
const rand=n=>{let x=Math.sin(n*91.17+4.27)*43758.5453;return x-Math.floor(x);};
function poly(points,color,alpha=1){if(alpha<=0)return;ctx.save();ctx.globalAlpha=clamp(alpha);ctx.fillStyle=color;ctx.beginPath();points.forEach(([x,y],i)=>i?ctx.lineTo(Math.round(x),Math.round(y)):ctx.moveTo(Math.round(x),Math.round(y)));ctx.closePath();ctx.fill();ctx.restore();}
function line(points,color,width=1,alpha=1){ctx.save();ctx.globalAlpha=clamp(alpha);ctx.strokeStyle=color;ctx.lineWidth=width;ctx.beginPath();points.forEach(([x,y],i)=>i?ctx.lineTo(Math.round(x),Math.round(y)):ctx.moveTo(Math.round(x),Math.round(y)));ctx.stroke();ctx.restore();}
function ellipse(x,y,rx,ry,color,alpha=1){ctx.save();ctx.fillStyle=color;ctx.globalAlpha=clamp(alpha);ctx.beginPath();ctx.ellipse(Math.round(x),Math.round(y),Math.max(.1,rx),Math.max(.1,ry),0,0,Math.PI*2);ctx.fill();ctx.restore();}
function shard(x,y,size,angle,alpha=1){const ca=Math.cos(angle),sa=Math.sin(angle),local=[[-size,0],[0,-size*.65],[size*.8,-size*.2],[size*.45,size*.5],[-size*.4,size*.65]],points=local.map(([a,b])=>[x+a*ca-b*sa,y+a*sa+b*ca]);
 poly(points,C.dark,alpha);poly(points.map(([a,b])=>[mix(a,x,.2),mix(b,y-1,.2)]),C.stone,alpha);
 line([points[0],points[1],points[2]],C.edge,1,alpha*.85);
 if(ca>.3)line([points[1],points[2]],C.gold,1,alpha*.75);
 if(energy>0)line([[x-1,y-1],[x+1,y+1]],C.cyan,1,alpha*energy);
}
function debris(x,y,age,seed,power=1,count=21){if(age<0||age>1.45)return;for(let i=0;i<Math.ceil(count*(reduced?.18:.52));i++){let a=rand(seed+i)*Math.PI*2,v=52+rand(seed+i+30)*115,z=(65+rand(seed+i+80)*95)*power*age-110*age*age;
 const dx=Math.cos(a)*v*age*power,dy=Math.sin(a)*v*.55*age*power,fade=clamp((1.45-age)/.45),size=1.5+rand(seed+i+140)*2.5;
 if(!reduced)ellipse(x+dx,y+dy,size+1,1.5,"#111b1d",.3*fade);
 shard(x+dx,y+dy-Math.max(0,z),size,a+age*4,fade);}}
function dust(x,y,age,seed,power=1){if(age<0||age>1.4)return;let n=reduced?2:7;for(let i=0;i<n;i++){const a=rand(seed+i)*Math.PI*2,r=(10+Math.sqrt(age)*(28+rand(seed+i+10)*45))*power,rx=(3+Math.sin(clamp(age/.5)*Math.PI/2)*6)*power,alpha=.24*(1-age/1.4);
 const dx=Math.cos(a)*r,dy=Math.sin(a)*r*.42;
 poly([[x+dx-rx,y+dy],[x+dx-rx*.6,y+dy-rx*.55],[x+dx+rx*.4,y+dy-rx*.7],[x+dx+rx,y+dy],[x+dx+rx*.5,y+dy+rx*.35],[x+dx-rx*.5,y+dy+rx*.4]],i%3?C.dust:C.deep,alpha);
}}
function fractures(x,y,age,seed,radius=65){if(age<0||age>2.3)return;const fade=clamp((2.3-age)/.6),grow=smooth(age/.12);
 for(let i=0;i<7;i++){const a=i*Math.PI*2/7+rand(seed+i)*.3,reach=radius*(.6+rand(seed+i+12)*.4)*grow,pts=[[x,y]];
 for(let j=1;j<=4;j++){let r=reach*j/4;pts.push([x+Math.cos(a)*r+(rand(seed+i*8+j)-.5)*8,y+Math.sin(a)*r*.44+(rand(seed+i*8+j+40)-.5)*4]);}
 line(pts,C.dark,3,fade);line(pts,C.edge,1,.45*fade);const glow=clamp(1-age/(.48+energy*.5));line(pts,C.stone,5,glow*.3);line(pts,C.cyan,1+energy,glow*.9);if(energy&&age<.25)line(pts.slice(2),C.white,1,glow*.7);
 }}
function shock(x,y,age,power=1){if(age<0||age>.3)return;const r=(10+age*150)*power,a=1-age/.3;
 for(let i=0;i<12;i++){let angle=i*Math.PI/6,pts=[];for(let j=0;j<=3;j++){let q=angle+j*.09;pts.push([x+Math.cos(q)*r,y+Math.sin(q)*r*.38]);}line(pts,C.cyan,3,a*.18);line(pts,i%4?C.cyan:C.gold,age<.06?2:1,a);}
 if(age<.055)poly([[x-38*power,y],[x-4,y-4],[x,y-24],[x+4,y-4],[x+38*power,y],[x+4,y+4],[x,y+9],[x-4,y+4]],C.white,.9);
}
function coreGlow(x,y,power){if(power<=0)return;ellipse(x,y,19,10,C.cyan,.1*power);ellipse(x,y,12,6,C.cyan,.12*power);line([[x-11,y-4],[x-7,y+2],[x,y+5],[x+7,y+2],[x+11,y-4]],C.cyan,2,.75*power);}
function phasePower(time){return mode==="core"?smooth((time-impact)/.45):bossPhase-1;}
function floatingArmor(time,pose){if(energy<=0)return;for(let side of [-1,1])for(let i=0;i<(reduced?1:3);i++){
 const x=250+side*(41+i*7),y=83+i*18+Math.sin(time*2.5+i*2)*3;
 if(!reduced)line([[pose.coreX+side*13,pose.coreY+i*2],[x-side*4,y+3]],C.cyan,1,.12*energy);
 shard(x,y,3.5-i*.4,side*.7+Math.sin(time+i)*.12,energy*.9);
}}
function bossPose(time){const a=time-impact;let row=0,frame=Math.floor(time*6)%8,sheet=images.motion,dy=0;
 if(time>=.55&&time<impact){if(mode==="sweep"){sheet=images.sweep;frame=Math.min(3,Math.floor(smooth((time-.55)/.85)*4));row=0;}
 else {row=2;frame=Math.min(7,Math.floor(smooth((time-.55)/.82)*8));dy=mode==="fault"?4*smooth((time-1.15)/.5):0;}}
 else if(a>=0&&a<.28){if(mode==="sweep"){sheet=images.sweep;frame=4+Math.min(1,Math.floor(a/.14));row=0;}else{row=3;frame=mode==="core"?1:Math.min(7,Math.floor(a/.18*8));dy=mode==="core"?-3:3;}}
 else if(a>=.28&&a<2){if(mode==="sweep"){sheet=images.sweep;frame=6+Math.min(1,Math.floor((a-.28)/1.5));row=0;}else{row=4;frame=Math.min(7,Math.floor(smooth((a-.28)/1.5)*8));}}
 const [cx,cy]=coreAnchors[sheet===images.sweep?"sweep":"motion"][row][frame];
 return {sheet,row,frame,dy,coreX:186+cx,coreY:36+Math.round(dy)+cy};}
function drawBoss(time,poseOverride=null,exposed=false){const a=time-impact,p=poseOverride||bossPose(time),bx=250,by=124;
 ellipse(bx,by+25,42,13,"#10151b",.65);
 floatingArmor(time,p);
 ctx.drawImage(p.sheet,p.frame*128,p.row*128,128,128,bx-64,by-88+Math.round(p.dy),128,128);
 const glow=poseOverride?(exposed?1:0):mode==="core"?(time<impact?smooth((time-.6)/1):.9):a>.25&&a<1.9?Math.sin((a-.25)/1.65*Math.PI):0;
 coreGlow(p.coreX,p.coreY,Math.max(glow,exposed?1:0,energy*(.8+Math.sin(time*3)*.12)));
 if(energy>0||(mode==="core"&&time>.65)){const strength=Math.max(energy*.85,time<impact?smooth((time-.65)/.9):clamp(1-(a-.15)/1.8));
 for(let side of [-1,1])line([[p.coreX+side*13,p.coreY-10],[p.coreX+side*24,p.coreY-3],[p.coreX+side*19,p.coreY+6],[p.coreX+side*28,p.coreY+17]],C.cyan,1,strength);}
}
function playerPosition(time){let x=260,y=190,dodgeStart=1.2,dodgeEnd=1.53,dx=168,dy=200;
 if(mode==="sweep"){x=315;y=170;dx=303;dy=96;dodgeStart=1.07;dodgeEnd=1.52;}
 if(mode==="fault"){x=375;y=232;dx=402;dy=163;dodgeStart=1.43;dodgeEnd=1.86;}
 if(mode==="core"){x=250;y=212;dx=250;dy=244;dodgeStart=.9;dodgeEnd=1.5;}
 let p=smooth((time-dodgeStart)/(dodgeEnd-dodgeStart));let px=mix(x,dx,p),py=mix(y,dy,p),moving=p>0&&p<1;
 if(time>2.65&&mode!=="core"){let q=smooth((time-2.65)/.8);px=mix(dx,mode==="fault"?292:252,q);py=mix(dy,mode==="sweep"?112:170,q);moving=q<1;}
 return{x:px,y:py,moving,dash:time>=dodgeStart&&time<=dodgeEnd,dx:dx-x,dy:dy-y};}
function drawPlayer(time,p){ellipse(p.x,p.y+15,16,5,"#07181b",.55);const frame=Math.floor(time*(p.dash?22:6))%(p.moving?4:8),sheet=p.moving?images.slimeMove:images.slimeIdle;
 let row=p.moving?(p.dash?(Math.abs(p.dx)>Math.abs(p.dy)?10:p.dy<0?11:5):p.dy<0?8:6):0;
 const draw=(x,y,alpha)=>{ctx.save();ctx.globalAlpha=alpha;ctx.drawImage(sheet,frame*48,row*48,48,48,Math.round(x)-24,Math.round(y)-23,48,48);ctx.restore();};
 if(p.dash&&!reduced)for(let i=2;i>=1;i--)draw(p.x-(p.dx/Math.max(1,Math.hypot(p.dx,p.dy)))*i*8,p.y-(p.dy/Math.max(1,Math.hypot(p.dx,p.dy)))*i*8,.17/i);
 draw(p.x,p.y,1);}
function windup(time){if(time<.65||time>impact)return;const p=smooth((time-.65)/.9),n=reduced?3:6;
 for(let i=0;i<n;i++){let a=i*Math.PI*2/n,r=32+rand(i+9)*15,x=250+Math.cos(a)*r,y=141+Math.sin(a)*r*.4;
 shard(x,y-p*(6+rand(i+8)*12),1.2+rand(i+12)*1.2,a,.75*p);
 line([[x,y-p*7],[mix(x,250,p*.5),mix(y,101,p*.6)]],C.cyan,1,p*(.16+energy*.18));}
 const pose=bossPose(time);coreGlow(pose.coreX,pose.coreY,p*(.3+energy*.4));
 if(mode==="sweep")line([[217,154],[206,163],[207,168]],C.cyan,1,p*.8);
 if(mode==="fault")line([[260,152],[270,154],[274,159],[284,159]],C.cyan,1,p*.8);
}
function groundFx(time){const a=time-impact;if(mode==="hammer"){fractures(250,168,a,42,85);shock(250,168,a,1.2);}
 if(mode==="sweep"&&a>=0&&a<1.8){let end=Math.PI*(1-clamp(a/.25)),pts=[];
 for(let angle=Math.PI;angle>=end;angle-=.08)pts.push([250+Math.cos(angle)*88,146+Math.sin(angle)*43]);if(pts.length>1)line(pts,C.dark,4,clamp(1-(a-.4)/1.3));}
 if(mode==="fault"&&a>=0){for(let i=0;i<7;i++){let age=a-i*.095;if(age>=0)fractures(267+i*18,164+i*12,age,92+i,21);}}
 if(mode==="core"&&a>=0){shock(250,151,a,2.2);fractures(250,151,a,121,105);}
}
function spike(x,y,age,seed){if(age<0||age>1)return;let rise=smooth(age/.075)*(1-smooth((age-.38)/.45)),h=(18+rand(seed)*24)*rise,w=5+rand(seed+2)*3;
 ellipse(x,y,w+2,3,C.dark,.7*rise);
 const tip=[x-3,y-h],left=[x-w,y-h*.38],right=[x+w,y-h*.57];
 poly([[x-w,y],left,tip,right,[x+w-1,y],[x,y+2]],C.dark,rise);
 poly([[x-w+1,y-1],[x-w+1,y-h*.38],[x-3,y-h+2],[x-1,y-h*.53],[x,y]],C.stone,rise);
 poly([[x-3,y-h+2],[x+w-1,y-h*.57],[x+w-2,y-1],[x+1,y],[x-1,y-h*.53]],C.edge,.65*rise);
 line([[x-w,y-h*.38],tip,[x+1,y-h*.8]],C.gold,1,.8*rise);
 const seam=[[x-3,y-h+3],[x-1,y-h*.53],[x+2,y-h*.3],[x,y-1]],glow=clamp(1-age/.85)*rise;
 line(seam,C.cyan,4,glow*(.1+energy*.2));line(seam,C.cyan,1+energy,glow);
 if(energy)line(seam,C.white,1,glow*.65);
}
function foregroundFx(time){const a=time-impact;
 if(mode==="hammer"){dust(250,168,a,32,1.4);debris(250,168,a,24,1.3,25);}
 if(mode==="sweep"&&a>=0){const p=clamp(a/.28),angle=Math.PI*(1-p);
 if(a<.3){let pts=[];for(let q=Math.max(0,angle);q<=Math.min(Math.PI,angle+.8);q+=.08)pts.push([250+Math.cos(q)*94,144+Math.sin(q)*45]);for(let q=Math.min(Math.PI,angle+.8);q>=Math.max(0,angle);q-=.08)pts.push([250+Math.cos(q)*72,144+Math.sin(q)*34]);poly(pts,C.stone,.55+energy*.15);const edge=pts.slice(0,Math.ceil(pts.length/2));line(edge,C.cyan,5,.15+energy*.1);line(edge,C.cyan,2,.95);line(edge.slice(0,2),C.gold,1,.95);if(energy)line(edge.slice(0,4),C.white,1,.9);}
 for(let i=0;i<6;i++){const q=Math.PI-i*Math.PI/5;dust(250+Math.cos(q)*80,146+Math.sin(q)*41,a-i*.045,210+i,.55);debris(250+Math.cos(q)*80,146+Math.sin(q)*41,a-i*.045,400+i,.55,5);}}
 if(mode==="fault"&&a>=0){for(let i=0;i<7;i++){let age=a-i*.095,x=267+i*18,y=164+i*12;spike(x,y,age,43+i);dust(x,y,age,63+i,.55);debris(x,y,age,83+i,.58,6);}}
 if(mode==="core"&&a>=0){for(let side of [-1,1]){debris(250+side*24,108,a,780+side,1.1,12);dust(250+side*31,151,a,830+side,1.1);}
 if(a<.45&&!reduced)for(let i=0;i<16;i++){let angle=i*Math.PI/8,r=20+a*160;line([[250+Math.cos(angle)*r,123+Math.sin(angle)*r*.65],[250+Math.cos(angle)*(r+9),123+Math.sin(angle)*(r+9)*.65]],C.cyan,1,1-a/.45);}}
}
function simTime(time){return time<impact?time:time<impact+.065?impact:time-.065;}
function renderAt(time){if(mode==="fight")return renderPressure();t=Math.max(0,Math.min(duration,time));const st=simTime(t),a=st-impact;energy=phasePower(st);
 ctx.imageSmoothingEnabled=false;ctx.clearRect(0,0,500,281);ctx.save();
 const shake=reduced||a<0?0:Math.max(0,1-a/.22)*(mode==="fault"?1.4:mode==="core"?2:2.6);
 ctx.translate(Math.round(Math.sin(t*143)*shake),Math.round(Math.cos(t*157)*shake*.65));ctx.drawImage(images.arena,0,0,500,281);
 groundFx(st);windup(st);const p=playerPosition(st);if(p.y<124)drawPlayer(st,p);drawBoss(st);if(p.y>=124)drawPlayer(st,p);foregroundFx(st);ctx.restore();
 const phase=t<.55?"READY":st<impact?"READ THE WIND-UP":a<.28?"COMMITTED":a<1.95?mode==="core"?"ARMOR SETTLES":"CORE EXPOSED · PUNISH":"RESET";
 const label=(energy>.5?"II · ":"I · ")+phase;if($("phase").textContent!==label)$("phase").textContent=label;$("scrub").value=t;
 return{mode,bossPhase:energy>.5?2:1,time:t,phase,reduced,player:p,impactTime:impact};}
async function enableSound(){audioContext??=new AudioContext();await audioContext.resume();if(!Object.keys(buffers).length){await Promise.all(["warden_windup","warden_slam","warden_sweep","warden_fracture","warden_strain","warden_rush"].map(async n=>{const r=await fetch(n+".wav");if(!r.ok)throw Error(n);buffers[n]=await audioContext.decodeAudioData(await r.arrayBuffer());}));}sound=true;$("sound").textContent="Sound on";$("sound").setAttribute("aria-pressed","true");}
function cue(name,gain=.45){if(!sound||!buffers[name])return;const src=audioContext.createBufferSource(),g=audioContext.createGain();src.buffer=buffers[name];g.gain.value=gain;src.connect(g).connect(audioContext.destination);src.start();}
function audioBetween(before,after){if(before<.65&&after>=.65)cue(mode==="core"?"warden_strain":"warden_windup",.3);if(before<impact&&after>=impact)cue(mode==="sweep"?"warden_sweep":mode==="fault"?"warden_fracture":"warden_slam",.5);}
function syncPhase(){document.querySelectorAll("[data-boss-phase]").forEach(b=>{b.setAttribute("aria-pressed",String(mode!=="core"&&Number(b.dataset.bossPhase)===bossPhase));b.disabled=mode==="core";});$("phase-note").textContent=mode==="core"?"Half-health transition · phase I to II":bossPhase===1?"Sealed armor · restrained cyan discharge":"Exposed core · floating violet armor · stronger cyan discharge";}
function setBossPhase(value){if(value!==1&&value!==2)throw Error("Unknown boss phase");bossPhase=value;if(mode==="fight"){const manual=pressure.manual;pressure.reset(value);pressure.manual=manual;clearPressureInput();}syncPhase();return renderAt(t);}
function select(next){if(!concepts[next])throw Error("Unknown concept");mode=next;t=0;clearPressureInput();if(mode==="fight")pressure.reset(bossPhase);$("practice").hidden=mode!=="fight";$("scrub").disabled=mode==="fight";$("scrub").hidden=mode==="fight";$("timeline").hidden=mode==="fight";const c=concepts[mode];for(const id of ["name","kicker","summary","read","response","punish","hint"])$(id).textContent=c[id];document.querySelectorAll("[data-mode]").forEach(b=>b.setAttribute("aria-pressed",String(b.dataset.mode===mode)));syncPhase();renderAt(0);}
$("play").onclick=()=>{playing=!playing;$("play").textContent=playing?"Pause":"Play";};$("replay").onclick=()=>{t=0;if(mode==="fight"){const manual=pressure.manual;pressure.reset(bossPhase);pressure.manual=manual;clearPressureInput();}playing=true;$("play").textContent="Pause";};$("slow").onclick=()=>{slow=!slow;$("slow").setAttribute("aria-pressed",String(slow));};
$("sound").onclick=async()=>{try{if(sound){sound=false;$("sound").textContent="Enable sound";$("sound").setAttribute("aria-pressed","false");}else await enableSound();}catch(e){$("sound").textContent="Sound unavailable";console.error(e);}};
$("scrub").oninput=e=>{playing=false;$("play").textContent="Play";renderAt(Number(e.target.value));};$("reduced").checked=reduced;$("reduced").onchange=e=>{reduced=e.target.checked;renderAt(t);};document.querySelectorAll("[data-mode]").forEach(b=>b.onclick=()=>select(b.dataset.mode));
document.querySelectorAll("[data-boss-phase]").forEach(b=>b.onclick=()=>{setBossPhase(Number(b.dataset.bossPhase));renderAt(0);});
async function start(){const paths={arena:"arena.png",motion:"warden_motion.png",sweep:"guardian_sweep.png",charge:"guardian_charge.png",slimeIdle:"slime_idle.png",slimeMove:"slime_sprite_sheet.png"};await Promise.all(Object.entries(paths).map(async([key,path])=>{let image=new Image();image.src=path;await image.decode();images[key]=image;}));$("loading").hidden=true;select(mode);
 window.guardianDraft={select,setBossPhase,renderAt,movementState(){return pressure.snapshot();},setReduced(value){reduced=!!value;$("reduced").checked=reduced;return renderAt(t);},pause(){playing=false;$("play").textContent="Play";},state(){return{mode,bossPhase,t,reduced,playing,sound,assets:Object.keys(images).length};}};
 function frame(now){const dt=Math.min(.05,(now-last)/1000)*(slow?.5:1);last=now;if(playing){if(mode==="fight"){pressure.update(dt,pressureControls());if(bossPhase!==pressure.phase){bossPhase=pressure.phase;syncPhase();}pressureInput.dash=false;for(const name of pressure.cues.splice(0))cue(name,name==="warden_windup"?.3:.45);renderPressure();}else{let before=t,after=t+dt;if(after>duration){after-=duration;before=0;}audioBetween(before,after);renderAt(after);}}requestAnimationFrame(frame);}last=performance.now();requestAnimationFrame(frame);
}
window.addEventListener("load",()=>start().catch(e=>{$("loading").textContent="Could not load the preview. Please reload.";console.error(e);}),{once:true});
