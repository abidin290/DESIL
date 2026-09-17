const crypto = require('node:crypto');

const PHOTO_NAMES = ['Depan_Rumah.jpg','Dalam_Rumah.jpg','Samping_Kiri.jpg','Samping_Kanan.jpg','Belakang.jpg','KTP.jpg','KK.jpg','IDPEL_Listrik.jpg'];
const MAX_PHOTO = 4 * 1024 * 1024;
const OBJECT_PREFIX = 'obj_';
let cachedToken='', tokenExpiresAt=0, tokenRequest=null, cachedS3=null, cachedS3Signature='';

function fail(message, status=400, retryable=false) { const e=new Error(message);e.status=status;e.retryable=retryable;throw e; }
function sha256(value) { return crypto.createHash('sha256').update(value).digest('hex'); }
function petugas() { try{return JSON.parse(process.env.PETUGAS_JSON || '[]');}catch(_){fail('Konfigurasi petugas tidak valid.',500);} }
function authenticate(code) {
  code=String(code||'').trim().toUpperCase();
  if(!/^[A-F0-9]{32}$/.test(code))fail('Kode akses salah atau dinonaktifkan.',401);
  const hash=sha256(code); const user=petugas().find(u=>u.active===true && u.hash===hash);
  if(!user)fail('Kode akses salah atau dinonaktifkan.',401); return user;
}
function storageDriver(){return String(process.env.STORAGE_DRIVER||'drive').trim().toLowerCase()==='s3'?'s3':'drive';}
function isObjectReport(r){return Array.isArray(r.ids)&&typeof r.ids[0]==='string'&&r.ids[0].startsWith(OBJECT_PREFIX);}
function objectKey(reportId,file){return 'laporan/'+reportId+'/'+file;}
function requiredEnv(keys,label){for(const key of keys)if(!process.env[key])fail('Environment Variable '+key+' belum diisi untuk '+label+'.',500);}

