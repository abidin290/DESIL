package id.desa.dokumentasirumah;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class UpdateChecker {
    static final String MANIFEST_URL="https://raw.githubusercontent.com/abidin290/DESIL/main/update.json";
    interface Callback { void done(JSONObject manifest, Exception error); }
    static void check(Context context, Callback callback) {
        new Thread(() -> {
            HttpURLConnection c=null;
            try {
                c=(HttpURLConnection)new URL(MANIFEST_URL).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(true);
                if(c.getResponseCode()!=200) throw new IOException("GitHub mengembalikan HTTP "+c.getResponseCode());
                InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream();
                byte[] b=new byte[4096]; int n; while((n=in.read(b))!=-1){ if(out.size()>128*1024) throw new IOException("Manifest terlalu besar"); out.write(b,0,n); }
                callback.done(new JSONObject(out.toString("UTF-8")),null);
            } catch(Exception e){ callback.done(null,e); }
            finally { if(c!=null)c.disconnect(); }
        },"update-check").start();
    }
}
