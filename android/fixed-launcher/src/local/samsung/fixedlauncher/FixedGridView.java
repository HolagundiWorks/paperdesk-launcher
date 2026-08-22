package local.samsung.fixedlauncher;

import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class FixedGridView extends View {
    private static final long TOTAL=1500000L;
    private static final String[] APPS={"org.fossify.gallery","org.fossify.calendar","org.fossify.clock","org.fossify.notes","org.tasks","org.fossify.math","org.fossify.filemanager"};
    private final PackageManager pm;
    private final Paint fill=new Paint(1), line=new Paint(1);
    private float scroll,downX,downY,startScroll; private long downAt,endAt;
    private boolean dockTouch,clockTouch,running; private long remaining=TOTAL;

    FixedGridView(Context c){super(c);pm=c.getPackageManager();setBackgroundColor(Color.BLACK);setWillNotDraw(false);line.setStyle(Paint.Style.STROKE);line.setStrokeCap(Paint.Cap.ROUND);}

    protected void onDraw(Canvas c){super.onDraw(c);tick();clock(c);date(c);dock(c);postInvalidateDelayed(1000);}

    private void clock(Canvas c){
        float x=getWidth()*.29f,y=getHeight()*.29f,r=Math.min(dp(76),getWidth()*.19f);
        line.setStrokeWidth(dp(2));line.setColor(Color.rgb(57,57,57));c.drawCircle(x,y,r,line);
        float f=Math.max(0,Math.min(1,remaining/(float)TOTAL)),sweep=360*f;
        line.setStrokeWidth(dp(3));line.setColor(Color.rgb(255,92,53));
        RectF ring=new RectF(x-r-dp(7),y-r-dp(7),x+r+dp(7),y+r+dp(7));c.drawArc(ring,-90,sweep,false,line);
        double e=Math.toRadians(-90+sweep);fill.setColor(Color.rgb(255,92,53));c.drawCircle(x+(r+dp(7))*(float)Math.cos(e),y+(r+dp(7))*(float)Math.sin(e),dp(4),fill);
        Calendar now=Calendar.getInstance();float sec=now.get(Calendar.SECOND),min=now.get(Calendar.MINUTE)+sec/60,hour=now.get(Calendar.HOUR)+min/60;
        hand(c,x,y,r*.5f,hour*30-90,dp(4),Color.WHITE);hand(c,x,y,r*.72f,min*6-90,dp(3),Color.WHITE);hand(c,x,y,r*.78f,sec*6-90,dp(1),Color.rgb(255,92,53));
        fill.setColor(Color.rgb(255,92,53));c.drawCircle(x,y,dp(4),fill);
        fill.setTypeface(Typeface.create("sans",0));fill.setTextAlign(Paint.Align.CENTER);fill.setTextSize(dp(13));fill.setColor(Color.rgb(198,198,198));long t=Math.max(0,remaining/1000);
        c.drawText(String.format(Locale.US,"%02d:%02d · Pomodoro",t/60,t%60),x,y+r+dp(35),fill);
    }

    private void date(Canvas c){float x=getWidth()*.29f,y=getHeight()*.29f,r=Math.min(dp(76),getWidth()*.19f);fill.setTypeface(Typeface.create("sans",0));fill.setTextAlign(Paint.Align.LEFT);fill.setTextSize(dp(20));fill.setColor(Color.WHITE);c.drawText(new SimpleDateFormat("dd MMMM yyyy",Locale.getDefault()).format(new Date()),x+r+dp(24),y+dp(7),fill);}

    private void dock(Canvas c){RectF d=dockRect();float cell=d.width()/5;c.save();c.clipRect(d);c.translate(-scroll,0);for(int i=0;i<APPS.length;i++){float l=d.left+i*cell;RectF tile=new RectF(l+dp(4),d.top+dp(4),l+cell-dp(4),d.bottom-dp(4));fill.setColor(Color.rgb(22,22,22));c.drawRoundRect(tile,dp(4),dp(4),fill);line.setStrokeWidth(dp(1));line.setColor(Color.rgb(57,57,57));c.drawRoundRect(tile,dp(4),dp(4),line);icon(c,APPS[i],tile);}c.restore();}
    private void icon(Canvas c,String p,RectF t){try{Drawable d=pm.getApplicationIcon(p);int z=Math.round(Math.min(t.width(),t.height())*.56f),l=Math.round(t.centerX()-z/2f),top=Math.round(t.centerY()-z/2f);d.setBounds(l,top,l+z,top+z);d.draw(c);}catch(PackageManager.NameNotFoundException ignored){}}
    private RectF dockRect(){float b=getHeight()-dp(28);return new RectF(dp(8),b-dp(86),getWidth()-dp(8),b);}

    public boolean onTouchEvent(MotionEvent e){float cx=getWidth()*.29f,cy=getHeight()*.29f,cr=Math.min(dp(95),getWidth()*.24f);switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();downAt=SystemClock.uptimeMillis();startScroll=scroll;dockTouch=dockRect().contains(downX,downY);clockTouch=dist(downX,downY,cx,cy)<=cr;return true;
        case MotionEvent.ACTION_MOVE:if(dockTouch){scroll=clamp(startScroll+downX-e.getX());invalidate();}return true;
        case MotionEvent.ACTION_UP:float travel=dist(downX,downY,e.getX(),e.getY());if(clockTouch&&travel<dp(12)){if(SystemClock.uptimeMillis()-downAt>=650)reset();else toggle();}else if(dockTouch&&travel<dp(10)){int i=(int)((e.getX()-dockRect().left+scroll)/(dockRect().width()/5));if(i>=0&&i<APPS.length)launch(APPS[i]);}dockTouch=clockTouch=false;return true;
        case MotionEvent.ACTION_CANCEL:dockTouch=clockTouch=false;return true;default:return false;}}

    private float clamp(float v){return Math.max(0,Math.min((APPS.length-5)*dockRect().width()/5,v));}
    private void toggle(){tick();if(remaining<=0)remaining=TOTAL;running=!running;if(running)endAt=SystemClock.elapsedRealtime()+remaining;invalidate();}
    private void reset(){running=false;remaining=TOTAL;invalidate();}
    private void tick(){if(running){remaining=Math.max(0,endAt-SystemClock.elapsedRealtime());if(remaining==0)running=false;}}
    private void hand(Canvas c,float x,float y,float len,float deg,float w,int color){double a=Math.toRadians(deg);line.setStrokeWidth(w);line.setColor(color);c.drawLine(x,y,x+len*(float)Math.cos(a),y+len*(float)Math.sin(a),line);}
    private void launch(String p){Intent i=pm.getLaunchIntentForPackage(p);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);getContext().startActivity(i);}}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float dist(float a,float b,float c,float d){float x=a-c,y=b-d;return(float)Math.sqrt(x*x+y*y);}
}
