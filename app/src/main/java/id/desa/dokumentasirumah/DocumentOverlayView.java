package id.desa.dokumentasirumah;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class DocumentOverlayView extends View {
    private final Paint dim=new Paint(Paint.ANTI_ALIAS_FLAG), line=new Paint(Paint.ANTI_ALIAS_FLAG), text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String type;
    DocumentOverlayView(Context context,String type){
        super(context);this.type=type==null?"KTP":type;
        dim.setColor(0x88000000);
        line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(dp(2));line.setColor(Color.WHITE);
        text.setColor(Color.WHITE);text.setTextSize(dp(14));text.setFakeBoldText(true);text.setTextAlign(Paint.Align.LEFT);
    }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if("KK".equals(type)){
            canvas.drawRect(0,0,getWidth(),dp(52),dim);
            canvas.drawText(label(),dp(16),dp(34),text);
            return;
        }
        RectF frame=frame(getWidth(),getHeight(),type);
        canvas.drawRect(0,0,getWidth(),frame.top,dim);
        canvas.drawRect(0,frame.bottom,getWidth(),getHeight(),dim);
        canvas.drawRect(0,frame.top,frame.left,frame.bottom,dim);
        canvas.drawRect(frame.right,frame.top,getWidth(),frame.bottom,dim);
        if("KTP".equals(type))drawKtpTemplate(canvas,frame);else canvas.drawRoundRect(frame,dp(8),dp(8),line);
        canvas.drawText(label(),frame.left,frame.top-dp(12),text);
    }
    RectF frameOnView(){return frame(getWidth(),getHeight(),type);}
    static RectF frame(int width,int height,String type){
        String safeType=type==null?"KTP":type;
        float ratio;
        float maxW;
        float maxH;
        float topLift;
        if("KK".equals(safeType)){
            ratio=1.414f;
            maxW=width*0.96f;
            maxH=height*0.82f;
            topLift=0.01f;
        }else if("IDPEL".equals(safeType)){
            ratio=1.75f;
            maxW=width*0.88f;
            maxH=height*0.54f;
            topLift=0.05f;
        }else{
            ratio=85.60f/53.98f;
            maxW=width*0.78f;
            maxH=height*0.62f;
            topLift=0.04f;
        }
        float w=maxW,h=w/ratio;
        if(h>maxH){h=maxH;w=h*ratio;}
        float left=(width-w)/2f,top=(height-h)/2f-(height*topLift);
        float minTop=height*0.08f;
        if(top<minTop)top=minTop;
        return new RectF(left,top,left+w,top+h);
    }
    private String label(){
        if("KK".equals(type))return "Foto KK penuh dan jelas";
        if("IDPEL".equals(type))return "Letakkan IDPEL listrik di dalam bingkai";
        return "Letakkan KTP di dalam bingkai";
    }
    private void drawKtpTemplate(Canvas canvas,RectF frame){
        Paint ktpLine=new Paint(line);ktpLine.setColor(0xff14284f);ktpLine.setStrokeWidth(dp(2));
        Paint guideFill=new Paint(Paint.ANTI_ALIAS_FLAG);guideFill.setColor(0x66ffffff);
        canvas.drawRect(frame,ktpLine);

        float nikW=frame.width()*0.50f,nikH=frame.height()*0.10f;
        float nikLeft=frame.left+frame.width()*0.16f,nikTop=frame.top+frame.height()*0.13f;
        RectF nik=new RectF(nikLeft,nikTop,nikLeft+nikW,nikTop+nikH);
        canvas.drawRect(nik,guideFill);
        canvas.drawRect(nik,ktpLine);

        float photoW=frame.width()*0.27f,photoH=frame.height()*0.62f;
        float photoLeft=frame.right-frame.width()*0.03f-photoW,photoTop=frame.top+frame.height()*0.18f;
        RectF photo=new RectF(photoLeft,photoTop,photoLeft+photoW,photoTop+photoH);
        canvas.drawRect(photo,guideFill);
        canvas.drawRect(photo,ktpLine);

        Paint nikText=new Paint(text);nikText.setColor(Color.BLACK);nikText.setTextAlign(Paint.Align.CENTER);nikText.setTextSize(dp(13));nikText.setFakeBoldText(false);
        Paint.FontMetrics fm=nikText.getFontMetrics();
        canvas.drawText("NIK",nik.centerX(),nik.centerY()-(fm.ascent+fm.descent)/2f,nikText);
    }
}
