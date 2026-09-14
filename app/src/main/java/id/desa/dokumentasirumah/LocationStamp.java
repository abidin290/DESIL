package id.desa.dokumentasirumah;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import java.text.DateFormat;
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
        String[] text=lines(header);
        int width=canvas.getWidth(),height=canvas.getHeight();
        float density=Math.max(1f,Math.min(width,height)/720f);
        Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);fill.setColor(0xaa000000);
        Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(1.5f*density);stroke.setColor(0x66ffffff);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.WHITE);paint.setTextSize(22f*density);paint.setFakeBoldText(true);
        float pad=16f*density,line=28f*density,max=0;
        for(String item:text)max=Math.max(max,paint.measureText(item));
        float boxWidth=max+pad*2,boxHeight=line*text.length+pad;
        float left=pad,top=height-boxHeight-pad,right=Math.min(width-pad,left+boxWidth),bottom=height-pad;
        RectF rect=new RectF(left,top,right,bottom);
        canvas.drawRoundRect(rect,10f*density,10f*density,fill);
        canvas.drawRoundRect(rect,10f*density,10f*density,stroke);
        float y=top+pad+paint.getTextSize();
        for(String item:text){canvas.drawText(item,left+pad,y,paint);y+=line;}
    }
}
