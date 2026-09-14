package id.desa.dokumentasirumah;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AtomicFile;
import java.io.*;
import java.security.MessageDigest;

/** Call only on the upload executor. Cache lives in filesDir, not evictable cacheDir. */
final class UploadCompressor {
    static final int MAX_EDGE=1600, MAX_BYTES=500*1024;
    static final int HOUSE_BYTES=320*1024, KTP_BYTES=550*1024, KK_BYTES=700*1024, IDPEL_BYTES=450*1024;
    private final File directory;
    UploadCompressor(File filesDir){directory=new File(filesDir,"upload_standard");}
    File prepare(File source)throws Exception{return prepare(source,null);}
    File prepare(File source,LocationStamp location)throws Exception{return prepare(source,location,new String[0]);}
    File prepare(File source,LocationStamp location,String... header)throws Exception{return prepare(source,location,MAX_BYTES,header);}
    File prepare(File source,LocationStamp location,int maxBytes,String... header)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(source)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)digest.update(buf,0,n);}
        if(location!=null){
            digest.update(Double.toString(location.latitude).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(Double.toString(location.longitude).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(Float.toString(location.accuracy).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update(Long.toString(location.time).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(header!=null)for(String line:header)digest.update((line==null?"":line).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        digest.update(Integer.toString(maxBytes).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder key=new StringBuilder(location==null?"standard4-":"standard4gps-");for(byte b:digest.digest())key.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Penyimpanan kompresi tidak tersedia.");
        File result=new File(directory,key+".jpg");
        AtomicFile dest=new AtomicFile(result);
        // AtomicFile also restores a previous valid file after an interrupted write.
        try(InputStream in=dest.openRead()){if(result.length()>0 && result.length()<=maxBytes)return result;}catch(FileNotFoundException ignored){}
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(source.getPath(),options);
        if(options.outWidth<=0 || options.outHeight<=0)throw new IOException("Foto tidak dapat dibaca. Ambil ulang foto.");
        options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/(options.inSampleSize*2)>=MAX_EDGE)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        Bitmap bitmap=BitmapFactory.decodeFile(source.getPath(),options);
        if(bitmap==null)throw new IOException("Foto gagal dibuka untuk kompresi.");
        try{
            int longest=Math.max(bitmap.getWidth(),bitmap.getHeight());
            if(longest>MAX_EDGE){Bitmap scaled=scale(bitmap,MAX_EDGE);bitmap.recycle();bitmap=scaled;}
            if(location!=null){
                Bitmap stamped=bitmap.copy(Bitmap.Config.ARGB_8888,true);
                if(stamped==null)throw new IOException("Foto gagal disiapkan untuk label koordinat.");
                bitmap.recycle();bitmap=stamped;
                location.drawOn(new Canvas(bitmap),header);
            }
            byte[] data=null;
            while(true){
                for(int quality:new int[]{82,74,66}){
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    if(!bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new IOException("Kompresi JPEG gagal.");
                    data=out.toByteArray();
                    if(data.length<=maxBytes)break;
                }
                if(data.length<=maxBytes)break;
                int edge=(int)(Math.max(bitmap.getWidth(),bitmap.getHeight())*.8f);
                if(edge<320)throw new IOException("Foto tidak dapat dikompres ke ukuran Standar.");
                Bitmap scaled=scale(bitmap,edge);bitmap.recycle();bitmap=scaled;
            }
            FileOutputStream out=dest.startWrite();
            try{out.write(data);dest.finishWrite(out);}catch(Exception e){dest.failWrite(out);throw e;}
            return result;
        }finally{bitmap.recycle();}
    }
    private static Bitmap scale(Bitmap source,int edge){
        double ratio=edge/(double)Math.max(source.getWidth(),source.getHeight());
        return Bitmap.createScaledBitmap(source,Math.max(1,(int)Math.round(source.getWidth()*ratio)),Math.max(1,(int)Math.round(source.getHeight()*ratio)),true);
    }
    void clear(){File[] files=directory.listFiles();if(files!=null)for(File f:files)if(f.isFile())f.delete();}
}
