package id.desa.dokumentasirumah;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.exifinterface.media.ExifInterface;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.activity.ComponentActivity;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileOutputStream;

public class DocumentCameraActivity extends ComponentActivity {
    public static final String EXTRA_TYPE="type", EXTRA_OUTPUT="output";
    private static final int CAMERA_PERMISSION=71;
    private ImageCapture imageCapture;
    private PreviewView previewView;
    private DocumentOverlayView overlayView;
    private String type;
    private File output;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        type=getIntent().getStringExtra(EXTRA_TYPE);
        output=new File(getIntent().getStringExtra(EXTRA_OUTPUT));
        if("KTP".equals(type)||"KK".equals(type))setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        buildUi();
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();
        else requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void buildUi(){
        FrameLayout root=new FrameLayout(this);
        previewView=new PreviewView(this);previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(previewView,new FrameLayout.LayoutParams(-1,-1));
        overlayView=new DocumentOverlayView(this,type);root.addView(overlayView,new FrameLayout.LayoutParams(-1,-1));
        TextView title=new TextView(this);title.setText(title());title.setTextColor(Color.WHITE);title.setTextSize(18);title.setGravity(Gravity.CENTER);title.setBackgroundColor(0x99000000);title.setPadding(dp(12),dp(12),dp(12),dp(12));
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);root.addView(title,tp);
        Button close=new Button(this);close.setText("Tutup");close.setAllCaps(false);close.setOnClickListener(v->{setResult(RESULT_CANCELED);finish();});
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(110),dp(52),Gravity.BOTTOM|Gravity.LEFT);cp.setMargins(dp(18),0,0,dp(24));root.addView(close,cp);
        View shot=shotButton();shot.setOnClickListener(v->capture());
        boolean landscape="KTP".equals(type)||"KK".equals(type);
        FrameLayout.LayoutParams sp=landscape
                ?new FrameLayout.LayoutParams(dp(76),dp(76),Gravity.RIGHT|Gravity.CENTER_VERTICAL)
                :new FrameLayout.LayoutParams(dp(76),dp(76),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        if(landscape)sp.setMargins(0,0,dp(22),0);else sp.setMargins(0,0,0,dp(22));
        root.addView(shot,sp);
        setContentView(root);
    }
    private View shotButton(){
        View view=new View(this);
        GradientDrawable outer=new GradientDrawable();outer.setShape(GradientDrawable.OVAL);outer.setColor(0xffffffff);outer.setStroke(dp(4),0xff087f70);
        view.setBackground(outer);
        view.setContentDescription("Ambil foto");
        view.setFocusable(true);
        return view;
    }
    private String title(){
        if("KK".equals(type))return "Foto Kartu Keluarga";
        if("IDPEL".equals(type))return "Foto IDPEL Listrik";
        return "Foto KTP";
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==CAMERA_PERMISSION){
            if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();
            else{Toast.makeText(this,"Izin kamera dibutuhkan untuk foto dokumen.",Toast.LENGTH_LONG).show();setResult(RESULT_CANCELED);finish();}
        }
    }
    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            try{
                ProcessCameraProvider provider=future.get();
                Preview p=new Preview.Builder().build();p.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture=new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                provider.unbindAll();provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,imageCapture);
            }catch(Exception e){Toast.makeText(this,"Kamera dokumen tidak tersedia.",Toast.LENGTH_LONG).show();setResult(RESULT_CANCELED);finish();}
        },ContextCompat.getMainExecutor(this));
    }
    private void capture(){
        if(imageCapture==null)return;
        output.getParentFile().mkdirs();
        ImageCapture.OutputFileOptions options=new ImageCapture.OutputFileOptions.Builder(output).build();
        imageCapture.takePicture(options,ContextCompat.getMainExecutor(this),new ImageCapture.OnImageSavedCallback(){
            @Override public void onImageSaved(ImageCapture.OutputFileResults r){
                try{
                    if("KTP".equals(type))cropToFrame();
                    setResult(RESULT_OK);finish();
                }catch(Exception e){Toast.makeText(DocumentCameraActivity.this,"Foto dokumen gagal dipotong.",Toast.LENGTH_LONG).show();}
            }
            @Override public void onError(ImageCaptureException e){Toast.makeText(DocumentCameraActivity.this,"Foto dokumen gagal disimpan.",Toast.LENGTH_LONG).show();}
        });
    }
    private void cropToFrame()throws Exception{
        Bitmap bitmap=BitmapFactory.decodeFile(output.getPath());
        if(bitmap==null)throw new java.io.IOException("Invalid document image");
        try{
            int orientation=new ExifInterface(output.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix=new Matrix();
            switch(orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:matrix.setRotate(90);break;
                case ExifInterface.ORIENTATION_ROTATE_180:matrix.setRotate(180);break;
                case ExifInterface.ORIENTATION_ROTATE_270:matrix.setRotate(270);break;
                default:break;
            }
            if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);bitmap.recycle();bitmap=rotated;}
            Rect crop=mapFrameToBitmap(bitmap.getWidth(),bitmap.getHeight());
            Bitmap cropped=Bitmap.createBitmap(bitmap,crop.left,crop.top,crop.width(),crop.height());
            try(FileOutputStream out=new FileOutputStream(output,false)){
                if(!cropped.compress(Bitmap.CompressFormat.JPEG,92,out))throw new java.io.IOException("JPEG failed");
            }finally{cropped.recycle();}
        }finally{bitmap.recycle();}
    }
    private Rect mapFrameToBitmap(int bitmapWidth,int bitmapHeight)throws Exception{
        int viewWidth=Math.max(1,previewView.getWidth()),viewHeight=Math.max(1,previewView.getHeight());
        RectF frame=overlayView.frameOnView();
        float scale=Math.max(viewWidth/(float)bitmapWidth,viewHeight/(float)bitmapHeight);
        float displayedWidth=bitmapWidth*scale,displayedHeight=bitmapHeight*scale;
        float offsetX=(viewWidth-displayedWidth)/2f,offsetY=(viewHeight-displayedHeight)/2f;
        int left=Math.max(0,Math.round((frame.left-offsetX)/scale));
        int top=Math.max(0,Math.round((frame.top-offsetY)/scale));
        int right=Math.min(bitmapWidth,Math.round((frame.right-offsetX)/scale));
        int bottom=Math.min(bitmapHeight,Math.round((frame.bottom-offsetY)/scale));
        if(right-left<32||bottom-top<32)throw new java.io.IOException("Invalid crop");
        return new Rect(left,top,right,bottom);
    }
}
