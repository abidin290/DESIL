package id.desa.dokumentasirumah;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

final class UpdateChecker {
    static final String MANIFEST_URL="https://raw.githubusercontent.com/abidin290/DESIL/main/update.json";
    interface Callback { void done(JSONObject manifest, Exception error); }
    interface DownloadCallback { void progress(int percent); void done(File apk, Exception error); }
    static void check(Context context, Callback callback) {
        new Thread(() -> {
            HttpURLConnection c=null;
            try {
                c=(HttpURLConnection)new URL(MANIFEST_URL+"?t="+System.currentTimeMillis()).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(true);
                c.setUseCaches(false); c.setRequestProperty("Cache-Control","no-cache");
                if(c.getResponseCode()!=200) throw new IOException("GitHub mengembalikan HTTP "+c.getResponseCode());
                InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream();
                byte[] b=new byte[4096]; int n; while((n=in.read(b))!=-1){ if(out.size()>128*1024) throw new IOException("Manifest terlalu besar"); out.write(b,0,n); }
                callback.done(new JSONObject(out.toString("UTF-8")),null);
            } catch(Exception e){ callback.done(null,e); }
            finally { if(c!=null)c.disconnect(); }
        },"update-check").start();
    }
    static void download(Context context,String address,String expectedSha256,DownloadCallback callback){
        new Thread(()->{
            HttpURLConnection c=null;File temp=null;
            try{
                URL url=new URL(address);
                if(!"https".equalsIgnoreCase(url.getProtocol()))throw new IOException("Alamat APK harus menggunakan HTTPS.");
                File dir=new File(context.getFilesDir(),"updates");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Folder pembaruan tidak dapat dibuat.");
                temp=new File(dir,"app-update.part");File target=new File(dir,"app-update.apk");
                if(target.exists()&&!target.delete())throw new IOException("Berkas pembaruan lama tidak dapat diganti.");
                c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(true);c.setUseCaches(false);
                c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("Accept","application/vnd.android.package-archive, application/octet-stream");
                int response=c.getResponseCode();if(response!=200)throw new IOException("Server unduhan mengembalikan HTTP "+response);
                long total=c.getContentLengthLong();if(total>100L*1024*1024)throw new IOException("Ukuran APK melebihi batas 100 MB.");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");long received=0;int last=-1;
                try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                    byte[] buffer=new byte[16*1024];int n;
                    while((n=in.read(buffer))!=-1){received+=n;if(received>100L*1024*1024)throw new IOException("Ukuran APK melebihi batas 100 MB.");out.write(buffer,0,n);digest.update(buffer,0,n);if(total>0){int percent=(int)Math.min(100,received*100/total);if(percent!=last){last=percent;callback.progress(percent);}}}
                    out.getFD().sync();
                }
                if(received<1024)throw new IOException("Berkas APK tidak lengkap.");
                String actual=hex(digest.digest());String expected=String.valueOf(expectedSha256==null?"":expectedSha256).trim();
                if(!expected.matches("(?i)[a-f0-9]{64}"))throw new IOException("Hash pembaruan belum tersedia.");
                if(!actual.equalsIgnoreCase(expected))throw new IOException("Hash APK tidak cocok. Unduhan dibatalkan.");
                if(!temp.renameTo(target))throw new IOException("Berkas pembaruan tidak dapat disiapkan.");
                callback.progress(100);callback.done(target,null);temp=null;
            }catch(Exception e){callback.done(null,e);}
            finally{if(c!=null)c.disconnect();if(temp!=null&&temp.exists())temp.delete();}
        },"update-download").start();
    }
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder(bytes.length*2);for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b&0xff));return out.toString();}
}
