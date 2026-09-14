package id.desa.dokumentasirumah;

import android.app.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import org.json.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int TEAL=0xff087f70, INK=0xff173c36, MUTED=0xff708780;
    private static final int CAMERA=41;
    private static final int LOCATION_PERMISSION=42;
    private static final int DOC_CAMERA=43;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ImageView[] thumbs=new ImageView[FormRules.LABELS.length];
    private final TextView[] captions=new TextView[FormRules.LABELS.length];
    private final LinearLayout[] rows=new LinearLayout[FormRules.LABELS.length];
    private SharedPreferences draft;
    private EditText name;
    private TextView count,status,account;
    private Button upload,connect,historyButton;
    private final String[] thumbnailKeys=new String[FormRules.LABELS.length];
    private ReportHistory history;
    private int stageProgress;
    private ProgressBar progress;
    private boolean busy=false;
    private int pending=-1;
    private AccessStore access;
    private LocationManager locationManager;
    private int pendingLocationPhoto=-1;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        draft=getSharedPreferences("draft",MODE_PRIVATE);
        pending=draft.getInt("pending",-1);
        access=new AccessStore(this);
        history=new ReportHistory(this);
        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        // Personal-Drive IDs from v1 cannot be sent to the central server. Keep local photos/name.
        if(draft.contains("id0") && !draft.contains("reportId")){SharedPreferences.Editor edit=draft.edit();for(int i=0;i<9;i++)edit.remove("id"+i);edit.commit();}
        getWindow().setStatusBarColor(0xfff4f8f6);
        getWindow().setNavigationBarColor(0xfff4f8f6);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        buildUi(); refresh();
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private void add(LinearLayout parent,View view,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(view,p);}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?Color.WHITE:TEAL);b.setBackground(bg(primary?TEAL:0xffe1efea,14));b.setMinHeight(dp(50));return b;}
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xfff4f8f6);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(16),dp(20),dp(16));scroll.addView(page);
        TextView brand=text("PENDATAAN LAPANGAN",10,TEAL);brand.setLetterSpacing(.12f);brand.setTypeface(null,Typeface.BOLD);add(page,brand,0);
        TextView title=text("Dokumentasi rumah",25,INK);title.setTypeface(null,Typeface.BOLD);add(page,title,4);
        account=text("Petugas belum diatur",12,MUTED);add(page,account,4);
        LinearLayout tools=new LinearLayout(this);
        historyButton=button("Riwayat",false);historyButton.setOnClickListener(v->showHistory());tools.addView(historyButton,new LinearLayout.LayoutParams(0,dp(48),1));
        connect=button("Pengaturan",false);connect.setOnClickListener(v->showSettings());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1);cp.leftMargin=dp(10);tools.addView(connect,cp);add(page,tools,12);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView photoTitle=text("Foto kondisi rumah",16,INK);photoTitle.setTypeface(null,Typeface.BOLD);heading.addView(photoTitle,new LinearLayout.LayoutParams(0,-2,1));
        count=text("0/5 foto",12,TEAL);heading.addView(count);add(page,heading,16);
        for(int i=0;i<FormRules.LABELS.length;i++){
            final int index=i;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(8),dp(10),dp(8));row.setBackground(bg(Color.WHITE,14));rows[i]=row;
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(bg(0xffeaf3ef,10));image.setClipToOutline(true);image.setContentDescription("Foto "+FormRules.LABELS[i]);thumbs[i]=image;row.addView(image,new LinearLayout.LayoutParams(dp(54),dp(54)));
            LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(12);row.addView(labels,lp);
            TextView label=text(FormRules.LABELS[i],13,INK);label.setTypeface(null,Typeface.BOLD);labels.addView(label);
            captions[i]=text(i<FormRules.REQUIRED_COUNT?"Ketuk untuk ambil foto":"Opsional - ketuk untuk ambil",11,MUTED);add(labels,captions[i],4);
            row.addView(text("+",23,TEAL));
            row.setOnClickListener(v->photoActions(index));row.setFocusable(true);row.setContentDescription("Ambil atau lihat foto "+FormRules.LABELS[i]);add(page,row,6);
        }
        TextView nameLabel=text("Nama kepala keluarga",14,INK);nameLabel.setTypeface(null,Typeface.BOLD);add(page,nameLabel,16);
        name=new EditText(this);name.setTextSize(15);name.setSingleLine(true);name.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);name.setHint("Contoh: Budi Santoso");name.setTextColor(INK);name.setPadding(dp(14),dp(12),dp(14),dp(12));name.setBackground(bg(Color.WHITE,12));name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});name.setText(draft.getString("name",""));add(page,name,8);
        name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int st,int before,int c){draft.edit().putString("name",s.toString()).apply();refresh();}public void afterTextChanged(Editable e){}});
        add(page,text("Draf otomatis tersimpan. Kompresi Standar aktif.",11,MUTED),8);
        LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.VERTICAL);footer.setPadding(dp(20),dp(8),dp(20),dp(12));footer.setBackgroundColor(Color.WHITE);root.addView(footer,new LinearLayout.LayoutParams(-1,-2));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(12);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(TEAL));add(footer,progress,0);
        status=text("Lengkapi lima foto dan nama KK.",12,MUTED);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(footer,status,4);
        upload=button("Upload ke Drive pusat",true);upload.setOnClickListener(v->send());add(footer,upload,8);
        setContentView(root);
    }
    private void showSettings(){
        new AlertDialog.Builder(this).setTitle("Pengaturan").setItems(new String[]{"Kode akses petugas","Cek pembaruan aplikasi","Hapus draf / rumah baru"},(d,index)->{if(index==0)configure();else if(index==1)checkForUpdate();else requestReset();}).setNegativeButton("Tutup",null).show();
    }
    private void checkForUpdate(){
        final ProgressDialog wait=ProgressDialog.show(this,"Pembaruan aplikasi","Memeriksa GitHub…",true,false);
        UpdateChecker.check(this,(manifest,error)->runOnUiThread(()->{
            wait.dismiss();
            if(error!=null){new AlertDialog.Builder(this).setTitle("Pembaruan belum dapat diperiksa").setMessage(error.getMessage()+"\n\nPeriksa koneksi internet atau coba lagi nanti.").setPositiveButton("Tutup",null).show();return;}
            int latest=manifest.optInt("versionCode",0); String version=manifest.optString("versionName","versi baru");
            long currentCode=0; String currentName="versi ini";
            try { android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0); currentCode=info.versionCode; currentName=info.versionName; } catch(Exception ignored) {}
            if(latest<=currentCode){new AlertDialog.Builder(this).setTitle("Aplikasi sudah terbaru").setMessage("Versi "+currentName+" sudah terpasang.").setPositiveButton("Tutup",null).show();return;}
            String notes=manifest.optString("notes","Pembaruan tersedia."); String url=manifest.optString("releaseUrl",manifest.optString("apkUrl","https://github.com/abidin290/DESIL/releases"));
            new AlertDialog.Builder(this).setTitle("Pembaruan tersedia: "+version).setMessage(notes+"\n\nAndroid akan meminta konfirmasi sebelum memasang APK.").setNegativeButton("Nanti",null).setPositiveButton("Buka unduhan",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));}catch(Exception e){error("Tautan unduhan tidak dapat dibuka.");}}).show();
        }));
    }
    private void requestReset(){
        new AlertDialog.Builder(this).setTitle("Hapus draf lokal?").setMessage("Foto dan nama dalam draf ini akan dihapus. Riwayat dan foto di Drive tetap ada.").setNegativeButton("Batal",null).setPositiveButton("Hapus draf",(d,w)->{
            try{record("Draf dihapus","Draf lokal dihapus oleh petugas; data yang sudah diterima server tetap ada.");clearDraft();status.setText("Draf baru siap diisi.");}catch(Exception e){error(e.getMessage());}
        }).show();
    }
    private void showHistory(){
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(20),dp(10),dp(20),dp(10));
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Riwayat di HP ini").setView(scroll).setPositiveButton("Tutup",null).create();
        boolean haveDraft=!name.getText().toString().trim().isEmpty();for(int i=0;i<FormRules.LABELS.length;i++)haveDraft|=photo(i).isFile();
        if(haveDraft){Button resume=button("Lanjutkan draf aktif",true);resume.setOnClickListener(v->dialog.dismiss());add(list,resume,0);add(list,text("Draf aktif: "+(name.getText().toString().trim().isEmpty()?"Nama KK belum diisi":name.getText().toString()),13,INK),8);}
        add(list,text("Maksimal 200 laporan terakhir di perangkat ini. Waktu mengikuti jam HP.",12,MUTED),10);
        try{
            JSONArray entries=history.entries();
            if(entries.length()==0)add(list,text("Belum ada riwayat pengiriman. Laporan yang dikirim sebelum versi ini tidak muncul otomatis.",14,INK),16);
            for(int i=0;i<entries.length();i++){
                JSONObject entry=entries.getJSONObject(i);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(12),dp(12),dp(12));card.setBackground(bg(0xfff4f8f6,12));
                TextView heading=text(entry.optString("name"),16,INK);heading.setTypeface(null,Typeface.BOLD);add(card,heading,0);
                String time=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT,new Locale("id","ID")).format(new Date(entry.optLong("updatedAt")));
                add(card,text(entry.optString("state")+" | "+time,12,"Selesai".equals(entry.optString("state"))?TEAL:0xff93601a),6);
                add(card,text(entry.optString("worker"),12,MUTED),4);
                TextView detail=text(entry.optString("detail")+"\nID: "+entry.optString("id"),12,INK);detail.setTextIsSelectable(true);add(card,detail,6);add(list,card,10);
            }
        }catch(Exception e){add(list,text("Riwayat belum dapat dibaca. Draf tetap tersedia.",13,INK),12);}
        dialog.show();
    }
    private void record(String state,String detail)throws Exception{
        history.save(draft.getString("reportId",""),draft.getString("name",""),draft.getString("workerName",access.get("workerName")),state,detail);
    }
    private File photo(int i){return new File(getFilesDir(),FormRules.FILES[i]);}
    private File capture(){File dir=new File(getFilesDir(),"capture");dir.mkdirs();return new File(dir,"pending.jpg");}
    private boolean locked(){return draft.contains("reportId");}
    private void refresh(){
        if(upload==null)return;
        boolean[] have=new boolean[FormRules.LABELS.length];int n=0;
        for(int i=0;i<FormRules.LABELS.length;i++){
            have[i]=photo(i).length()>0;if(have[i]&&i<FormRules.REQUIRED_COUNT)n++;
            LocationStamp stamp=LocationStamp.fromDraft(draft,i);
            captions[i].setText(have[i]?(stamp==null?"Tersimpan":stamp.shortLabel()):(i<FormRules.REQUIRED_COUNT?"Ketuk untuk ambil foto":"Opsional - ketuk untuk ambil"));
            captions[i].setTextColor(have[i]?TEAL:MUTED);
            String thumbnailKey=have[i]?photo(i).length()+":"+photo(i).lastModified():"empty";
            if(!thumbnailKey.equals(thumbnailKeys[i])){
                if(have[i]){BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=4;thumbs[i].setImageBitmap(BitmapFactory.decodeFile(photo(i).getPath(),opts));}else{thumbs[i].setImageBitmap(cameraIcon());}
                thumbnailKeys[i]=thumbnailKey;
            }
            ((TextView)rows[i].getChildAt(2)).setText(have[i]?"Lihat":"+");
            ((TextView)rows[i].getChildAt(2)).setTextSize(have[i]?12:23);
            rows[i].setEnabled(!busy);
        }
        account.setText(access.get("workerName").isEmpty()?"Drive pusat - Masukkan kode akses":"Drive pusat - "+access.get("workerName"));

        count.setText(n+"/5 foto wajib");name.setEnabled(!busy&&!locked());connect.setEnabled(!busy);historyButton.setEnabled(!busy);progress.setVisibility(busy?View.VISIBLE:View.GONE);
        boolean ready=FormRules.ready(name.getText().toString(),have);upload.setEnabled(!busy&&ready);upload.setAlpha(!busy&&ready?1f:.45f);
        upload.setText(busy?"Sedang memproses…":locked()?"Lanjutkan upload  ↑":"Upload ke Drive pusat");
        if(!busy)status.setText(locked()?"Draf belum selesai. Lanjutkan upload laporan ini.":ready?"Siap dikirim ke Drive pusat.":"Lengkapi lima foto dan nama KK.");
    }
    private Bitmap cameraIcon(){Bitmap b=Bitmap.createBitmap(dp(52),dp(52),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(b.getWidth()/52f,b.getHeight()/52f);Paint p=new Paint(3);p.setColor(TEAL);c.drawRoundRect(10,18,42,38,4,4,p);c.drawRoundRect(18,13,31,23,2,2,p);p.setColor(0xffeaf3ef);c.drawCircle(26,28,8,p);p.setColor(TEAL);c.drawCircle(26,28,5,p);return b;}
    private void photoActions(int i){
        if(!photo(i).exists()){takePhoto(i);return;}
        Bitmap bitmap=BitmapFactory.decodeFile(photo(i).getPath());if(bitmap==null){error("Foto tidak dapat dibuka.");return;}
        final ZoomPhotoView preview=new ZoomPhotoView(this,bitmap);
        Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(0xff102924);layout.setPadding(dp(12),dp(12),dp(12),dp(12));
        layout.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(12),insets.getSystemWindowInsetTop()+dp(12),dp(12),insets.getSystemWindowInsetBottom()+dp(12));return insets;});
        TextView title=text(FormRules.LABELS[i],17,Color.WHITE);add(layout,title,4);
        add(layout,text("Cubit / ketuk dua kali untuk zoom. Geser untuk detail.",12,0xffc1dbd3),6);
        layout.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(this);
        String[] labels={"-","Pas","+"};for(int j=0;j<3;j++){final int action=j;Button b=button(labels[j],false);b.setContentDescription(j==0?"Perkecil foto":j==1?"Tampilkan seluruh foto":"Perbesar foto");b.setOnClickListener(v->{if(action==1)preview.reset();else preview.zoomBy(action==0?.67f:1.5f);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMargins(dp(3),0,dp(3),0);controls.addView(b,lp);}add(layout,controls,8);
        if(!locked()){Button retake=button("Ambil ulang foto",true);retake.setOnClickListener(v->{dialog.dismiss();takePhoto(i);});add(layout,retake,8);}
        Button close=button("Tutup preview",false);close.setOnClickListener(v->dialog.dismiss());add(layout,close,8);
        dialog.setContentView(layout);dialog.setOnDismissListener(d->preview.release());dialog.show();dialog.getWindow().setLayout(-1,-1);
    }
    private void takePhoto(int i){
        if(busy||locked())return;
        if(i>=FormRules.REQUIRED_COUNT){takeDocumentPhoto(i);return;}
        if(!hasLocationPermission()){
            pendingLocationPhoto=i;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_PERMISSION);
            return;
        }
        takePhotoWithoutLocation(i);
    }
    private void takeDocumentPhoto(int i){
        try{
            capture().delete();pending=i;draft.edit().putInt("pending",i).commit();
            Intent intent=new Intent(this,DocumentCameraActivity.class);
            intent.putExtra(DocumentCameraActivity.EXTRA_TYPE,i==5?"KTP":i==6?"KK":"IDPEL");
            intent.putExtra(DocumentCameraActivity.EXTRA_OUTPUT,capture().getAbsolutePath());
            startActivityForResult(intent,DOC_CAMERA);
        }catch(Exception e){pending=-1;draft.edit().remove("pending").apply();error("Kamera dokumen tidak tersedia.");}
    }
    private void takePhotoWithoutLocation(int i){
        try{
            capture().delete();pending=i;draft.edit().putInt("pending",i).commit();
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".photos",capture());
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("Foto rumah",uri));startActivityForResult(intent,CAMERA);
        }catch(Exception e){pending=-1;draft.edit().remove("pending").apply();error("Kamera tidak tersedia. Pastikan perangkat memiliki aplikasi kamera.");}
    }
    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==LOCATION_PERMISSION){
            int index=pendingLocationPhoto;pendingLocationPhoto=-1;
            if(index>=0 && index<FormRules.REQUIRED_COUNT){
                if(hasLocationPermission())takePhoto(index);
                else{
                    Toast.makeText(this,"Izin lokasi ditolak. Foto tetap bisa diambil tanpa koordinat.",Toast.LENGTH_LONG).show();
                    takePhotoWithoutLocation(index);
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    private LocationStamp currentLocation(){
        if(!hasLocationPermission()||locationManager==null)return null;
        Location best=null;
        for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER,LocationManager.PASSIVE_PROVIDER}){
            try{
                Location location=locationManager.getLastKnownLocation(provider);
                if(location!=null && (best==null || location.getTime()>best.getTime()))best=location;
            }catch(Exception ignored){}
        }
        return LocationStamp.from(best);
    }
    private void configure(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(12),dp(24),0);
        EditText code=new EditText(this);code.setHint("Kode akses petugas");code.setSingleLine(true);code.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);add(box,code,0);
        add(box,text("Masukkan kode dari admin. Server Drive pusat sudah tertanam di aplikasi.",12,MUTED),12);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Kode akses petugas").setView(box).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String endpoint=CentralClient.DEFAULT_ENDPOINT, secret=code.getText().toString().trim().toUpperCase(java.util.Locale.ROOT);
            if(!secret.matches("[A-F0-9]{32}")){code.setError("Kode admin terdiri dari 32 karakter. Salin kode lengkap.");return;}
            if(locked() && !endpoint.equals(draft.getString("endpoint",""))){error("Selesaikan atau hapus draf lama sebelum memakai server tertanam.");return;}
            dialog.dismiss();busy=true;refresh();status.setText("Memeriksa kode akses...");
            worker.execute(()->{try{
                JSONObject result=new CentralClient(endpoint,secret).call(new JSONObject().put("action","auth"));
                String id=result.getString("workerId");
                if(locked() && !id.equals(draft.getString("workerId","")))throw new IOException("Draf ini milik petugas lain. Gunakan kode petugas yang sama.");
                access.save(endpoint,secret,id,result.getString("workerName"));
                runOnUiThread(()->{busy=false;refresh();status.setText("Kode terverifikasi. Upload akan masuk ke Drive pusat.");});
            }catch(Exception e){runOnUiThread(()->{busy=false;refresh();error(e.getMessage());});}});
        }));dialog.show();
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==CAMERA||req==DOC_CAMERA){final int index=pending;pending=-1;draft.edit().remove("pending").apply();
            if(result!=RESULT_OK||index<0||index>=FormRules.LABELS.length){capture().delete();return;}
            final LocationStamp stamp=req==CAMERA?currentLocation():null;
            busy=true;refresh();status.setText("Menyimpan foto...");
            worker.execute(()->{try{normalizePhoto(capture(),photo(index));SharedPreferences.Editor editor=draft.edit();if(stamp!=null)stamp.save(editor,index);else LocationStamp.clear(editor,index);editor.commit();new UploadCompressor(getFilesDir()).clear();runOnUiThread(()->{busy=false;refresh();status.setText(index>=FormRules.REQUIRED_COUNT?"Lampiran opsional tersimpan.":stamp==null?"Foto tersimpan. Koordinat belum tersedia di HP.":"Foto tersimpan dengan koordinat.");});}catch(Exception e){runOnUiThread(()->{busy=false;refresh();error("Foto gagal disimpan. Silakan ambil ulang.");});}finally{capture().delete();}});
        }
    }
    private void normalizePhoto(File input,File output)throws Exception{
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(input.getPath(),o);
        if(o.outWidth<=0)throw new IOException("Invalid image");
        o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>2400)o.inSampleSize*=2;o.inJustDecodeBounds=false;
        Bitmap raw=BitmapFactory.decodeFile(input.getPath(),o);if(raw==null)throw new IOException("Invalid bitmap");
        int orientation=new ExifInterface(input.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);Matrix m=new Matrix();
        switch(orientation){case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(270);m.postScale(-1,1);break;case 8:m.setRotate(270);break;}
        Bitmap rotated=Bitmap.createBitmap(raw,0,0,raw.getWidth(),raw.getHeight(),m,true);android.util.AtomicFile dest=new android.util.AtomicFile(output);FileOutputStream stream=dest.startWrite();
        try{if(!rotated.compress(Bitmap.CompressFormat.JPEG,88,stream))throw new IOException("JPEG failed");dest.finishWrite(stream);}catch(Exception e){dest.failWrite(stream);throw e;}finally{if(rotated!=raw)rotated.recycle();raw.recycle();}
    }
    private void send(){
        if(busy)return;
        if(access.get("workerId").isEmpty()){configure();return;}
        final String folderName=name.getText().toString().trim();
        busy=true;refresh();getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);progress.setMax(15);progress.setProgress(0);stageProgress=0;
        worker.execute(()->{
            try{
                String endpoint=CentralClient.DEFAULT_ENDPOINT;
                if(!CentralClient.validEndpoint(endpoint))throw new IOException("URL server tidak valid.");
                CentralClient client=new CentralClient(endpoint,access.code());
                File[] uploadPhotos=new File[FormRules.LABELS.length];
                if(draft.contains("reportId")){
                    // v2 partial reports must keep their original bytes: the server checks digests.
                    for(int i=0;i<FormRules.LABELS.length;i++){
                        if(!draft.contains("id"+(i+1)))continue;
                        uploadPhotos[i]=draft.contains("compressed"+i)?new File(getFilesDir(),draft.getString("compressed"+i,"")):photo(i);
                        if(i<FormRules.REQUIRED_COUNT && (!uploadPhotos[i].isFile() || uploadPhotos[i].length()==0))throw new IOException("Salinan upload draf tidak ditemukan. Jangan lanjutkan dengan foto berbeda; mulai laporan baru.");
                    }
                }else{
                    UploadCompressor compressor=new UploadCompressor(getFilesDir());
                    for(int i=0;i<FormRules.LABELS.length;i++){
                        if(i>=FormRules.REQUIRED_COUNT && !photo(i).isFile())continue;
                        update(Math.min(i,5),"Menyiapkan "+FormRules.LABELS[i]+"...");
                        LocationStamp stamp=LocationStamp.fromDraft(draft,i);
                        uploadPhotos[i]=i==0?compressor.prepare(photo(i),stamp,"Rumah "+folderName,"Desa Tombulang"):compressor.prepare(photo(i),stamp);
                    }
                }
                if(!draft.contains("reportId")){
                    boolean[] photos=new boolean[FormRules.LABELS.length];for(int i=0;i<FormRules.LABELS.length;i++)photos[i]=photo(i).length()>0;
                    if(!FormRules.ready(folderName,photos))throw new IOException("Lengkapi nama dan lima foto.");
                    
                    JSONArray ids=request(client,new JSONObject().put("action","reserve"),"Menyiapkan laporan").getJSONArray("ids");
                    if(ids.length()<6)throw new IOException("ID server tidak lengkap.");
                    boolean hasOptional=false;for(int i=FormRules.REQUIRED_COUNT;i<FormRules.LABELS.length;i++)hasOptional|=photo(i).isFile();
                    if(hasOptional && ids.length()<FormRules.LABELS.length+1)throw new IOException("Server belum mendukung lampiran KTP/KK/IDPEL. Perbarui Code.gs lalu deploy versi baru.");
                    SharedPreferences.Editor ed=draft.edit().putString("reportId",UUID.randomUUID().toString()).putString("endpoint",endpoint).putString("workerId",access.get("workerId")).putString("workerName",access.get("workerName"));
                    for(int i=0;i<ids.length();i++)ed.putString("id"+i,ids.getString(i));
                    for(int i=0;i<FormRules.LABELS.length;i++)if(uploadPhotos[i]!=null)ed.putString("compressed"+i,"upload_standard/"+uploadPhotos[i].getName());
                    if(!ed.commit())throw new IOException("Penyimpanan draf gagal.");
                }
                if(!endpoint.equals(draft.getString("endpoint","")) || !access.get("workerId").equals(draft.getString("workerId","")))throw new IOException("Gunakan server dan kode petugas yang sama dengan draf.");
                record("Belum selesai","Menyiapkan pengiriman. Draf tersedia di HP ini.");
                JSONArray ids=new JSONArray();for(int i=0;i<9 && draft.contains("id"+i);i++)ids.put(draft.getString("id"+i,""));
                JSONObject report=new JSONObject().put("reportId",draft.getString("reportId","")).put("ids",ids).put("name",folderName);
                update(5,"Menyiapkan folder pusat...");request(client,new JSONObject(report.toString()).put("action","begin"),"Menyiapkan folder");update(6,"Folder pusat siap.");
                for(int i=0;i<FormRules.REQUIRED_COUNT;i++){update(i+6,"Mengirim foto "+(i+1)+"/5...");request(client,client.photoBody(report,i,uploadPhotos[i]),"Mengirim foto "+(i+1)+"/5");record("Belum selesai",(i+1)+"/5 foto diterima; menunggu verifikasi lengkap.");update(i+7,(i+1)+"/5 foto diterima server");}
                for(int i=FormRules.REQUIRED_COUNT;i<FormRules.LABELS.length;i++)if(uploadPhotos[i]!=null && uploadPhotos[i].isFile() && draft.contains("id"+(i+1))){update(i+7,"Mengirim "+FormRules.LABELS[i]+"...");request(client,client.photoBody(report,i,uploadPhotos[i]),"Mengirim "+FormRules.LABELS[i]);}
                update(14,"Memeriksa kelengkapan laporan...");
                JSONObject result=request(client,new JSONObject(report.toString()).put("action","complete"),"Memeriksa laporan");
                if(!result.optBoolean("complete") || !report.getString("reportId").equals(result.optString("reportId")))throw new IOException("Server belum mengonfirmasi laporan lengkap.");
                record("Selesai","Kelima foto telah dikonfirmasi server.");
                update(15,"Laporan selesai.");
                final String receipt=result.getString("reportId");
                runOnUiThread(()->{busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);clearDraft();status.setText("Lima foto berhasil disimpan ke Drive pusat.");new AlertDialog.Builder(this).setTitle("Upload berhasil").setMessage("Nama KK: "+folderName+"\nID laporan: "+receipt+"\n\nForm siap untuk rumah berikutnya.").setPositiveButton("Selesai",null).show();});
            }catch(Exception e){try{record("Belum selesai",e.getMessage()==null?"Upload terhenti. Lanjutkan draf.":e.getMessage());}catch(Exception ignored){}runOnUiThread(()->{busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refresh();error(e.getLocalizedMessage()==null?"Upload gagal. Draf tetap tersimpan.":e.getLocalizedMessage());});}
        });
    }
    private JSONObject request(CentralClient client,JSONObject body,String stage)throws Exception{
        return client.callWithRetry(body,(retry,total,millis)->update(stageProgress,stage+" - koneksi/server sibuk. Coba ulang "+retry+"/"+total+" dalam "+(millis/1000)+" detik..."));
    }
    private void update(int value,String message){stageProgress=value;runOnUiThread(()->{progress.setProgress(value);status.setText(message);});}
    private void clearDraft(){new UploadCompressor(getFilesDir()).clear();for(int i=0;i<FormRules.LABELS.length;i++)photo(i).delete();capture().delete();draft.edit().clear().commit();name.setText("");progress.setProgress(0);refresh();}
    private void error(String message){status.setText(message);new AlertDialog.Builder(this).setTitle("Belum berhasil").setMessage(message).setPositiveButton("Mengerti",null).show();}
    @Override public void onBackPressed(){if(busy){Toast.makeText(this,"Tunggu proses selesai. Draf tersimpan bila aplikasi terhenti.",Toast.LENGTH_SHORT).show();}else super.onBackPressed();}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
