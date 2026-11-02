"use strict";
// Run verifyGuardianDraft() in this preview's browser console after assets load.
window.verifyGuardianDraft=()=>{
 const api=window.guardianDraft,saved=api.state(),result=[];
 const canvas=document.getElementById("scene"),ctx=canvas.getContext("2d");api.pause();
 try{
  for(const mode of ["hammer","sweep","fault","core"]){
   api.select(mode);let firstPhaseFrame,firstPhasePath;
   for(const bossPhase of mode==="core"?[1]:[1,2]){
    api.setBossPhase(bossPhase);api.setReduced(false);const hashes=[],path=[];
    for(const time of [0,.8,1.5,1.65,1.85,2.16,2.8,4.6]){
     const state=api.renderAt(time);path.push(state.player);
     const pixels=ctx.getImageData(0,0,500,281).data;let hash=2166136261;
     for(let i=0;i<pixels.length;i+=4)hash=Math.imul(hash^pixels[i]^(pixels[i+1]<<8)^(pixels[i+2]<<16),16777619);
     hashes.push(hash>>>0);
    }
    if(new Set(hashes).size<6)throw Error(mode+": animation is not progressing");
    api.renderAt(1.92);const normal=canvas.toDataURL();
    if(bossPhase===1){firstPhaseFrame=normal;firstPhasePath=JSON.stringify(path);}
    else{
     if(normal===firstPhaseFrame)throw Error(mode+": phases look identical");
     if(JSON.stringify(path)!==firstPhasePath)throw Error(mode+": phase changed the approved dodge path");
    }
    api.setReduced(true);
    if(normal===canvas.toDataURL())throw Error(mode+": reduced effects are missing");
    result.push({mode,bossPhase,distinctFrames:new Set(hashes).size,reducedDiffers:true});
   }
  }
  api.select("core");
  if(api.renderAt(.8).bossPhase!==1||api.renderAt(2.3).bossPhase!==2)throw Error("Core transition did not change phase");
  if([...document.querySelectorAll("[data-boss-phase]")].some(b=>!b.disabled))throw Error("Phase buttons active during transition");
  api.select("hammer");
  if([...document.querySelectorAll("[data-boss-phase]")].some(b=>b.disabled))throw Error("Phase buttons stayed disabled");
  if(document.documentElement.scrollWidth>innerWidth)throw Error("Horizontal page overflow");
  return result;
 }finally{api.select(saved.mode);api.setBossPhase(saved.bossPhase);api.setReduced(saved.reduced);api.renderAt(saved.t);}
};
