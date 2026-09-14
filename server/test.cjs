const fs=require('node:fs'),vm=require('node:vm'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const properties={}, files=new Map();let locked=false,creates=0,failAfterCreate=false,driveFailure=false;
const ctx={console,PropertiesService:{getScriptProperties:()=>({getProperty:k=>properties[k],setProperty:(k,v)=>properties[k]=v})},
 Utilities:{DigestAlgorithm:{SHA_256:1},computeDigest:(_,v)=>[...crypto.createHash('sha256').update(typeof v==='string'?v:Buffer.from(v)).digest()],base64Decode:s=>[...Buffer.from(s,'base64')],newBlob:(b,m,n)=>({b,m,n})},
 LockService:{getScriptLock:()=>({tryLock:()=>!locked,releaseLock:()=>{}})},ScriptApp:{getOAuthToken:()=> 'OWNER-TOKEN'},
 UrlFetchApp:{fetch:url=>{if(driveFailure)return{getResponseCode:()=>503};const id=url.split('/files/')[1].split('?')[0],f=files.get(id);return {getResponseCode:()=>f?200:404,getContentText:()=>JSON.stringify(f)};}},
 Drive:{Files:{generateIds:o=>({ids:Array.from({length:o.count},(_,i)=>'generated_id_'+i)}),create:(meta,blob,options)=>{assert(!files.has(meta.id));creates++;const f={...meta,mimeType:meta.mimeType||blob.m,trashed:false};files.set(meta.id,f);if(failAfterCreate){failAfterCreate=false;throw new Error('response lost');}return Object.fromEntries((options?.fields || Object.keys(f).join(',')).split(',').map(key=>[key,f[key]]));},update:(meta,id)=>Object.assign(files.get(id),meta)}}};
vm.createContext(ctx);vm.runInContext(fs.readFileSync(__dirname+'/Code.gs','utf8'),ctx);
const code='A'.repeat(32),other='B'.repeat(32);
properties.ROOT_FOLDER_ID='root';properties.PETUGAS_JSON=JSON.stringify([{id:'P1',name:'Satu',active:true,hash:ctx.hash_(code)},{id:'P2',name:'Dua',active:true,hash:ctx.hash_(other)}]);
const call=(action,extra={})=>ctx.handle_({code,action,...extra});
assert.throws(()=>ctx.handle_({code:'1234',action:'reserve'}));assert.equal(creates,0);
assert.equal(call('auth').workerId,'P1');
const report={reportId:crypto.randomUUID(),ids:call('reserve').ids,name:'Budi'};
assert.equal(report.ids.length,9);
const cleanReport={reportId:crypto.randomUUID(),ids:Array.from({length:6},(_,i)=>'clean_report_id_'+i),name:'Ani'};
call('begin',cleanReport);assert.match(files.get(cleanReport.ids[0]).description,/Petugas: Satu/);assert.match(files.get(cleanReport.ids[0]).description,/Diterima:/);
files.clear();creates=0;
failAfterCreate=true;assert.throws(()=>call('begin',report));call('begin',report);assert.equal(creates,1);
assert.match(files.get(report.ids[0]).description,/^Status: BELUM LENGKAP/);
assert.equal(files.get(report.ids[0]).appProperties.complete,'false');
assert.throws(()=>call('begin',{...report,code:other}));
assert.throws(()=>call('begin',{...report,name:'Changed'}));
assert.throws(()=>call('complete',report));
assert.throws(()=>call('photo',{...report,slot:0,data:'not-a-jpeg'}));
const data=Buffer.from([255,216,1,2,255,217]).toString('base64');
for(let i=0;i<5;i++){call('photo',{...report,slot:i,data});call('photo',{...report,slot:i,data});}
call('photo',{...report,slot:5,data});call('photo',{...report,slot:6,data});call('photo',{...report,slot:7,data});
assert.equal(files.get(report.ids[6]).name,'KTP.jpg');assert.equal(files.get(report.ids[7]).name,'KK.jpg');assert.equal(files.get(report.ids[8]).name,'IDPEL_Listrik.jpg');
assert.equal(creates,9);assert.equal(call('complete',report).complete,true);
const completedAt=files.get(report.ids[0]).appProperties.completedAt;
call('begin',report);call('complete',report);
assert.equal(files.get(report.ids[0]).appProperties.completedAt,completedAt);
assert.match(files.get(report.ids[0]).description,/^Status: SELESAI/);
assert.equal(files.get(report.ids[0]).description.split('Status:').length,2);
assert.match(files.get(report.ids[0]).description,/Petugas: Satu/);
assert.throws(()=>call('photo',{...report,slot:0,data:Buffer.from([255,216,3,4,255,217]).toString('base64')}));
files.get(report.ids[1]).trashed=true;assert.throws(()=>call('complete',report));files.get(report.ids[1]).trashed=false;
driveFailure=true;assert.throws(()=>call('begin',report));assert.equal(creates,9);driveFailure=false;
locked=true;assert.throws(()=>call('begin',report),e=>e.retryable===true);locked=false;
properties.PETUGAS_JSON=JSON.stringify([{id:'P1',active:false,hash:ctx.hash_(code)}]);assert.throws(()=>call('photo',{...report,slot:0,data}));
console.log('PASS: auth, revocation, ownership, metadata, incomplete report, JPEG validation, retry after lost response, five photo slots, conflicting retry, trashed file, Drive error, concurrency lock.');
