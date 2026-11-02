'use strict';
const dialog=document.querySelector('#preview');
const preview=document.querySelector('#preview-content');
const concepts=[...document.querySelectorAll('.concept')];
const favoriteKey='b2bj-loading-concepts-2026-10-30';
let favorites=[];
try{const saved=JSON.parse(localStorage.getItem(favoriteKey)||'[]');if(Array.isArray(saved))favorites=saved.filter(id=>concepts.some(c=>c.id===id));}catch{}
function showFavorites(){
  for(const card of concepts){const selected=favorites.includes(card.id);const button=card.querySelector('.favorite');button.setAttribute('aria-pressed',String(selected));button.textContent=selected?'★ Saved favorite':'Save favorite';}
  document.querySelector('#saved-note').textContent=favorites.length?'Favorites: '+concepts.filter(c=>favorites.includes(c.id)).map(c=>c.dataset.number+' '+c.querySelector('h2').textContent).join(' · '):'Save any favorites to compare later.';
}
showFavorites();
document.querySelectorAll('[data-filter]').forEach(button=>button.addEventListener('click',()=>{
  document.body.dataset.filter=button.dataset.filter;
  document.querySelectorAll('[data-filter]').forEach(other=>other.setAttribute('aria-pressed',String(other===button)));
}));
document.querySelector('#motion').addEventListener('click',event=>{
  const paused=document.body.classList.toggle('paused');event.currentTarget.setAttribute('aria-pressed',String(paused));event.currentTarget.textContent=paused?'Play motion':'Pause motion';
});
document.querySelectorAll('.favorite').forEach(button=>button.addEventListener('click',()=>{
  const id=button.closest('.concept').id;favorites=favorites.includes(id)?favorites.filter(f=>f!==id):[...favorites,id];
  try{localStorage.setItem(favoriteKey,JSON.stringify(favorites));}catch{}
  showFavorites();
}));
document.querySelectorAll('.open-preview').forEach(button=>button.addEventListener('click',()=>{
  const card=button.closest('.concept');const scene=button.firstElementChild;
  document.querySelector('#preview-title').textContent=card.dataset.number.padStart(2,'0')+' / '+card.querySelector('h2').textContent+' / '+(scene.classList.contains('scene')?'Loading screen':'Title logo');
  preview.replaceChildren(scene.cloneNode(true));dialog.showModal();
}));
document.querySelector('#close-preview').addEventListener('click',()=>dialog.close());
dialog.addEventListener('click',event=>{if(event.target===dialog){const b=dialog.getBoundingClientRect();if(event.clientX<b.left||event.clientX>b.right||event.clientY<b.top||event.clientY>b.bottom)dialog.close();}});
dialog.addEventListener('close',()=>preview.replaceChildren());
