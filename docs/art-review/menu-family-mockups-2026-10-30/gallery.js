const styles=[
{id:"01",name:"Ivory Heraldry",note:"Bold ivory silhouettes · restrained heraldic trim"},
{id:"02",name:"Moonstone",note:"Carved slate · compact gems · tactile depth"},
{id:"03",name:"Midnight Folio",note:"Storybook lettering · dark vellum · woodcut symbols"},
{id:"04",name:"Astral Seal",note:"Golden sigils · quiet magical detail"},
{id:"05",name:"Enamel Relic",note:"Embossed enamel · small sculpted medallions"}
];
const labels={board:"Complete set",menu:"Main menu",settings:"Settings",icons:"Icon set"};
const defaults={board:[0,0,1,1],menu:[.01,.075,.62,.60],settings:[.64,.075,.35,.84],icons:[.01,.70,.62,.26]};
let view="board",selected=0,manifest=window.MOCKUP_MANIFEST||{};
const gallery=document.querySelector("#gallery"),dialog=document.querySelector("#preview");
function crop(item,large=false){
 const info=manifest[item.id]||{},coords=(info.crops||{})[view]||defaults[view],[x,y,w,h]=coords;
 const frame=document.createElement("div");frame.className="crop";
 frame.style.aspectRatio=String((info.width||2560)*w/((info.height||1600)*h));
 const img=document.createElement("img");img.src="assets/board-"+item.id+".png";img.alt=item.name+" — "+labels[view];img.loading=large?"eager":"lazy";
 img.style.width=(100/w)+"%";img.style.left=(-x/w*100)+"%";img.style.top=(-y/h*100)+"%";frame.append(img);
 return frame;
}
function render(){
 document.body.dataset.view=view;gallery.replaceChildren();
 styles.forEach((item,index)=>{
 const card=document.createElement("article");card.innerHTML='<div class="card-header"><span class="number">'+item.id+'</span><h2>'+item.name+'</h2></div>';
 const button=document.createElement("button");button.type="button";button.className="media-button";button.setAttribute("aria-label","Open "+item.name+" "+labels[view]);button.append(crop(item));button.onclick=()=>open(index);card.append(button);
 const footer=document.createElement("div");footer.className="card-footer";footer.innerHTML='<span class="hint">'+item.note+'</span><span class="open-label">Open large ↗</span>';card.append(footer);gallery.append(card);
 });
}
function show(){
 const item=styles[selected];document.querySelector("#preview-title").textContent=item.id+" / "+item.name+" · "+labels[view];
 const area=document.querySelector("#large");area.replaceChildren(crop(item,true));
 const info=manifest[item.id]||{},c=(info.crops||{})[view]||defaults[view],ratio=(info.width||2560)*c[2]/((info.height||1600)*c[3]);
 area.style.width="min(100%, "+Math.round((innerHeight-145)*ratio)+"px)";
 dialog.style.width="min(96vw, "+Math.max(620,Math.round((innerHeight-145)*ratio))+"px)";
 document.querySelector("#source").href="assets/board-"+item.id+".png";
}
function open(index){selected=index;show();dialog.showModal()}
document.querySelectorAll("nav button").forEach(button=>button.onclick=()=>{
 view=button.dataset.view;document.querySelectorAll("nav button").forEach(b=>b.setAttribute("aria-pressed",String(b===button)));render();
});
document.querySelector("#close").onclick=()=>dialog.close();
document.querySelector("#previous").onclick=()=>{selected=(selected+4)%5;show()};
document.querySelector("#next").onclick=()=>{selected=(selected+1)%5;show()};
dialog.addEventListener("click",event=>{if(event.target===dialog)dialog.close()});
document.addEventListener("keydown",event=>{if(!dialog.open)return;if(event.key==="ArrowLeft"){event.preventDefault();selected=(selected+4)%5;show()}if(event.key==="ArrowRight"){event.preventDefault();selected=(selected+1)%5;show()}});
addEventListener("resize",()=>{if(dialog.open)show()});
render();
