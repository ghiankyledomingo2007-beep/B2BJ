"use strict";
function verifyGuardianPressure(){
 const check=(ok,message)=>{if(!ok)throw Error(message);},results=[];
 const run=(s,seconds,input={})=>{for(let t=0;t<seconds-1e-8;t+=.01)s.update(Math.min(.01,seconds-t),input);};
 const manual=phase=>{const s=new WardenPractice(phase);s.manual=true;return s;};
 let s=manual(1),d=distance(s.player,s.boss);run(s,.5);check(distance(s.player,s.boss)<d-25,"Boss did not close distance");results.push("Pursuit closes distance");
 s=manual(1);s.player.vx=450;const lead=s.lead();check(distance(lead,s.player)<=24.001,"Prediction exceeded its cap");results.push("Prediction capped at 24px");
 s=manual(1);s.startMove("rush");run(s,.82);const angle=s.boss.angle,target=JSON.stringify(s.boss.target);s.player.x=80;s.player.y=65;run(s,.36);
 check(s.boss.locked&&s.boss.angle===angle&&JSON.stringify(s.boss.target)===target,"Committed rush retargeted");results.push("Rush locks and cannot steer");
 s=manual(1);s.boss.x=431;s.boss.y=160;s.player.x=460;s.player.y=160;s.startMove("rush");run(s,1.3);check(s.boss.wall&&s.boss.state==="recover","Missed wall rush has no opening");run(s,1);check(s.boss.state==="recover","Wall recovery ended too early");results.push("Wall rush gives extra recovery");
 const active=(move,phase=1)=>{const a=manual(phase);a.startMove(move);Object.assign(a.boss,{state:"active",time:move==="beam"?.44:.14,angle:0,locked:true});return a;};
 s=active("sweep");s.player.x=s.boss.x-70;s.player.y=s.boss.y;run(s,.12);check(s.player.hp===100,"Sweep hit behind the boss");
 s=active("sweep");s.player.x=s.boss.x+70;s.player.y=s.boss.y;run(s,.03);check(s.player.hp<100,"Sweep missed its visible front arc");results.push("Sweep front hits; back is safe");
 s=active("beam",2);s.player.x=s.boss.x+40;s.player.y=s.boss.y;run(s,.04);check(s.player.hp===100,"Beam hit its inner safe space");
 s=active("beam",2);s.player.x=s.boss.x+100;s.player.y=s.boss.y;run(s,.04);check(s.player.hp===82,"Beam did not hit the visible lane");run(s,.25);check(s.player.hp===82,"Beam dealt repeated tick damage");results.push("Beam lane, inner safe space and single hit");
 s=manual(2);s.startMove("fists");run(s,1.12);check(s.boss.second!==null,"Second fist did not commit");const second=JSON.stringify(s.boss.second);s.player.x=60;s.player.y=65;run(s,.2);check(JSON.stringify(s.boss.second)===second,"Second fist chased after commitment");results.push("Second fist commits before landing");
 s=manual(1);s.boss.state="recover";s.boss.time=0;s.player.x=65;s.player.y=60;s.update(.1,{mx:1,dash:true});const cooldown=s.player.cooldown;check(s.player.dash>0&&cooldown>1,"Dash did not start");s.update(.1,{mx:1,dash:true});check(s.player.cooldown<cooldown,"Holding dash bypassed cooldown");results.push("Dash cooldown cannot be bypassed");
 s=manual(1);Object.assign(s.boss,{hp:590,state:"recover",time:2});run(s,.01);check(s.boss.state==="transition"&&s.phase===1,"Half-health transition skipped");run(s,1.4);check(s.phase===2&&s.player.hp===100,"Transition damaged player or failed");results.push("Half-health transition is safe");
 const pressure=[],naiveCircling=[];
 for(const phase of [1,2]){
  s=new WardenPractice(phase);s.player.hp=10000;s.boss.hp=100000;const moves=new Set();
  for(let i=0;i<1800;i++){s.update(1/30);moves.add(s.boss.move);check([s.player.x,s.player.y,s.boss.x,s.boss.y].every(Number.isFinite),"Non-finite actor position");check(s.boss.x>=65&&s.boss.x<=435&&s.boss.y>=72&&s.boss.y<=232,"Boss left arena");}
  check(moves.has("rush")&&moves.has("punch")&&moves.has("sweep"),"Physical attacks missing from moving encounter");
  if(phase===2)check(moves.has("fists")&&moves.has("beam"),"Phase two has no new attacks");
  pressure.push({phase,moves:[...moves],damageTaken:10000-s.player.hp});
  s=manual(phase);s.player.hp=10000;s.boss.hp=100000;
  for(let i=0;i<1800;i++){const input=s.demoInput();input.dash=false;s.update(1/30,input);}
  naiveCircling.push({phase,damageTaken:10000-s.player.hp});
 }
 results.push("Both phases remain bounded for 60 seconds; full kit selected");
 check(naiveCircling[1].damageTaken>naiveCircling[0].damageTaken&&naiveCircling[0].damageTaken>0,"Constant circling escaped all pressure or phase two lost pressure");
 results.push("Constant circling takes damage; phase two adds pressure in the fixed scenario");
 s=new WardenPractice(2);const visible=new Set();
 for(let i=0;i<2400&&!s.end;i++){s.update(1/60);if(s.boss.state==="active")visible.add(s.boss.move);}
 check(visible.has("fists")&&visible.has("beam"),"Demo ended before showing both phase-two spells");
 results.push("Default phase-two demo shows both spells before victory");
 return{checks:results,circlingWithReactiveDash:pressure,naiveCircling};
}
if(typeof window!=="undefined")window.verifyGuardianPressure=verifyGuardianPressure;
