// Software Canvas review of the same source rectangles/scales used on Android.
const fs = require('fs');
const path = require('path');
const { createCanvas, loadImage } = require('@napi-rs/canvas');
const root = path.resolve(__dirname, '../..');
const assets = path.join(root, 'app/src/main/assets/avatar/realistic');
const manifest = JSON.parse(fs.readFileSync(path.join(assets,'manifest.json')));
(async()=>{
const images={};for(const name of Object.keys(manifest.sheets))images[name]=await loadImage(path.join(assets,name+'.png'));
const modes=[['base',0,1],['walk',0,.94],['walk',2,.94],['dance',1,1],['dance',3,1],['poses',0,.72],['poses',1,.67],['poses',2,.62],['poses',4,1],['poses',5,1],['poses',6,1]];
for(const light of [false,true]){
 const canvas=createCanvas(1800,1200),ctx=canvas.getContext('2d');ctx.fillStyle=light?'#eff2f7':'#0b1423';ctx.fillRect(0,0,1800,1200);
 for(let j=0;j<modes.length;j++){
  const [name,index,scale]=modes[j],[l,t,r,b]=manifest.sheets[name].frames[index];const x=(j%6)*300+150,y=Math.floor(j/6)*600+560;
  let h=520*scale*(b-t)/manifest.sheets[name].frameHeights[index],w=h*(r-l)/(b-t);if(w>520*.76){h*=520*.76/w;w=520*.76}
  ctx.save();ctx.translate(x,y);ctx.drawImage(images[name],l,t,r-l,b-t,-w/2,-h,w,h);ctx.restore();ctx.fillStyle=light?'#123':'#a8d5e8';ctx.font='20px sans-serif';ctx.fillText(`${name} ${index}`,x-60,y+28);
 }
 const output=path.join(root,'build/sprite-review');fs.mkdirSync(output,{recursive:true});fs.writeFileSync(path.join(output,light?'light.png':'dark.png'),canvas.toBuffer('image/png'));
}
console.log('Canvas review rendered on dark and light backgrounds.');
})().catch(e=>{console.error(e);process.exit(1)});
