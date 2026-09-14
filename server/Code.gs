const PHOTO_NAMES = ['Depan_Rumah.jpg','Dalam_Rumah.jpg','Samping_Kiri.jpg','Samping_Kanan.jpg','Belakang.jpg','KTP.jpg','KK.jpg','IDPEL_Listrik.jpg'];
const REQUIRED_PHOTOS = 5;
const MAX_PHOTO = 4 * 1024 * 1024;

// Run only in the owner's Apps Script editor. Never callable via doPost.
function setupPetugas() {
  const p = PropertiesService.getScriptProperties();
  if (p.getProperty('PETUGAS_JSON')) throw new Error('Petugas sudah ada. Jangan reset kode yang sedang dipakai.');
  if (!p.getProperty('ROOT_FOLDER_ID')) throw new Error('Isi Script Property ROOT_FOLDER_ID terlebih dahulu.');
  const users = [], codes = [];
  for (let i = 1; i <= 20; i++) {
    const code = Utilities.getUuid().replace(/-/g, '').toUpperCase();
    users.push({id:'P'+i, name:'Petugas '+i, hash:hash_(code), active:true});
    codes.push('Petugas '+i+': '+code);
  }
  p.setProperty('PETUGAS_JSON', JSON.stringify(users));
  console.log(codes.join('\n')); // Shown once to the owner, never returned to the APK.
}
// Run only in the owner's Apps Script editor when all access codes are lost.
// This preserves ROOT_FOLDER_ID and replaces the complete petugas list.
function resetDanBuat20Petugas() {
  const p = PropertiesService.getScriptProperties();
  if (!p.getProperty('ROOT_FOLDER_ID')) throw new Error('Isi Script Property ROOT_FOLDER_ID terlebih dahulu.');
  const users = [], codes = [];
  for (let i = 1; i <= 20; i++) {
    const code = Utilities.getUuid().replace(/-/g, '').toUpperCase();
    users.push({id:'P'+i, name:'Petugas '+i, hash:hash_(code), active:true});
    codes.push('Petugas '+i+': '+code);
  }
  p.setProperty('PETUGAS_JSON', JSON.stringify(users));
  console.log('KODE BARU - bagikan secara pribadi, log ini jangan dipublikasikan\\n' + codes.join('\\n'));
}
function hash_(value) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, value).map(b => ('0'+(b & 255).toString(16)).slice(-2)).join('');
}
function rotateKodePetugas() {
  const workerId='P1'; // Admin: ubah ke P1, P2, P3, P4 atau P5 sebelum Run.
  const p=PropertiesService.getScriptProperties();
  const users=JSON.parse(p.getProperty('PETUGAS_JSON') || '[]');
  const user=users.find(u=>u.id===workerId);
  if(!user)throw new Error('ID petugas tidak ditemukan.');
  const code=Utilities.getUuid().replace(/-/g,'').toUpperCase();
  user.hash=hash_(code);user.active=true;
  p.setProperty('PETUGAS_JSON',JSON.stringify(users));
  console.log(user.name+': '+code);
}
function fail_(message,retryable=false) { const e=new Error(message); e.publicMessage=message; e.retryable=retryable; throw e; }
function reply_(value) { return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON); }
function doGet() { return reply_({ok:true, service:'Dokumentasi Rumah Pusat', version:4}); }
function doPost(e) {
  try {
    if (!e || !e.postData || e.postData.contents.length > 5700000) fail_('Permintaan terlalu besar atau kosong.');
    return reply_(handle_(JSON.parse(e.postData.contents)));
  } catch (err) {
    return reply_({ok:false, retryable:err.retryable===true, message:err.publicMessage || 'Server belum dapat memproses. Periksa konfigurasi/kuota Drive atau coba lagi. Draf tetap tersimpan.'});
  }
}
function handle_(r) {
  const p=PropertiesService.getScriptProperties();
  const code=String(r.code || '').trim().toUpperCase();
  if (!/^[A-F0-9]{32}$/.test(code)) fail_('Kode akses salah atau dinonaktifkan.');
  const user=JSON.parse(p.getProperty('PETUGAS_JSON') || '[]').find(u=>u.active === true && u.hash === hash_(code));
  if (!user) fail_('Kode akses salah atau dinonaktifkan.');
  const root=p.getProperty('ROOT_FOLDER_ID');
  if (!root || !/^[\w-]+$/.test(root)) fail_('Folder pusat belum diatur oleh admin.');
  if (r.action === 'auth') return {ok:true, workerId:user.id, workerName:user.name};
  if (r.action === 'reserve') return {ok:true, ids:Drive.Files.generateIds({count:PHOTO_NAMES.length+1,space:'drive',type:'files'}).ids};
  if (!['begin','photo','complete'].includes(r.action)) fail_('Aksi tidak dikenal.');
  if (!/^[a-f0-9-]{36}$/.test(r.reportId || '') || !Array.isArray(r.ids) || r.ids.length<6 || r.ids.length>PHOTO_NAMES.length+1 || new Set(r.ids).size!==r.ids.length || r.ids.some(id=>typeof id!=='string'|| !/^[\w-]{10,100}$/.test(id))) fail_('ID laporan tidak valid.');
  const name=String(r.name || '').trim();
  if (!name || name.length>100) fail_('Nama KK wajib diisi, maksimal 100 karakter.');
  const lock=LockService.getScriptLock();
  if (!lock.tryLock(1000)) fail_('Server sedang melayani petugas lain. Coba upload lagi beberapa saat.',true);
  try {
    const props={reportId:r.reportId, workerId:user.id};
    let folder=get_(r.ids[0]);
    if (r.action==='begin' && !folder) {
      const metadata={id:r.ids[0],name:name,mimeType:'application/vnd.google-apps.folder',parents:[root],appProperties:{...props,complete:'false'},folderColorRgb:'#f6c5be',
        description:'Status: BELUM LENGKAP\nLaporan: '+r.reportId+'\nPetugas: '+user.name+' ('+user.id+')\nDiterima: '+new Date().toISOString()};
      for(let i=1;i<r.ids.length;i++) metadata.appProperties['photo'+i]=r.ids[i];
      folder=Drive.Files.create(metadata, null, {fields:'id,name,mimeType,parents,appProperties,trashed,description'});
    }
    if (!folder || folder.trashed || folder.mimeType!=='application/vnd.google-apps.folder' || !(folder.parents || []).includes(root) || folder.appProperties?.reportId!==r.reportId || folder.appProperties?.workerId!==user.id || folder.name!==name || r.ids.slice(1).some((id,i)=>folder.appProperties['photo'+(i+1)]!==id)) fail_('Laporan tidak cocok. Gunakan kode petugas dan server yang sama; jangan ubah folder di Drive.');
    if (r.action==='begin') {
      if(folder.appProperties.complete!=='true' && !String(folder.description||'').startsWith('Status: BELUM LENGKAP\n')){
        Drive.Files.update({description:statusDescription_(folder,'BELUM LENGKAP'),folderColorRgb:'#f6c5be',appProperties:{...folder.appProperties,complete:'false'}},folder.id);
      }
      return {ok:true};
    }
    if (r.action==='photo') {
      if (!Number.isInteger(r.slot) || r.slot<0 || r.slot>=PHOTO_NAMES.length || !r.ids[r.slot+1]) fail_('Kategori foto tidak valid.');
      if (typeof r.data!=='string' || r.data.length>Math.ceil(MAX_PHOTO/3)*4 || !/^[A-Za-z0-9+/]+={0,2}$/.test(r.data)) fail_('Data foto tidak valid atau melebihi 4 MB.');
      const bytes=Utilities.base64Decode(r.data);
      if(bytes.length<4 || bytes.length>MAX_PHOTO || (bytes[0]&255)!==255 || (bytes[1]&255)!==216 || (bytes[bytes.length-2]&255)!==255 || (bytes[bytes.length-1]&255)!==217) fail_('Foto harus berupa JPEG.');
      const digest=hash_(bytes), id=r.ids[r.slot+1], existing=get_(id);
      if (existing) {
        validatePhoto_(existing,folder.id,r,r.slot);
        if(existing.appProperties.digest!==digest) fail_('Foto berbeda dari laporan sebelumnya.');
      } else {
        Drive.Files.create({id:id,name:PHOTO_NAMES[r.slot],parents:[folder.id],appProperties:{...props,slot:String(r.slot),digest:digest}},Utilities.newBlob(bytes,'image/jpeg',PHOTO_NAMES[r.slot]),{fields:'id'});
      }
      return {ok:true,slot:r.slot};
    }
    for(let i=0;i<REQUIRED_PHOTOS;i++) validatePhoto_(get_(r.ids[i+1]),folder.id,r,i);
    Drive.Files.update({description:statusDescription_(folder,'SELESAI'),folderColorRgb:'#b3efd3',appProperties:{...folder.appProperties,complete:'true',completedAt:folder.appProperties.completedAt || new Date().toISOString()}},folder.id);
    return {ok:true,reportId:r.reportId,complete:true};
  } finally { lock.releaseLock(); }
}
function statusDescription_(folder,status){
  const body=String(folder.description || '').replace(/^Status: [^\n]*\n/,'');
  return 'Status: '+status+'\n'+body;
}
function validatePhoto_(f,parent,r,slot) {
  if(!f || f.trashed || f.name!==PHOTO_NAMES[slot] || f.mimeType!=='image/jpeg' || !(f.parents||[]).includes(parent) || f.appProperties?.reportId!==r.reportId || f.appProperties?.slot!==String(slot)) fail_('Foto laporan belum lengkap atau telah diubah. Silakan lanjutkan upload.');
}
function get_(id) {
  // Read status explicitly: quota/server errors must never be treated as missing files.
  const response=UrlFetchApp.fetch('https://www.googleapis.com/drive/v3/files/'+encodeURIComponent(id)+'?fields=id,name,mimeType,parents,appProperties,trashed,description',{
    headers:{Authorization:'Bearer '+ScriptApp.getOAuthToken()},muteHttpExceptions:true});
  if(response.getResponseCode()===404) return null;
  if(response.getResponseCode()!==200) fail_('Drive tidak dapat diakses. Periksa kuota/izin lalu coba lagi.',response.getResponseCode()===429 || response.getResponseCode()>=500);
  return JSON.parse(response.getContentText());
}
