package local.samsung.fixedlauncher;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.*;
import java.text.SimpleDateFormat;
import java.util.*;

final class FixedGridView extends View {
    private static final long DEFAULT_TIMER=1200000L;
    private static final long MAX_TIMER=3600000L;
    private static final String[] APPS={"org.fossify.gallery","org.fossify.calendar","org.fossify.clock","org.fossify.notes","org.tasks","org.fossify.math","org.fossify.filemanager"};
    private static final String[] ICONS={"fossify_gallery","fossify_calendar","fossify_clock","fossify_notes","tasks","fossify_calculator","fossify_filemanager"};
    private static final String ICON_PACK="com.donnnno.arcticons";
    private final PackageManager pm;
    private final Paint fill=new Paint(1), line=new Paint(1);
    private float scroll,downX,downY,startScroll; private long downAt,endAt,lastClockTap;
    private boolean dockTouch,clockTouch,ringTouch,running; private long configured=DEFAULT_TIMER,remaining=DEFAULT_TIMER;
    private final Runnable singleTap=new Runnable(){public void run(){toggle();}};

    FixedGridView(Context c){super(c);pm=c.getPackageManager();setBackgroundColor(Color.BLACK);setWillNotDraw(false);line.setStyle(Paint.Style.STROKE);line.setStrokeCap(Paint.Cap.ROUND);}

    protected void onDraw(Canvas c){super.onDraw(c);tick();clock(c);date(c);dock(c);postInvalidateDelayed(1000);}

    private void clock(Canvas c){
        float x=getWidth()*.29f,y=getHeight()*.29f,r=Math.min(dp(76),getWidth()*.19f);
        line.setStrokeWidth(dp(2));line.setColor(Color.rgb(57,57,57));c.drawCircle(x,y,r,line);
        float f=Math.max(0,Math.min(1,remaining/(float)MAX_TIMER)),sweep=360*f;
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

    private void dock(Canvas c){RectF d=dockRect();float cell=d.width()/5;c.save();c.clipRect(d);c.translate(-scroll,0);for(int i=0;i<APPS.length;i++){float l=d.left+i*cell;RectF slot=new RectF(l,d.top,l+cell,d.bottom);icon(c,i,slot);}c.restore();}
    private void icon(Canvas c,int index,RectF t){try{Resources r=pm.getResourcesForApplication(ICON_PACK);int id=r.getIdentifier(ICONS[index],"drawable",ICON_PACK);Drawable d=id==0?pm.getApplicationIcon(APPS[index]):r.getDrawable(id,null);int z=Math.round(Math.min(t.width(),t.height())*.58f),l=Math.round(t.centerX()-z/2f),top=Math.round(t.centerY()-z/2f);d.setBounds(l,top,l+z,top+z);d.draw(c);}catch(Exception ignored){}}
    private RectF dockRect(){float b=getHeight()-dp(28);return new RectF(dp(8),b-dp(86),getWidth()-dp(8),b);}

    public boolean onTouchEvent(MotionEvent e){float cx=getWidth()*.29f,cy=getHeight()*.29f,r=Math.min(dp(76),getWidth()*.19f),distance=dist(e.getX(),e.getY(),cx,cy);switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();downAt=SystemClock.uptimeMillis();startScroll=scroll;ringTouch=Math.abs(distance-(r+dp(7)))<=dp(22);dockTouch=!ringTouch&&dockRect().contains(downX,downY);clockTouch=!ringTouch&&distance<=r;return true;
        case MotionEvent.ACTION_MOVE:if(ringTouch){setTimerFromDial(e.getX(),e.getY(),cx,cy);}else if(dockTouch){scroll=clamp(startScroll+downX-e.getX());invalidate();}return true;
        case MotionEvent.ACTION_UP:float travel=dist(downX,downY,e.getX(),e.getY());if(ringTouch){setTimerFromDial(e.getX(),e.getY(),cx,cy);}else if(clockTouch&&travel<dp(12)){clockTap();}else if(dockTouch&&travel<dp(10)){int i=(int)((e.getX()-dockRect().left+scroll)/(dockRect().width()/5));if(i>=0&&i<APPS.length)launch(APPS[i]);}dockTouch=clockTouch=ringTouch=false;return true;
        case MotionEvent.ACTION_CANCEL:dockTouch=clockTouch=ringTouch=false;return true;default:return false;}}

    private float clamp(float v){return Math.max(0,Math.min((APPS.length-5)*dockRect().width()/5,v));}
    private void clockTap(){long now=SystemClock.uptimeMillis();if(now-lastClockTap<=1500){removeCallbacks(singleTap);lastClockTap=0;reset();}else{lastClockTap=now;removeCallbacks(singleTap);postDelayed(singleTap,300);}}
    private void setTimerFromDial(float x,float y,float cx,float cy){double angle=Math.toDegrees(Math.atan2(y-cy,x-cx))+90;if(angle<=0)angle+=360;int minutes=Math.max(1,Math.min(60,(int)Math.round(angle/6)));running=false;configured=minutes*60000L;remaining=configured;invalidate();}
    private void toggle(){tick();if(remaining<=0)remaining=configured;running=!running;if(running)endAt=SystemClock.elapsedRealtime()+remaining;invalidate();}
    private void reset(){running=false;remaining=configured;invalidate();}
    private void tick(){if(running){remaining=Math.max(0,endAt-SystemClock.elapsedRealtime());if(remaining==0)running=false;}}
    private void hand(Canvas c,float x,float y,float len,float deg,float w,int color){double a=Math.toRadians(deg);line.setStrokeWidth(w);line.setColor(color);c.drawLine(x,y,x+len*(float)Math.cos(a),y+len*(float)Math.sin(a),line);}
    private void launch(String p){Intent i=pm.getLaunchIntentForPackage(p);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);getContext().startActivity(i);}}
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float dist(float a,float b,float c,float d){float x=a-c,y=b-d;return(float)Math.sqrt(x*x+y*y);}
}
