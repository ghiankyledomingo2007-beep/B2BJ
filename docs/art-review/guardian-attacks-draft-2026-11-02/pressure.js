"use strict";
// Local encounter study: positions are native canvas pixels, not game world units.
const pressureMoves={
 punch:{name:"Advancing fist",windup:.86,lock:.56,active:.18,recovery:1.25},
 sweep:{name:"Turning backhand",windup:1,lock:.68,active:.3,recovery:1.4},
 rush:{name:"Shoulder rush",windup:1.12,lock:.78,active:.55,recovery:1.75},
 fists:{name:"Detached fists",windup:1.1,lock:.72,active:.96,recovery:1.5},
 beam:{name:"Core beam",windup:1.25,lock:.91,active:.9,recovery:1.8}
};
const distance=(a,b)=>Math.hypot(a.x-b.x,a.y-b.y);
const bounded=(v,a,b)=>Math.max(a,Math.min(b,v));
const wrapped=a=>Math.atan2(Math.sin(a),Math.cos(a));
function segmentDistance(p,a,b){const dx=b.x-a.x,dy=b.y-a.y,q=bounded(((p.x-a.x)*dx+(p.y-a.y)*dy)/Math.max(1,dx*dx+dy*dy),0,1);return Math.hypot(p.x-a.x-dx*q,p.y-a.y-dy*q);}
class WardenPractice{
 constructor(phase=1){this.reset(phase);}
 reset(phase=1){
  this.phase=phase;this.elapsed=0;this.manual=false;this.end="";this.endTime=0;this.shots=[];this.effects=[];this.cues=[];
  this.player={x:385,y:215,vx:0,vy:0,hp:100,dash:0,cooldown:0,invincible:0,shot:0,dirX:-1,dirY:0};
  this.boss={x:245,y:139,hp:phase===2?600:1200,state:"pursue",time:0,move:"punch",angle:0,turn:1,locked:false,target:null,second:null,previous:"",count:0,spell:0,hit:false,hit2:false,wall:false};
 }
 update(dt,input={}){
  for(let left=Math.min(.1,Math.max(0,dt));left>1e-8;){const step=Math.min(left,1/120);this.step(step,input);input={...input,dash:false};left-=step;}
 }
 lead(horizon=.22,cap=24){const p=this.player,n=Math.max(1,Math.hypot(p.vx,p.vy)),lead=Math.min(cap,n*horizon);return{x:bounded(p.x+p.vx/n*lead,42,458),y:bounded(p.y+p.vy/n*lead,57,257)};}
 startMove(move){const b=this.boss;b.move=move;b.state="windup";b.time=0;b.locked=false;b.target=this.lead();b.second=null;b.hit=false;b.hit2=false;b.wall=false;b.angle=Math.atan2(b.target.y-b.y,b.target.x-b.x);b.previous=move;b.count++;this.cues.push(move==="beam"||move==="fists"?"warden_strain":"warden_windup");}
 chooseMove(){const b=this.boss,d=distance(b,this.player);
  if(this.phase===2&&b.count%2===1){this.startMove(b.spell++%2?"beam":"fists");return;}
  const p=this.player,escaping=((p.x-b.x)*p.vx+(p.y-b.y)*p.vy)/Math.max(1,d)>35;
  this.startMove((d>82||(escaping&&d>60))&&b.previous!=="rush"?"rush":d<92&&b.previous!=="sweep"?"sweep":"punch");
 }
 strike(x,y,kind="hit"){this.effects.push({x,y,kind,age:0});this.cues.push(kind==="rush"?"warden_rush":"warden_slam");}
 hurt(amount){const p=this.player;if(p.invincible>0||p.dash>0||this.end)return false;p.hp=Math.max(0,p.hp-amount);p.invincible=.7;this.effects.push({x:p.x,y:p.y,kind:"hurt",age:0});return true;}
 demoInput(){const p=this.player,b=this.boss,a=Math.atan2((p.y-158)/83,(p.x-250)/154)+.32,g={x:250+Math.cos(a)*154,y:158+Math.sin(a)*83},d=distance(p,g),warn=b.state==="windup"&&pressureMoves[b.move].windup-b.time<.19;
  return{mx:d>3?(g.x-p.x)/d:0,my:d>3?(g.y-p.y)/d:0,aimX:b.x,aimY:b.y,fire:this.elapsed%2.4<1.5,dash:warn&&distance(p,b)<95};
 }
 step(dt,input){
  this.elapsed+=dt;const p=this.player,b=this.boss;
  for(const f of this.effects)f.age+=dt;this.effects=this.effects.filter(f=>f.age<1.4);
  if(this.end){this.endTime+=dt;if(!this.manual&&this.endTime>2.5)this.reset(this.phase);return;}
  if(!this.manual)input=this.demoInput();
  p.cooldown=Math.max(0,p.cooldown-dt);p.invincible=Math.max(0,p.invincible-dt);p.shot=Math.max(0,p.shot-dt);
  let mx=input.mx||0,my=input.my||0,length=Math.hypot(mx,my);if(length>1){mx/=length;my/=length;}
  if(input.dash&&p.cooldown===0){if(length>0){p.dirX=mx/(length>1?1:length);p.dirY=my/(length>1?1:length);}p.dash=.16;p.cooldown=1.2;}
  const dashing=p.dash>0,speed=dashing?450:130;p.vx=(dashing?p.dirX:mx)*speed;p.vy=(dashing?p.dirY:my)*speed;
  p.x=bounded(p.x+p.vx*dt,38,462);p.y=bounded(p.y+p.vy*dt,55,258);p.dash=Math.max(0,p.dash-dt);
  const sep=distance(p,b);if(sep<42){const a=Math.atan2(p.y-b.y,p.x-b.x);p.x=bounded(b.x+Math.cos(a)*42,38,462);p.y=bounded(b.y+Math.sin(a)*42,55,258);}
  if(input.fire&&p.shot===0){const a=Math.atan2((input.aimY??b.y)-p.y,(input.aimX??b.x)-p.x);this.shots.push({x:p.x,y:p.y,vx:Math.cos(a)*270,vy:Math.sin(a)*270,age:0});p.shot=.28;}
  for(const s of this.shots){s.x+=s.vx*dt;s.y+=s.vy*dt;s.age+=dt;if(distance(s,b)<32){b.hp=Math.max(0,b.hp-(b.state==="recover"?26:8));s.age=9;}}
  this.shots=this.shots.filter(s=>s.age<1.8&&s.x>0&&s.x<500&&s.y>0&&s.y<281);
  if(!b.hp||!p.hp){this.end=b.hp?"DEFEATED · REPLAY TO RETRY":"WARDEN DEFEATED";this.endTime=0;return;}
  b.time+=dt;
  if(b.state==="pursue"){
   const aim=this.lead(),d=distance(b,aim),speed=this.phase===2?96:87;
   if(distance(b,p)>59&&d>1){b.x=bounded(b.x+(aim.x-b.x)/d*speed*dt,65,435);b.y=bounded(b.y+(aim.y-b.y)/d*speed*dt,72,232);b.angle=Math.atan2(aim.y-b.y,aim.x-b.x);}
   if(b.time>.55&&(distance(b,p)<86||b.time>(this.phase===2?.95:1.15)))this.chooseMove();
  }else if(b.state==="windup"){
   const move=pressureMoves[b.move];
   if(!b.locked){b.target=b.move==="fists"?this.lead(.45,48):this.lead();b.turn=Math.sign((p.x-b.x)*p.vy-(p.y-b.y)*p.vx)||1;b.angle=Math.atan2(b.target.y-b.y,b.target.x-b.x);if(b.move==="punch"&&distance(b,p)>60){b.x+=Math.cos(b.angle)*48*dt;b.y+=Math.sin(b.angle)*48*dt;}if(b.time>=move.lock)b.locked=true;}
   b.x=bounded(b.x,65,435);b.y=bounded(b.y,72,232);
   if(b.time>=move.windup){b.state="active";b.time=0;
    if(b.move==="punch")this.strike(b.x+Math.cos(b.angle)*48,b.y+Math.sin(b.angle)*48);
    if(b.move==="sweep")this.cues.push("warden_sweep");
    if(b.move==="rush")this.cues.push("warden_rush");
    if(b.move==="fists")b.second=this.lead(.55,60);
    if(b.move==="beam")this.cues.push("warden_fracture");
   }
  }else if(b.state==="active"){
   const move=pressureMoves[b.move],d=distance(p,b),a=wrapped(Math.atan2(p.y-b.y,p.x-b.x)-b.angle);
   if(b.move==="rush"){
    const nx=b.x+Math.cos(b.angle)*280*dt,ny=b.y+Math.sin(b.angle)*280*dt;
    b.x=bounded(nx,65,435);b.y=bounded(ny,72,232);
    if(nx!==b.x||ny!==b.y){b.wall=true;this.strike(b.x,b.y,"rush");b.state="recover";b.time=0;}
    else if(!b.hit&&distance(p,b)<54){b.hit=this.hurt(18);}
   }else if(b.move==="punch"&&!b.hit){const c={x:b.x+Math.cos(b.angle)*48,y:b.y+Math.sin(b.angle)*48};if(distance(p,c)<38)b.hit=this.hurt(14);}
   else if(b.move==="sweep"&&!b.hit&&d<91&&a>=-Math.PI*.5+clamp(b.time/move.active)*Math.PI-.7&&a<=-Math.PI*.5+clamp(b.time/move.active)*Math.PI+.12){b.hit=this.hurt(13);}
   else if(b.move==="fists"){
    for(let i=0;i<2;i++){const age=b.time-i*.48,key=i?"hit2":"hit",target=i?b.second:b.target;
     if(age>=.2&&age<.2+dt)this.strike(target.x,target.y);
     if(!b[key]&&age>=.2&&age<.35&&distance(p,target)<31)b[key]=this.hurt(14);
    }
   }else if(b.move==="beam"&&!b.hit){const angle=b.angle+b.turn*(.45-clamp(b.time/.9)*.9),end={x:b.x+Math.cos(angle)*235,y:b.y+Math.sin(angle)*235};if(d>43&&segmentDistance(p,b,end)<12)b.hit=this.hurt(18);}
   if(b.state==="active"&&b.time>=move.active){b.state="recover";b.time=0;}
  }else if(b.state==="recover"){
   const recovery=pressureMoves[b.move].recovery-(this.phase===2?.15:0)+(b.wall?.4:0);
   if(b.time>=recovery){b.state=this.phase===1&&b.hp<=600?"transition":"pursue";b.time=0;if(b.state==="transition")this.cues.push("warden_strain");}
  }else if(b.state==="transition"&&b.time>=1.4){this.phase=2;b.state="pursue";b.time=0;this.strike(b.x,b.y,"transition");}
 }
 snapshot(){return{phase:this.phase,elapsed:this.elapsed,end:this.end,manual:this.manual,player:{...this.player},boss:{...this.boss}};}
}
const pressure=new WardenPractice(),pressureInput={mx:0,my:0,fire:false,dash:false,aimX:250,aimY:140},pressureKeys=new Set();
function pressurePose(b){let row=0,frame=Math.floor(pressure.elapsed*6)%8,sheet=images.motion,dy=0;
 if(b.state==="transition"){row=2;frame=Math.min(7,Math.floor(b.time/1.4*8));}
 else if(b.state==="pursue"){row=1;frame=Math.floor(pressure.elapsed*9)%8;}
 else if(b.move==="sweep"||b.move==="rush"){
  sheet=b.move==="sweep"?images.sweep:images.charge;
  frame=b.state==="windup"?Math.min(3,Math.floor(b.time/pressureMoves[b.move].windup*4)):b.state==="active"?4+Math.min(1,Math.floor(b.time/pressureMoves[b.move].active*2)):6+Math.min(1,Math.floor(b.time/pressureMoves[b.move].recovery*2));
 }else if(b.state==="windup"){row=b.move==="beam"||b.move==="fists"?0:2;frame=Math.min(7,Math.floor(b.time/pressureMoves[b.move].windup*8));}
 else if(b.state==="active"){row=b.move==="beam"||b.move==="fists"?0:3;frame=Math.min(7,Math.floor(b.time/pressureMoves[b.move].active*8));}
 else if(b.state==="recover"){row=4;frame=Math.min(7,Math.floor(b.time/pressureMoves[b.move].recovery*8));}
 else if(b.state==="transition"){row=2;frame=Math.min(7,Math.floor(b.time/1.4*8));}
 const anchor=sheet===images.charge?[64,65]:coreAnchors[sheet===images.sweep?"sweep":"motion"][row][frame];
 return{sheet,row,frame,dy,coreX:186+anchor[0],coreY:36+anchor[1]};
}
function drawPressureBoss(){const b=pressure.boss,p=pressurePose(b);ctx.save();ctx.translate(Math.round(b.x-250),Math.round(b.y-149));drawBoss(pressure.elapsed,p,b.state==="recover");ctx.restore();}
function drawPressureFist(x,y,age){const lift=age<.2?Math.sin(clamp(age/.2)*Math.PI)*15:0;ellipse(x,y,13,5,C.dark,.55);y-=lift+10;
 poly([[x-13,y-9],[x-5,y-13],[x+3,y-11],[x+12,y-5],[x+13,y+8],[x+5,y+12],[x-11,y+7]],C.dark);
 poly([[x-11,y-7],[x-4,y-10],[x+9,y-4],[x+10,y+6],[x+4,y+9],[x-9,y+5]],C.stone);
 line([[x-11,y-7],[x-4,y-10],[x+9,y-4]],C.gold,2);for(let k=0;k<3;k++)line([[x-6+k*5,y-8+k*2],[x-7+k*5,y-3+k*2]],C.dark,2);poly([[x+7,y+1],[x+15,y+1],[x+15,y+6],[x+9,y+8]],C.stone);line([[x-6,y-2],[x-4,y+4],[x+6,y+6]],C.cyan,2);
}
function pressureAttackFx(){const b=pressure.boss,m=pressureMoves[b.move],wind=b.state==="windup",active=b.state==="active";
 if(wind&&b.move==="rush"){const a=b.angle;for(let i=0;i<3;i++)line([[b.x+Math.cos(a)*(30+i*9),b.y+Math.sin(a)*(30+i*9)],[b.x+Math.cos(a)*(34+i*9),b.y+Math.sin(a)*(34+i*9)]],C.cyan,1,.5);}
 if(active&&b.move==="sweep"){const a=b.angle-Math.PI*.5+clamp(b.time/m.active)*Math.PI,pts=[];for(let q=a-.7;q<=a;q+=.07)pts.push([b.x+Math.cos(q)*89,b.y+Math.sin(q)*70-8]);for(let q=a;q>=a-.7;q-=.07)pts.push([b.x+Math.cos(q)*67,b.y+Math.sin(q)*52-8]);poly(pts,C.stone,.65);line(pts.slice(0,11),C.cyan,2,.95);}
 if((wind||active)&&b.move==="fists")for(let i=0;i<2;i++){
  const target=i?b.second||b.target:b.target,age=active?b.time-i*.48:-.2,origin={x:b.x+(i?35:-35),y:b.y-35},q=active?smooth(age/.2):0;
  if(b.locked&&(i===0||b.second)&&age<.2){ellipse(target.x,target.y,12,4,C.dark,.6);line([[target.x-7,target.y],[target.x-2,target.y-2],[target.x+7,target.y]],C.cyan,1,.65);}
  if(age<.36){const x=mix(origin.x,target.x,q),y=mix(origin.y,target.y,q);if(!reduced)line([[origin.x,origin.y-10],[x,y-10]],C.cyan,2,.2);drawPressureFist(x,y,age);}
 }
 if((wind||active)&&b.move==="beam"){
  const pose=pressurePose(b),core={x:b.x-250+pose.coreX,y:b.y-149+pose.coreY},a=b.angle+b.turn*(.45-(active?clamp(b.time/m.active)*.9:0)),start={x:b.x+Math.cos(a)*35,y:b.y+Math.sin(a)*35},end={x:b.x+Math.cos(a)*235,y:b.y+Math.sin(a)*235};
  if(wind){ellipse(core.x,core.y,6+Math.sin(b.time*13),6,C.cyan,.7);line([[core.x,core.y],[start.x,start.y-13]],C.cyan,1,.45);}
  else{line([[core.x,core.y],[end.x,end.y-13]],C.stone,18,.6);line([[core.x,core.y],[end.x,end.y-13]],C.cyan,9,.9);line([[core.x,core.y],[end.x,end.y-13]],C.white,3,.95);}
 }
}
function renderPressure(){const b=pressure.boss,p=pressure.player;energy=pressure.phase-1;ctx.clearRect(0,0,500,281);ctx.save();
 const lastHit=pressure.effects.find(f=>f.kind!=="hurt"&&f.age<.17),shake=!reduced&&lastHit?(1-lastHit.age/.17)*1.8:0;
 ctx.translate(Math.round(Math.sin(pressure.elapsed*143)*shake),Math.round(Math.cos(pressure.elapsed*157)*shake*.6));ctx.drawImage(images.arena,0,0,500,281);
 for(const f of pressure.effects)if(f.kind!=="hurt"){fractures(f.x,f.y,f.age,17, f.kind==="transition"?70:38);shock(f.x,f.y,f.age,.65);dust(f.x,f.y,f.age,24,.6);debris(f.x,f.y,f.age,19,.6,10);}
 const player={x:p.x,y:p.y-15,moving:Math.hypot(p.vx,p.vy)>1,dash:p.dash>0,dx:p.vx,dy:p.vy};
 if(p.y<b.y)drawPlayer(pressure.elapsed,player);drawPressureBoss();if(p.y>=b.y)drawPlayer(pressure.elapsed,player);
 pressureAttackFx();for(const s of pressure.shots){line([[s.x-s.vx*.025,s.y-12-s.vy*.025],[s.x,s.y-12]],C.cyan,3,.7);ellipse(s.x,s.y-12,2,2,C.white);}
 if(p.invincible>0){ellipse(p.x,p.y,19,6,C.white,.14);}
 ctx.restore();
 const label=pressure.end||`${pressure.phase===1?"I":"II"} · ${b.state==="pursue"?"CLOSING DISTANCE":b.state==="windup"?(b.locked?"COMMITTED · DODGE":"READ THE BODY"):b.state==="recover"?"CORE OPEN · COUNTER":b.state==="transition"?"CORE UNBOUND":pressureMoves[b.move].name.toUpperCase()}`;
 $("phase").textContent=label;$("practice-status").textContent=`${pressure.manual?"YOU":"DEMO"}  HP ${p.hp}/100 · Warden ${b.hp}/1200 · Dash ${p.cooldown>0?p.cooldown.toFixed(1)+"s":"ready"}`;
 return pressure.snapshot();
}
function clearPressureInput(){pressureKeys.clear();pressureInput.mx=pressureInput.my=0;pressureInput.fire=pressureInput.dash=false;}
function pressureControls(){pressureInput.mx=Number(pressureKeys.has("KeyD")||pressureKeys.has("ArrowRight"))-Number(pressureKeys.has("KeyA")||pressureKeys.has("ArrowLeft"));pressureInput.my=Number(pressureKeys.has("KeyS")||pressureKeys.has("ArrowDown"))-Number(pressureKeys.has("KeyW")||pressureKeys.has("ArrowUp"));return pressureInput;}
window.addEventListener("keydown",e=>{if(mode!=="fight"||!pressure.manual||!playing||e.target.closest("button,input"))return;if(["KeyW","KeyA","KeyS","KeyD","ArrowUp","ArrowDown","ArrowLeft","ArrowRight","Space"].includes(e.code)){e.preventDefault();pressureKeys.add(e.code);if(e.code==="Space"&&!e.repeat)pressureInput.dash=true;}});
window.addEventListener("keyup",e=>pressureKeys.delete(e.code));window.addEventListener("blur",()=>{clearPressureInput();if(mode==="fight"&&pressure.manual){playing=false;$("play").textContent="Play";}});
canvas.addEventListener("pointermove",e=>{const r=canvas.getBoundingClientRect();pressureInput.aimX=(e.clientX-r.left)*500/r.width;pressureInput.aimY=(e.clientY-r.top)*281/r.height+12;});
canvas.addEventListener("pointerdown",e=>{if(mode==="fight"&&pressure.manual&&e.button===0){canvas.focus();pressureInput.fire=true;}});
window.addEventListener("pointerup",()=>pressureInput.fire=false);window.addEventListener("pointercancel",clearPressureInput);
$("practice-control").onclick=()=>{pressure.reset(bossPhase);pressure.manual=true;playing=true;$("play").textContent="Pause";clearPressureInput();canvas.focus();};
$("practice-demo").onclick=()=>{pressure.reset(bossPhase);playing=true;$("play").textContent="Pause";clearPressureInput();};