async function accessToken(){
  if(cachedToken&&Date.now()<tokenExpiresAt-60000)return cachedToken;
  if(tokenRequest)return tokenRequest;
  tokenRequest=(async()=>{
  requiredEnv(['GOOGLE_CLIENT_ID','GOOGLE_CLIENT_SECRET','GOOGLE_REFRESH_TOKEN'],'Google Drive');
  const body=new URLSearchParams({client_id:process.env.GOOGLE_CLIENT_ID,client_secret:process.env.GOOGLE_CLIENT_SECRET,refresh_token:process.env.GOOGLE_REFRESH_TOKEN,grant_type:'refresh_token'});
  const r=await fetch('https://oauth2.googleapis.com/token',{method:'POST',headers:{'content-type':'application/x-www-form-urlencoded'},body});
  const j=await r.json().catch(()=>({}));
  if(!r.ok||!j.access_token){console.error('Google OAuth token error:',j.error||r.status,j.error_description||'');if(j.error==='invalid_client')fail('GOOGLE_CLIENT_ID dan GOOGLE_CLIENT_SECRET tidak cocok atau sudah dicabut.',500);if(j.error==='invalid_grant')fail('GOOGLE_REFRESH_TOKEN tidak valid, sudah dicabut, kedaluwarsa, atau dibuat dengan OAuth Client yang berbeda.',500);fail('Google OAuth menolak permintaan token. Periksa Vercel Function Logs.',503,true);}
  cachedToken=j.access_token;tokenExpiresAt=Date.now()+Math.max(300,Number(j.expires_in)||3600)*1000;return cachedToken;
  })();try{return await tokenRequest;}finally{tokenRequest=null;}
}
async function drive(path, options={}){
  const token=await accessToken(); const r=await fetch('https://www.googleapis.com'+path,{...options,headers:{...(options.headers||{}),authorization:'Bearer '+token}});
  if(r.status===404)return null; const text=await r.text(); let data={};try{data=text?JSON.parse(text):{};}catch(_){}
  if(!r.ok){console.error('Google Drive API error:',r.status,data.error?.message||'');if(r.status===403)fail('Google Drive API menolak akses. Pastikan Drive API aktif dan OAuth memiliki izin Drive.',500);fail(r.status===429?'Kuota Google Drive sedang penuh. Coba lagi.':'Google Drive belum dapat memproses permintaan.',r.status>=500||r.status===429?503:400,r.status>=500||r.status===429);} return data;
}
async function getFile(id){return drive('/drive/v3/files/'+encodeURIComponent(id)+'?fields=id,name,mimeType,parents,appProperties,trashed,md5Checksum,description&supportsAllDrives=true');}
function validateReport(r,user){
  if(!/^[a-f0-9-]{36}$/.test(r.reportId||'')||!Array.isArray(r.ids)||r.ids.length<6||r.ids.length>9||new Set(r.ids).size!==r.ids.length||r.ids.some(id=>typeof id!=='string'||!/^[-\w]{10,100}$/.test(id)))fail('ID laporan tidak valid.');
  const name=String(r.name||'').trim();if(!name||name.length>100)fail('Nama KK wajib diisi, maksimal 100 karakter.');return name;
}
function matchesFolder(folder,r,user,name){return folder&&!folder.trashed&&folder.mimeType==='application/vnd.google-apps.folder'&&(folder.parents||[]).includes(process.env.GOOGLE_ROOT_FOLDER_ID)&&folder.appProperties?.reportId===r.reportId&&folder.appProperties?.workerId===user.id&&folder.name===name;}
async function createFolder(r,user,name){
  requiredEnv(['GOOGLE_ROOT_FOLDER_ID'],'Google Drive');
  const metadata={id:r.ids[0],name,mimeType:'application/vnd.google-apps.folder',parents:[process.env.GOOGLE_ROOT_FOLDER_ID],appProperties:{reportId:r.reportId,workerId:user.id,complete:'false'},description:'Status: BELUM LENGKAP\nLaporan: '+r.reportId+'\nPetugas: '+user.name+' ('+user.id+')\nDiterima: '+new Date().toISOString()};
  return drive('/drive/v3/files?supportsAllDrives=true&fields=id',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(metadata)});
}
function photoBytes(r){
  let bytes=r._bytes;try{if(!Buffer.isBuffer(bytes))bytes=Buffer.from(String(r.data||''),'base64');}catch(_){fail('Data foto tidak valid.');}
  if(bytes.length<4||bytes.length>MAX_PHOTO||bytes[0]!==0xff||bytes[1]!==0xd8||bytes[bytes.length-2]!==0xff||bytes[bytes.length-1]!==0xd9)fail('Foto harus berupa JPEG yang valid, maksimal 4 MB.');
  return bytes;
}
function photoSlot(r){const slot=Number(r.slot);if(!Number.isInteger(slot)||slot<0||slot>=PHOTO_NAMES.length||!r.ids[slot+1])fail('Slot foto tidak valid.');return slot;}
async function uploadDrivePhoto(r,user,name){
  const slot=photoSlot(r),bytes=photoBytes(r);
  const folder=await getFile(r.ids[0]);if(!matchesFolder(folder,r,user,name))fail('Laporan tidak cocok. Gunakan kode petugas dan server yang sama.');
  const existing=await getFile(r.ids[slot+1]);const digest=crypto.createHash('md5').update(bytes).digest('hex');
  if(existing){if(existing.name!==PHOTO_NAMES[slot]||!(existing.parents||[]).includes(r.ids[0])||existing.md5Checksum!==digest)fail('Foto yang tersimpan berbeda. Jangan mengubah laporan di Drive.');return;}
  const boundary='desil_'+crypto.randomBytes(12).toString('hex');
  const meta=JSON.stringify({id:r.ids[slot+1],name:PHOTO_NAMES[slot],parents:[r.ids[0]],appProperties:{reportId:r.reportId,workerId:user.id,slot:String(slot)}});
  const body=Buffer.concat([Buffer.from('--'+boundary+'\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n'+meta+'\r\n--'+boundary+'\r\nContent-Type: image/jpeg\r\n\r\n'),bytes,Buffer.from('\r\n--'+boundary+'--')]);
  await drive('/upload/drive/v3/files?uploadType=multipart&supportsAllDrives=true&fields=id',{method:'POST',headers:{'content-type':'multipart/related; boundary='+boundary},body});
}
async function completeDrive(r,user,name){
  const folder=await getFile(r.ids[0]);if(!matchesFolder(folder,r,user,name))fail('Laporan tidak cocok.');
  const files=await Promise.all(Array.from({length:5},(_,i)=>getFile(r.ids[i+1])));
  for(let i=0;i<5;i++){const f=files[i];if(!f||f.trashed||f.name!==PHOTO_NAMES[i]||!(f.parents||[]).includes(r.ids[0]))fail('Laporan belum lengkap. Lanjutkan upload foto yang belum diterima.',409,true);}
  const completedAt=folder.appProperties?.completedAt||new Date().toISOString();
  const metadata={appProperties:{...folder.appProperties,complete:'true',completedAt},description:'Status: SELESAI\nLaporan: '+r.reportId+'\nPetugas: '+user.name+' ('+user.id+')\nSelesai: '+completedAt};
  await drive('/drive/v3/files/'+encodeURIComponent(r.ids[0])+'?supportsAllDrives=true&fields=id',{method:'PATCH',headers:{'content-type':'application/json'},body:JSON.stringify(metadata)});return completedAt;
}

