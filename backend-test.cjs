const assert=require('node:assert/strict');
const api=require('./api/index.js');
const code='A'.repeat(32);
process.env.PETUGAS_JSON=JSON.stringify([{id:'P1',name:'Petugas 1',active:true,hash:api._test.sha256(code)}]);
assert.equal(api._test.authenticate(code).id,'P1');
assert.throws(()=>api._test.authenticate('1234'));
assert.throws(()=>api._test.authenticate('B'.repeat(32)));
const report={reportId:'12345678-1234-1234-1234-123456789abc',ids:Array.from({length:9},(_,i)=>'drive_file_id_'+i),name:'Rumah Ahmad'};
assert.equal(api._test.validateReport(report,{id:'P1'}),'Rumah Ahmad');
assert.throws(()=>api._test.validateReport({...report,name:''},{id:'P1'}));
console.log('PASS: Vercel auth hash and report validation');
(async()=>{
  let status=0,response;
  const meta=Buffer.from(JSON.stringify({...report,action:'photo',slot:0})).toString('base64url');
  await api({method:'POST',headers:{'content-type':'application/octet-stream','x-desil-code':'1234','x-desil-meta':meta},body:Buffer.from([0xff,0xd8,0xff,0xd9])},{setHeader(){},status(value){status=value;return this;},json(value){response=value;return value;}});
  assert.equal(status,401);assert.equal(response.ok,false);
  console.log('PASS: Vercel binary upload request parsing');
})().catch(error=>{console.error(error);process.exitCode=1;});
