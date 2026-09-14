package id.desa.dokumentasirumah;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class LocationStamp {
    final double latitude;
    final double longitude;
    final float accuracy;
    final long time;

    LocationStamp(double latitude,double longitude,float accuracy,long time){
        this.latitude=latitude;
        this.longitude=longitude;
        this.accuracy=accuracy;
        this.time=time;
    }

    static LocationStamp from(Location location){
        if(location==null)return null;
        long when=location.getTime()>0?location.getTime():System.currentTimeMillis();
        return new LocationStamp(location.getLatitude(),location.getLongitude(),location.hasAccuracy()?location.getAccuracy():-1,when);
    }

    static LocationStamp fromDraft(android.content.SharedPreferences draft,int index){
        if(!draft.contains("lat"+index)||!draft.contains("lng"+index))return null;
        return new LocationStamp(
                Double.longBitsToDouble(draft.getLong("lat"+index,0)),
                Double.longBitsToDouble(draft.getLong("lng"+index,0)),
                draft.getFloat("acc"+index,-1),
                draft.getLong("locTime"+index,0));
    }

    void save(android.content.SharedPreferences.Editor editor,int index){
        editor.putLong("lat"+index,Double.doubleToRawLongBits(latitude));
        editor.putLong("lng"+index,Double.doubleToRawLongBits(longitude));
        editor.putFloat("acc"+index,accuracy);
        editor.putLong("locTime"+index,time);
    }

    static void clear(android.content.SharedPreferences.Editor editor,int index){
        editor.remove("lat"+index).remove("lng"+index).remove("acc"+index).remove("locTime"+index);
    }

    String shortLabel(){
        if(accuracy>=0)return String.format(Locale.ROOT,"GPS %.5f, %.5f | %.0f m",latitude,longitude,accuracy);
        return String.format(Locale.ROOT,"GPS %.5f, %.5f",latitude,longitude);
    }

    String[] lines(String... header){
        String accuracyText=accuracy>=0?String.format(Locale.ROOT,"Akurasi: %.0f m",accuracy):"Akurasi: -";
        String timeText=time>0?DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,new Locale("id","ID")).format(new Date(time)):"-";
        String[] gps=new String[]{
                String.format(Locale.ROOT,"Lat: %.6f",latitude),
                String.format(Locale.ROOT,"Lng: %.6f",longitude),
                accuracyText,
                "Waktu: "+timeText
        };
        if(header==null||header.length==0)return gps;
        String[] result=new String[header.length+gps.length];
        System.arraycopy(header,0,result,0,header.length);
        System.arraycopy(gps,0,result,header.length,gps.length);
        return result;
    }

    void drawOn(Canvas canvas,String... header){
        int width=canvas.getWidth(),height=canvas.getHeight();
        float density=Math.max(1f,Math.min(width,height)/720f);
        float pad=18f*density;
        String title=header!=null&&header.length>0?header[0]:"DOKUMENTASI RUMAH";
        String address1=header!=null&&header.length>1?header[1]:"Desa Tombulang, Kec. Pinogaluman,";
        String address2=header!=null&&header.length>2?header[2]:"Kab. Bolaang Mongondow Utara, Sulawesi Utara";
        String coordinates=String.format(Locale.ROOT,"Lat: %.6f   |   Lng: %.6f",latitude,longitude);
        String accuracyText=accuracy>=0?String.format(Locale.ROOT,"Akurasi: %.0f m",accuracy):"Akurasi: -";
        String timeText=time>0?new SimpleDateFormat("dd MMM yyyy HH:mm",new Locale("id","ID")).format(new Date(time)):"-";
        float panelWidth=Math.min(width-pad*2,Math.max(520f*density,width*.72f));
        float titleSize=27f*density,bodySize=19f*density,line=27f*density;
        float panelHeight=pad*2+titleSize+line*4.35f;
        float left=pad,top=height-panelHeight-pad,right=left+panelWidth,bottom=height-pad;
        Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setShader(new LinearGradient(left,top,right,bottom,0xc756514d,0xb51c211f,Shader.TileMode.CLAMP));
        Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(1.5f*density);stroke.setColor(0x66ffffff);
        RectF rect=new RectF(left,top,right,bottom);
        canvas.drawRoundRect(rect,13f*density,13f*density,fill);canvas.drawRoundRect(rect,13f*density,13f*density,stroke);
        Paint titlePaint=new Paint(Paint.ANTI_ALIAS_FLAG);titlePaint.setColor(Color.WHITE);titlePaint.setTextSize(titleSize);titlePaint.setFakeBoldText(true);
        Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);body.setColor(0xfff7f3ed);body.setTextSize(bodySize);
        float x=left+pad,y=top+pad+titleSize;canvas.drawText(title,x,y,titlePaint);
        y+=line;canvas.drawText(address1,x,y,body);y+=line;canvas.drawText(address2,x,y,body);
        y+=10f*density;Paint divider=new Paint(Paint.ANTI_ALIAS_FLAG);divider.setColor(0x77ffffff);divider.setStrokeWidth(1f*density);canvas.drawLine(x,y,right-pad,y,divider);
        float iconX=x+8f*density,textX=x+31f*density;y+=line;
        drawPin(canvas,iconX,y-7f*density,body,density);canvas.drawText(coordinates,textX,y,body);
        y+=line;drawTarget(canvas,iconX,y-7f*density,body,density);canvas.drawText(accuracyText,textX,y,body);
        float timeX=left+panelWidth*.48f;drawClock(canvas,timeX,y-7f*density,body,density);canvas.drawText("Waktu: "+timeText,timeX+23f*density,y,body);
    }

    private static void drawPin(Canvas c,float x,float y,Paint p,float d){Paint q=new Paint(p);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(1.5f*d);c.drawCircle(x,y-3*d,7*d,q);c.drawCircle(x,y-3*d,2.2f*d,q);c.drawLine(x-4*d,y+3*d,x,y+9*d,q);c.drawLine(x+4*d,y+3*d,x,y+9*d,q);}
    private static void drawTarget(Canvas c,float x,float y,Paint p,float d){Paint q=new Paint(p);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(1.5f*d);c.drawCircle(x,y,7*d,q);c.drawCircle(x,y,2.5f*d,q);c.drawLine(x-10*d,y,x-5*d,y,q);c.drawLine(x+5*d,y,x+10*d,y,q);c.drawLine(x,y-10*d,x,y-5*d,q);c.drawLine(x,y+5*d,x,y+10*d,q);}
    private static void drawClock(Canvas c,float x,float y,Paint p,float d){Paint q=new Paint(p);q.setStyle(Paint.Style.STROKE);q.setStrokeWidth(1.5f*d);c.drawCircle(x,y,8*d,q);c.drawLine(x,y,x,y-5*d,q);c.drawLine(x,y,x+4*d,y+2*d,q);}
}