function s3Config(){
  requiredEnv(['S3_ENDPOINT','S3_REGION','S3_BUCKET','S3_ACCESS_KEY_ID','S3_SECRET_ACCESS_KEY'],'Object Storage');
  return {endpoint:process.env.S3_ENDPOINT.replace(/\/$/,''),region:process.env.S3_REGION,bucket:process.env.S3_BUCKET,accessKeyId:process.env.S3_ACCESS_KEY_ID,secretAccessKey:process.env.S3_SECRET_ACCESS_KEY,forcePathStyle:String(process.env.S3_FORCE_PATH_STYLE||'true').toLowerCase()!=='false'};
}
function s3(){
  const c=s3Config(),signature=JSON.stringify(c);
  if(!cachedS3||cachedS3Signature!==signature){
    const {S3Client}=require('@aws-sdk/client-s3');
    cachedS3=new S3Client({endpoint:c.endpoint,region:c.region,forcePathStyle:c.forcePathStyle,credentials:{accessKeyId:c.accessKeyId,secretAccessKey:c.secretAccessKey}});cachedS3Signature=signature;
  }
  return {client:cachedS3,config:c};
}
function s3Missing(e){return e?.name==='NotFound'||e?.name==='NoSuchKey'||e?.$metadata?.httpStatusCode===404;}
async function s3Head(key){
  const {HeadObjectCommand}=require('@aws-sdk/client-s3'),{client,config}=s3();
  try{return await client.send(new HeadObjectCommand({Bucket:config.bucket,Key:key}));}catch(e){if(s3Missing(e))return null;throw e;}
}
async function streamText(stream){const chunks=[];for await(const chunk of stream)chunks.push(Buffer.from(chunk));return Buffer.concat(chunks).toString('utf8');}
async function s3GetJson(key){
  const {GetObjectCommand}=require('@aws-sdk/client-s3'),{client,config}=s3();
  try{const out=await client.send(new GetObjectCommand({Bucket:config.bucket,Key:key}));const text=typeof out.Body?.transformToString==='function'?await out.Body.transformToString():await streamText(out.Body);return JSON.parse(text);}catch(e){if(s3Missing(e))return null;throw e;}
}
async function s3Put(key,body,contentType,metadata={}){
  const {PutObjectCommand}=require('@aws-sdk/client-s3'),{client,config}=s3();
  return client.send(new PutObjectCommand({Bucket:config.bucket,Key:key,Body:body,ContentType:contentType,Metadata:metadata}));
}
function matchesManifest(m,r,user,name){return m&&m.reportId===r.reportId&&m.workerId===user.id&&m.name===name&&Array.isArray(m.ids)&&m.ids.length===r.ids.length&&m.ids.every((id,i)=>id===r.ids[i]);}
async function objectManifest(r){return s3GetJson(objectKey(r.reportId,'manifest.json'));}
async function beginObject(r,user,name){
  if(r.ids.some(id=>!id.startsWith(OBJECT_PREFIX)))fail('ID Object Storage tidak valid.');
  const current=await objectManifest(r);if(current){if(!matchesManifest(current,r,user,name))fail('Laporan tidak cocok.');return;}
  const manifest={version:1,reportId:r.reportId,name,workerId:user.id,workerName:user.name,ids:r.ids,complete:false,createdAt:new Date().toISOString()};
  await s3Put(objectKey(r.reportId,'manifest.json'),JSON.stringify(manifest),'application/json',{reportid:r.reportId,workerid:user.id});
}
async function uploadObjectPhoto(r,user,name){
  const slot=photoSlot(r),bytes=photoBytes(r),manifest=await objectManifest(r);
  if(!matchesManifest(manifest,r,user,name))fail('Laporan tidak cocok. Gunakan kode petugas dan server yang sama.');
  const key=objectKey(r.reportId,PHOTO_NAMES[slot]),digest=sha256(bytes),existing=await s3Head(key);
  if(existing){if(existing.Metadata?.sha256!==digest||existing.Metadata?.photoid!==r.ids[slot+1])fail('Foto yang tersimpan berbeda. Jangan mengubah laporan di Object Storage.');return;}
  await s3Put(key,bytes,'image/jpeg',{sha256:digest,photoid:r.ids[slot+1],reportid:r.reportId,workerid:user.id,slot:String(slot)});
}
async function completeObject(r,user,name){
  const manifest=await objectManifest(r);if(!matchesManifest(manifest,r,user,name))fail('Laporan tidak cocok.');
  const files=await Promise.all(Array.from({length:5},(_,i)=>s3Head(objectKey(r.reportId,PHOTO_NAMES[i]))));
  for(let i=0;i<5;i++)if(!files[i]||files[i].Metadata?.photoid!==r.ids[i+1])fail('Laporan belum lengkap. Lanjutkan upload foto yang belum diterima.',409,true);
  const completedAt=manifest.completedAt||new Date().toISOString();
  await s3Put(objectKey(r.reportId,'manifest.json'),JSON.stringify({...manifest,complete:true,completedAt}),'application/json',{reportid:r.reportId,workerid:user.id,complete:'true'});return completedAt;
}
function reserveObjectIds(){return Array.from({length:9},()=>OBJECT_PREFIX+crypto.randomUUID());}

