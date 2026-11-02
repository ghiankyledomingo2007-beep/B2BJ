'use strict';
const concepts = [
  {id:'gatekeeper', name:'Gatekeeper', tagline:'A NEW LIFE BEYOND THE GATE', description:'<strong>The closest evolution of your current menu.</strong> A smaller logo, open scenery, light rain and a breathing cyan glow.'},
  {id:'moonlit-center', name:'Moonlit Center', tagline:'FROM A SMALL BEGINNING', description:'<strong>The quietest option.</strong> A centered title and menu, deeper shadows, drifting mist and a few distant lights.'},
  {id:'moonlit-dock', name:'Moonlit Dock', tagline:'YOUR NEXT LIFE AWAITS', description:'<strong>More room for the world.</strong> Navigation along the bottom, a slow camera drift and the gate left clear.'},
  {id:'seal-stone', name:'Seal & Stone', tagline:'CHAPTER I · RUINED OUTPOST', description:'<strong>A little more fantasy weight.</strong> A recessed stone panel, worn gold corners and warm lantern motes.'},
  {id:'through-gate', name:'Through the Gate', tagline:'ONE LIFE ENDS. ANOTHER BEGINS.', description:'<strong>A more cinematic entrance.</strong> Closer framing of the arch, a gentle camera settle and a right-hand menu.'}
];
const icons = [
  '<path d="M3 15V6h2V3h2V1h4v2h2v3h2v9h-4V7H7v8H3zm4-10h4V3H7v2z"/>',
  '<path d="M1 3h5V2h4v1h5v11h-5v1H6v-1H1V3zm2 2v7h3V5H3zm7 0v7h3V5h-3zM7 4v9h2V4H7z"/>',
  '<path d="M6 0h4v3h2V2h2v2h-1v2h3v4h-3v2h1v2h-2v-1h-2v3H6v-3H4v1H2v-2h1v-2H0V6h3V4H2V2h2v1h2V0zm0 5v1H5v4h1v1h4v-1h1V6h-1V5H6zm1 2h2v2H7V7z"/>',
  '<path d="M2 1h7v5H7V3H4v10h3v-3h2v5H2V1zm9 3 4 4-4 4V9H6V7h5V4z"/>'
];
const labels = ['Continue', 'New Journey', 'Settings', 'Quit'];
function sceneMarkup(concept) {
  const rain = Array.from({length:24}, (_,i) => `<i style="--x:${(i*37)%100}%;--speed:${1.5+(i%5)*.24}s;--delay:-${i*.47}s"></i>`).join('');
  const motes = Array.from({length:10}, (_,i) => `<i style="--x:${12+(i*29)%78}%;--y:${22+(i*17)%64}%;--speed:${4+i%4}s;--delay:-${i*.71}s"></i>`).join('');
  return `<section class="scene ${concept.id}" aria-label="${concept.name} interactive main menu">
    <div class="world"></div><div class="aura"></div><div class="shade"></div><div class="mist"></div>
    <div class="rain" aria-hidden="true">${rain}</div><div class="motes" aria-hidden="true">${motes}</div>
    ${concept.id==='seal-stone'?'<div class="stone-rail" aria-hidden="true"></div>':''}
    <div class="brand"><img src="assets/moonlit-gate-logo.png" alt="Blob to Blade" draggable="false"><small>${concept.tagline}</small></div>
    <div class="fine-rule" aria-hidden="true"></div>
    <nav class="game-nav" aria-label="${concept.name} game menu">
      ${labels.map((label,i)=>`<button type="button" class="menu-item${i===0?' selected':''}" aria-pressed="${i===0}" style="--order:${i}"><svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">${icons[i]}</svg><span>${label}${i===0?'<small>RUINED OUTPOST</small>':''}</span></button>`).join('')}
    </nav>
    <div class="scene-footer"><span>${concept.id==='moonlit-dock'?'← →':'↑ ↓'} SELECT &nbsp; ENTER CONFIRM</span><span>CHAPTER I</span></div>
  </section>`;
}
const gallery=document.querySelector('#gallery');
gallery.innerHTML=concepts.map((c,i)=>`<article class="concept" id="${c.id}"><div class="card-header"><div><span class="number">0${i+1}</span><h2>${c.name}</h2></div><button class="open-preview" type="button" aria-label="Open ${c.name} preview">Open preview ↗</button></div>${sceneMarkup(c)}<p class="description">${c.description}</p></article>`).join('')+
  '<article class="concept reference"><div class="card-header"><div><span class="number">REF</span><h2>Your chosen loading screen</h2></div><span>APPROVED</span></div><img class="reference-image" src="assets/approved-loading.png" alt="Approved Moonlit Gate loading screen: ivory and cyan logo on dark navy"><p class="description">The exact same logo in every option. Ivory lettering, cyan moon and a dark navy foundation.</p></article>';
const dialog=document.querySelector('#preview');
const preview=document.querySelector('#preview-content');
let currentConcept;
function select(button){
  for(const other of button.closest('.game-nav').querySelectorAll('.menu-item')){
    other.classList.toggle('selected',other===button);
    other.setAttribute('aria-pressed',String(other===button));
  }
}
document.addEventListener('pointerover',event=>{const b=event.target.closest('.menu-item');if(b&&event.pointerType!=='touch')select(b);});
document.addEventListener('focusin',event=>{const b=event.target.closest('.menu-item');if(b)select(b);});
document.addEventListener('click',event=>{const b=event.target.closest('.menu-item');if(b)select(b);});
document.addEventListener('keydown',event=>{
  const button=event.target.closest('.menu-item');
  if(!button||!['ArrowDown','ArrowUp','ArrowLeft','ArrowRight','Home','End'].includes(event.key))return;
  event.preventDefault();
  const buttons=[...button.closest('.game-nav').querySelectorAll('.menu-item')];
  const step=['ArrowDown','ArrowRight'].includes(event.key)?1:-1;
  const next=event.key==='Home'?0:event.key==='End'?buttons.length-1:(buttons.indexOf(button)+step+buttons.length)%buttons.length;
  buttons[next].focus();
});
document.querySelectorAll('.open-preview').forEach(button=>button.addEventListener('click',()=>{
  currentConcept=concepts.find(c=>c.id===button.closest('.concept').id);
  document.querySelector('#preview-title').textContent=`0${concepts.indexOf(currentConcept)+1} / ${currentConcept.name}`;
  preview.innerHTML=sceneMarkup(currentConcept);
  dialog.showModal();
}));
document.querySelector('#close-preview').addEventListener('click',()=>dialog.close());
dialog.addEventListener('close',()=>preview.replaceChildren());
document.querySelector('#replay').addEventListener('click',()=>{if(currentConcept)preview.innerHTML=sceneMarkup(currentConcept);});
document.querySelector('#motion').addEventListener('click',event=>{
  const paused=document.body.classList.toggle('paused');
  event.currentTarget.setAttribute('aria-pressed',String(paused));
  event.currentTarget.textContent=paused?'Play motion':'Pause motion';
});
const observer=new IntersectionObserver(entries=>{
  for(const entry of entries)entry.target.classList.toggle('offscreen',!entry.isIntersecting);
});
gallery.querySelectorAll('.scene').forEach(scene=>observer.observe(scene));
