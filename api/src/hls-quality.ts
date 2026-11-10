/** Filter master variants only; retain audio/subtitle/key tags and leave media playlists untouched. */
export function limitHlsMaster(text:string,maxHeight?:number){
 if(!maxHeight)return text;const lines=text.split(/\r?\n/),variants:{tag:number;uri:number;height:number;bandwidth:number}[]=[];
 for(let i=0;i<lines.length;i++){if(!lines[i].startsWith('#EXT-X-STREAM-INF:'))continue;let next=i+1;while(next<lines.length&&(!lines[next].trim()||lines[next].startsWith('#')))next++;if(next>=lines.length)continue;const resolution=/RESOLUTION=\d+x(\d+)/i.exec(lines[i]),bw=/(?:^|[:,])BANDWIDTH=(\d+)/.exec(lines[i]);variants.push({tag:i,uri:next,height:resolution?+resolution[1]:0,bandwidth:bw?+bw[1]:Number.MAX_SAFE_INTEGER})}
 if(variants.length<2)return text;const eligible=variants.filter(v=>v.height>0&&v.height<=maxHeight&&v.bandwidth<=2500000);const selected=eligible.length?eligible.sort((a,b)=>b.height-a.height||b.bandwidth-a.bandwidth)[0]:[...variants].sort((a,b)=>a.bandwidth-b.bandwidth||a.height-b.height)[0];const removed=new Set<number>();for(const v of variants)if(v!==selected){removed.add(v.tag);removed.add(v.uri)}return lines.filter((_l,i)=>!removed.has(i)).join('\n');
}