async function handle(r){
  const user=authenticate(r.code);
  if(r.action==='auth')return {ok:true,workerId:user.id,workerName:user.name};
  if(r.action==='reserve'){
    if(storageDriver()==='s3'){s3Config();return {ok:true,ids:reserveObjectIds()};}
    requiredEnv(['GOOGLE_ROOT_FOLDER_ID'],'Google Drive');const j=await drive('/drive/v3/files/generateIds?count=9&space=drive&type=files');return {ok:true,ids:j.ids};
  }
  if(!['begin','photo','complete'].includes(r.action))fail('Aksi tidak dikenal.');const name=validateReport(r,user),object=isObjectReport(r);
  if(r.action==='begin'){if(object)await beginObject(r,user,name);else{requiredEnv(['GOOGLE_ROOT_FOLDER_ID'],'Google Drive');const folder=await getFile(r.ids[0]);if(!folder)await createFolder(r,user,name);else if(!matchesFolder(folder,r,user,name))fail('Laporan tidak cocok.');}return {ok:true};}
  if(r.action==='photo'){if(object)await uploadObjectPhoto(r,user,name);else await uploadDrivePhoto(r,user,name);return {ok:true};}
  const completedAt=object?await completeObject(r,user,name):await completeDrive(r,user,name);return {ok:true,complete:true,reportId:r.reportId,completedAt};
}
module.exports=async function handler(req,res){
  res.setHeader('Cache-Control','no-store');
  if(req.method==='GET')return res.status(200).json({ok:true,service:'Dokumentasi Rumah Vercel',version:2,storage:storageDriver()});
  if(req.method!=='POST')return res.status(405).json({ok:false,message:'Metode tidak didukung.'});
  try{
    let request=req.body||{};
    if(String(req.headers['content-type']||'').startsWith('application/octet-stream')){
      const encoded=String(req.headers['x-desil-meta']||'');if(!encoded||encoded.length>12000)fail('Metadata upload tidak valid.');
      request=JSON.parse(Buffer.from(encoded,'base64url').toString('utf8'));request.code=String(req.headers['x-desil-code']||'');
      if(Buffer.isBuffer(req.body))request._bytes=req.body;
      else if(typeof req.body==='string')request._bytes=Buffer.from(req.body,'binary');
      else {const chunks=[];for await(const chunk of req)chunks.push(Buffer.from(chunk));request._bytes=Buffer.concat(chunks);}
    }
    return res.status(200).json(await handle(request));
  }
  catch(e){console.error('Backend error:',e?.name||'',e?.message||e);const transient=e?.$metadata?.httpStatusCode>=500||e?.name==='TimeoutError';return res.status(e.status||(transient?503:500)).json({ok:false,retryable:e.retryable===true||transient,message:e.status?e.message:'Server belum dapat memproses. Coba lagi.'});}
};
module.exports._test={sha256,authenticate,validateReport,storageDriver,isObjectReport,objectKey,reserveObjectIds,s3Config};
