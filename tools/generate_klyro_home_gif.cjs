// Hand-authored 160 × 100, 12-frame, 180 ms GIF for Klyro Home.
// Regenerate with: node tools/generate_klyro_home_gif.cjs
// No npm packages or external assets required.
const fs = require("node:fs");
const path = require("node:path");
function generateKlyroGif(){
const W=160,H=100,F=12;
const hex=['081F2A','0E303B','B7D6CF','407E87','00D8D9','BDCC64','1B4C57','14404B','0B3039','092832','113843','1F5660','09222D','3A6670','E2B66A','566F70','788E88','9DADA2','263E42','E1E6D9','C68759','00F0FF','071A22','183F48'];
const C={sky:0,sky2:1,star:2,dim:3,accent:4,lime:5,back:6,mid:7,front:8,trees:9,lake:10,ripple:11,cab:12,roof:13,window:14,fur:15,fur2:16,furhi:17,shadow:18,cream:19,orange:20,eye:21,black:22,hills:23};
function scene(frame){
const b=new Uint8Array(W*H).fill(C.sky);
function rect(c,x,y,w,h){x=Math.round(x);y=Math.round(y);w=Math.round(w);h=Math.round(h);for(let yy=Math.max(0,y);yy<Math.min(H,y+h);yy++)for(let xx=Math.max(0,x);xx<Math.min(W,x+w);xx++)b[yy*W+xx]=C[c];}
for(const [n,x,y] of [[0,8,16],[1,31,10],[2,58,22],[3,83,9],[4,110,25],[5,142,10],[6,150,31],[7,23,28],[8,95,18],[9,43,6],[10,71,15]]){const state=(frame+2*n)%F;rect(state<3?'accent':n%3===0?'star':'dim',x,y,state<=1?2:1,1)}
const mx=113,my=19;
for(let yy=0;yy<10;yy++)for(let xx=0;xx<10;xx++)if((xx-5)**2+(yy-5)**2<=27&&(xx-8)**2+(yy-3)**2>21)rect(xx<4?'accent':'star',mx+xx,my+yy,1,1);
function ridge(y,peaks,slope,color){for(let x=0;x<W;x+=2){let h=0;for(const [a,amp] of peaks)h=Math.max(h,amp-Math.abs(x-a)*slope);const top=Math.round(y-Math.max(0,h));rect(color,x,top,2,H-top)}}
ridge(63,[[7,15],[52,26],[87,32],[128,22],[160,14]],.7,'back');ridge(73,[[0,15],[36,27],[68,17],[109,28],[156,19]],.95,'mid');
rect('lake',0,73,W,27);
for(let x=0;x<W;x+=9)rect('trees',x,x%18===0?67:70,2,7);
rect('front',0,73,W,2);
for(const [j,x,len,y] of [[0,5,25,80],[1,45,19,86],[2,76,23,83],[3,108,28,89],[4,133,17,78]]){const drift=(frame*(j%2===0?1:-1)+16)%8;rect('ripple',x+drift,y,len,1);if(j===0||j===3)rect('dim',x+drift+6,y+3,9,1)}
rect('cab',17,62,18,11);rect('roof',15,61,22,2);rect('roof',19,58,15,3);rect('cab',20,57,3,5);
rect('window',22,65,4,5);rect('window',29,65,3,5);rect('sky2',21+frame%4,54-frame%3,2,2);
rect('front',106,71,54,7);for(const x of [108,117,139,153])rect('hills',x,68,7,2);
const x=115,g=71,tail=[0,0,1,1,2,2,2,1,0,0,-1,-1][frame];
rect('black',x-13,g-16+tail,5,5);rect('fur',x-12,g-17+tail,7,6);rect('fur2',x-9,g-14+tail,8,4);
rect('cream',x-13,g-15+tail,4,3);rect('fur',x-5,g-10,8,5);
rect('shadow',x+3,g-4,5,5);rect('shadow',x+14,g-4,5,5);rect('black',x+2,g,7,2);rect('black',x+13,g,7,2);
rect('fur',x+1,g-18,19,15);rect('fur2',x+3,g-17,10,9);rect('shadow',x+9,g-8,9,5);
const bob=(frame===6||frame===7)?1:0;
rect('shadow',x+13,g-24+bob,14,13);rect('fur2',x+14,g-25+bob,14,12);
rect('fur',x+21,g-32+bob,4,10);rect('fur2',x+23,g-34+bob,3,9);rect('orange',x+23,g-30+bob,2,5);
rect('cream',x+11,g-25+bob,3,5);rect('cream',x+10,g-30+bob,2,6);rect('cream',x+8,g-32+bob,3,3);
rect('cream',x+17,g-26+bob,2,6);rect('cream',x+17,g-32+bob,2,7);rect('cream',x+18,g-34+bob,3,3);
rect('furhi',x+22,g-18+bob,7,5);rect('cream',x+24,g-14+bob,8,4);rect('cream',x+29,g-16+bob,5,3);
rect('black',x+33,g-14+bob,2,2);rect('shadow',x+29,g-11+bob,4,1);
if(frame!==5&&frame!==6)rect('eye',x+25,g-22+bob,2,2);else rect('black',x+25,g-21+bob,3,1);
rect('lime',x+6,g-14,4,2);rect('accent',x+8,g-12,6,2);rect('accent',x+9+Math.floor(frame/3)%2,g-10,2,4);
return b;
}
const out=[];const push=(...v)=>{for(const x of v)out.push(x&255)};const str=t=>{for(let i=0;i<t.length;i++)push(t.charCodeAt(i))};const u16=x=>push(x&255,(x>>8)&255);
str('GIF89a');u16(W);u16(H);push(0xF4,0,0);
for(let i=0;i<32;i++){const h=hex[i]||'000000';push(parseInt(h.slice(0,2),16),parseInt(h.slice(2,4),16),parseInt(h.slice(4,6),16))}
push(0x21,0xff,11);str('NETSCAPE2.0');push(3,1,0,0,0);
function gifLzw(pix,min=5){const clear=1<<min,end=clear+1;let codeSize=min+1,next=end+1,dict=new Map();const res=[];let acc=0,bits=0;function emit(c){acc|=c<<bits;bits+=codeSize;while(bits>=8){res.push(acc&255);acc>>>=8;bits-=8}}function reset(){dict=new Map();next=end+1;codeSize=min+1}
emit(clear);let prefix=pix[0];for(let j=1;j<pix.length;j++){const val=pix[j];const key=prefix*32+val;const prev=dict.get(key);if(prev!==undefined){prefix=prev;continue}emit(prefix);if(next<4096){dict.set(key,next++);if(next>(1<<codeSize)&&codeSize<12)codeSize++}else{emit(clear);reset()}prefix=val}emit(prefix);emit(end);if(bits>0)res.push(acc&255);return res}
for(let i=0;i<F;i++){push(0x21,0xf9,4,0x08);u16(18);push(0,0);push(0x2c);u16(0);u16(0);u16(W);u16(H);push(0,5);const comp=gifLzw(scene(i));for(let j=0;j<comp.length;j+=255){const sl=comp.slice(j,j+255);push(sl.length,...sl)}push(0)}
push(0x3b);return new Uint8Array(out);
}
if (require.main === module) {
 const dest = path.resolve(__dirname,"../app/src/main/res/drawable-nodpi/klyro_home_landscape_loop.gif");
 fs.mkdirSync(path.dirname(dest),{recursive:true});
 fs.writeFileSync(dest,generateKlyroGif());
}
module.exports={generateKlyroGif};
