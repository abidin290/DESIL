package id.desa.dokumentasirumah;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

final class CentralClient {
    static final String DEFAULT_ENDPOINT="https://script.google.com/macros/s/AKfycby0jklBdmBe2FATZPx2qd0Kv-N1Mu4yCdro7EHQaqN3N2QfFHCm1Agy2m55jq7OTdkd/exec";
    private final String endpoint,code;
    CentralClient(String endpoint,String code) { this.endpoint=endpoint;this.code=code; }
    static boolean validEndpoint(String s) { return s!=null && s.matches("https://script\\.google\\.com/macros/s/[A-Za-z0-9_-]+/exec"); }
    JSONObject callWithRetry(JSONObject body,RetryPolicy.Notice notice)throws Exception{
        return RetryPolicy.run(()->call(body),Thread::sleep,notice);
    }
    JSONObject call(JSONObject body) throws Exception {
        body.put("code",code);
        byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
        c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setFixedLengthStreamingMode(data.length);
        try {
            try(OutputStream out=c.getOutputStream()){out.write(data);}
            int status=c.getResponseCode();
            // Apps Script may issue more than one redirect before returning JSON.
            for(int hop=0; (status==301||status==302||status==303||status==307||status==308) && hop<5; hop++){
                String location=c.getHeaderField("Location"); if(location==null)throw new IOException("Redirect server tidak memiliki tujuan.");
                c.disconnect(); URL next=new URL(new URL(endpoint),location);
                String host=next.getHost();
                if(!"https".equals(next.getProtocol()) || !("script.googleusercontent.com".equals(host)||"script.google.com".equals(host)) || next.getUserInfo()!=null || (next.getPort()!=-1 && next.getPort()!=443)) throw new IOException("Redirect server Google tidak valid. Periksa deployment Apps Script.");
                c=(HttpURLConnection)next.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(120000);status=c.getResponseCode();
            }
            if(status==301||status==302||status==303||status==307||status==308)throw new RetryPolicy.Failure("Server terlalu banyak mengalihkan koneksi. Periksa deployment Apps Script.",true);
            if(status!=200)throw new RetryPolicy.Failure("Server merespons HTTP "+status+". Draf tetap tersimpan; coba lagi.",status==408||status==429||status>=500);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){bytes.write(b,0,n);if(bytes.size()>65536)throw new IOException("Respons server tidak valid.");}}
            JSONObject response;
            try{response=new JSONObject(bytes.toString("UTF-8"));}catch(JSONException e){throw new IOException("Server tidak mengirim JSON. Periksa URL deployment /exec dan akses Anyone.");}
            if(!response.optBoolean("ok")){
                String message=response.optString("message","Upload gagal. Draf tetap tersimpan.");
                // Older deployments have no retryable field; only their exact busy response is retried.
                boolean retryable=response.has("retryable")?response.optBoolean("retryable"):message.equals("Server sedang melayani petugas lain. Coba upload lagi beberapa saat.");
                throw new RetryPolicy.Failure(message,retryable);
            }
            return response;
        } catch(SocketTimeoutException | UnknownHostException e){throw new RetryPolicy.Failure("Koneksi terputus atau lambat. Draf tetap tersimpan; lanjutkan upload setelah internet tersedia.",true);}
        finally{c.disconnect();}
    }
    JSONObject photoBody(JSONObject report,int slot,File file)throws Exception{
        if(file.length()>4*1024*1024)throw new IOException("Foto melebihi 4 MB. Hapus draf dan ambil ulang foto.");
        return new JSONObject(report.toString()).put("action","photo").put("slot",slot).put("data",Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath())));
    }
}
