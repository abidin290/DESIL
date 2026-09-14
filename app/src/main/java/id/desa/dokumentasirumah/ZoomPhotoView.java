package id.desa.dokumentasirumah;
import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Full-screen preview with pinch, drag and double tap; visible buttons use the same zoom. */
final class ZoomPhotoView extends View {
    private Bitmap bitmap;
    private float zoom=1, offsetX,offsetY,lastX,lastY;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector scale;
    private final GestureDetector gestures;
    ZoomPhotoView(Context c,Bitmap b){
        super(c);bitmap=b;setContentDescription("Preview foto. Cubit untuk memperbesar, geser untuk melihat detail.");
        scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoomBy(d.getScaleFactor());return true;}});
        gestures=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(android.view.MotionEvent e){return true;}@Override public boolean onDoubleTap(android.view.MotionEvent e){if(zoom>1)reset();else zoomBy(2);return true;}});
    }
    void zoomBy(float factor){zoom=Math.max(1,Math.min(6,zoom*factor));clamp();invalidate();}
    void reset(){zoom=1;offsetX=offsetY=0;invalidate();}
    private float fit(){return bitmap==null?1:Math.min(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());}
    private void clamp(){if(bitmap==null)return;float w=bitmap.getWidth()*fit()*zoom,h=bitmap.getHeight()*fit()*zoom;float x=Math.max(0,(w-getWidth())/2),y=Math.max(0,(h-getHeight())/2);offsetX=Math.max(-x,Math.min(x,offsetX));offsetY=Math.max(-y,Math.min(y,offsetY));}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){clamp();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawColor(0xff102924);if(bitmap==null)return;float s=fit()*zoom;canvas.save();canvas.translate(getWidth()/2f+offsetX,getHeight()/2f+offsetY);canvas.scale(s,s);canvas.drawBitmap(bitmap,-bitmap.getWidth()/2f,-bitmap.getHeight()/2f,paint);canvas.restore();}
    @Override public boolean onTouchEvent(MotionEvent e){scale.onTouchEvent(e);gestures.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();}else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){if(!scale.isInProgress() && e.getPointerCount()==1){offsetX+=e.getX()-lastX;offsetY+=e.getY()-lastY;clamp();invalidate();}lastX=e.getX();lastY=e.getY();}else if(e.getActionMasked()==MotionEvent.ACTION_POINTER_UP){int i=e.getActionIndex()==0?1:0;lastX=e.getX(i);lastY=e.getY(i);}else if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;}
    @Override public boolean performClick(){super.performClick();return true;}
    void release(){if(bitmap!=null){bitmap.recycle();bitmap=null;}}
}
