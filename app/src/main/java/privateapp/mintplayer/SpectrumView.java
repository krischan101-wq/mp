package privateapp.mintplayer;
import android.content.Context;import android.graphics.*;import android.view.View;import android.media.audiofx.Visualizer;
public class SpectrumView extends View {
 private Visualizer v;private int session=-1;private final float[] bars=new float[36];private final Paint p=new Paint(3); public boolean live=false;
 public SpectrumView(Context c){super(c);}
 public void attach(int id){if(session==id&&v!=null)return;release();try{v=new Visualizer(id);session=id;v.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);v.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){public void onWaveFormDataCapture(Visualizer x,byte[] b,int r){}public void onFftDataCapture(Visualizer x,byte[] b,int r){synchronized(bars){for(int i=0;i<bars.length;i++){int k=2+2*i;if(k+1<b.length)bars[i]=Math.min(1,(float)Math.hypot(b[k],b[k+1])/75f);}}postInvalidate();}},Visualizer.getMaxCaptureRate()/2,false,true);v.setEnabled(true);live=true;}catch(Exception e){release();}}
 public void release(){if(v!=null){try{v.release();}catch(Exception e){}v=null;}session=-1;live=false;java.util.Arrays.fill(bars,0);invalidate();}
 protected void onDraw(Canvas c){super.onDraw(c);float step=getWidth()/40f; synchronized(bars){for(int i=0;i<36;i++){p.setColor(Color.rgb(110,231,196));p.setAlpha(live?200:55);float h=Math.max(4,bars[i]*(getHeight()-12));float x=(i+2)*step;c.drawRoundRect(x,getHeight()/2f-h/2,x+step*.55f,getHeight()/2f+h/2,5,5,p);}}}
}
