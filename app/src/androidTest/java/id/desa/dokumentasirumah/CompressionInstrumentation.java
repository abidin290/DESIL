package id.desa.dokumentasirumah;
import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Runs actual Android JPEG encoder, scaling and AtomicFile, without mocked Bitmap APIs. */
public class CompressionInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private void stage(String message){Bundle b=new Bundle();b.putString("stream",message+"\n");sendStatus(0,b);}
    @Override public void onStart(){
        Bundle result=new Bundle();
        File root=new File(getTargetContext().getFilesDir(),"compression_test");root.mkdirs();
        try{
            stage("Testing Android JPEG encoder...");
            UploadCompressor compressor=new UploadCompressor(root);
            File source=new File(root,"source.jpg");
            Bitmap bitmap=Bitmap.createBitmap(2400,1800,Bitmap.Config.ARGB_8888);
            int[] pixels=new int[2400*1800];int seed=42;
            for(int i=0;i<pixels.length;i++){seed^=seed<<13;seed^=seed>>>17;seed^=seed<<5;pixels[i]=0xff000000|(seed&0xffffff);}
            bitmap.setPixels(pixels,0,2400,0,0,2400,1800);pixels=null;
            try(OutputStream out=new FileOutputStream(source)){bitmap.compress(Bitmap.CompressFormat.JPEG,95,out);}bitmap.recycle();
            byte[] original=Files.readAllBytes(source.toPath());
            long started=System.nanoTime();File compressed=compressor.prepare(source);long millis=(System.nanoTime()-started)/1000000;
            check(compressed.length()<=UploadCompressor.MAX_BYTES,"500 KiB upper bound");
            check(compressed.length()<source.length(),"Noisy photo must shrink");
            Bitmap decoded=BitmapFactory.decodeFile(compressed.getPath());
            check(decoded!=null,"Valid JPEG");check(Math.max(decoded.getWidth(),decoded.getHeight())<=1600,"Max 1600 pixels");
            check(Math.abs(decoded.getWidth()/(double)decoded.getHeight()-4.0/3)<.01,"Aspect ratio");decoded.recycle();
            check(Arrays.equals(original,Files.readAllBytes(source.toPath())),"Draft source preserved");
            byte[] first=Files.readAllBytes(compressed.toPath());long modified=compressed.lastModified();
            check(compressor.prepare(source).equals(compressed),"Same cache file");
            check(modified==compressed.lastModified() && Arrays.equals(first,Files.readAllBytes(compressed.toPath())),"Retry does not recompress");
            Bitmap gpsBitmap=Bitmap.createBitmap(1200,900,Bitmap.Config.ARGB_8888);gpsBitmap.eraseColor(0xff6ea88f);
            try(OutputStream out=new FileOutputStream(source)){gpsBitmap.compress(Bitmap.CompressFormat.JPEG,90,out);}gpsBitmap.recycle();
            LocationStamp stamp=new LocationStamp(1.456789,124.987654,7.5f,1800000000000L);
            File stamped=compressor.prepare(source,stamp,"Rumah Ahmat Sekian","Desa Tombulang");
            check(stamped.length()<=UploadCompressor.MAX_BYTES,"Stamped photo stays below 500 KiB");
            check(!stamped.equals(compressed),"GPS stamp has separate cache key");
            check(compressor.prepare(source,stamp).length()<=UploadCompressor.MAX_BYTES,"GPS-only stamp remains supported");
            decoded=BitmapFactory.decodeFile(stamped.getPath());
            check(decoded!=null,"Stamped JPEG decodes");
            check(Math.max(decoded.getWidth(),decoded.getHeight())<=1600,"Stamped max 1600 pixels");
            decoded.recycle();
            Bitmap small=Bitmap.createBitmap(300,400,Bitmap.Config.ARGB_8888);small.eraseColor(Color.BLUE);
            try(OutputStream out=new FileOutputStream(source)){small.compress(Bitmap.CompressFormat.JPEG,90,out);}small.recycle();
            File replacement=compressor.prepare(source);check(!replacement.equals(compressed),"Retake invalidates content cache");
            decoded=BitmapFactory.decodeFile(replacement.getPath());check(decoded.getWidth()==300 && decoded.getHeight()==400,"Small portrait not upscaled");decoded.recycle();
            try(FileOutputStream out=new FileOutputStream(source)){out.write(new byte[]{1,2,3});}
            boolean rejected=false;try{compressor.prepare(source);}catch(IOException e){rejected=true;}check(rejected,"Reject corrupt image");
            long bytes=compressed.length();compressor.clear();check(!compressed.exists() && !replacement.exists(),"Cache cleanup");
            stage("Testing persistent receipt history...");
            android.content.Context isolated=new android.content.ContextWrapper(getTargetContext()){
                @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("instrumentation_history",mode);}
            };
            isolated.getSharedPreferences("",0).edit().clear().commit();
            ReportHistory history=new ReportHistory(isolated);
            history.save("report-1","Budi","Petugas 1","Belum selesai","2/5");
            history.save("report-1","Budi","Petugas 1","Selesai","5/5 confirmed");
            history.save("report-1","Budi","Petugas 1","Belum selesai","stale callback");
            check(history.entries().length()==1,"Retry does not duplicate history");
            check("Selesai".equals(new ReportHistory(isolated).entries().getJSONObject(0).getString("state")),"Receipt persists and never downgrades");
            history.save("report-2","Budi","Petugas 1","Draf dihapus","deleted locally");
            check(history.entries().length()==2,"Same name separate reports");
            isolated.getSharedPreferences("",0).edit().clear().commit();
            stage("History tests passed.");
            result.putString("stream","PASS: Android JPEG compression, GPS and front-house watermark, dimensions, aspect ratio, draft preservation, cache reuse, retake, portrait, corrupt input, cleanup.\nNoisy 2400x1800: "+original.length+" -> "+bytes+" bytes, "+millis+" ms on emulator (not phone benchmark).\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+e);finish(Activity.RESULT_CANCELED,result);}
        finally{File[] files=root.listFiles();if(files!=null)for(File f:files)if(f.isFile())f.delete();}
    }
}
