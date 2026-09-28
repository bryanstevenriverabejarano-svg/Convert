package salve.avatar;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded preview: one decoded view, frontal mesh only. No fabricated side/back rig. */
public final class CoreModelView extends View {
    private final Handler main=new Handler(Looper.getMainLooper());
    private ExecutorService worker;
    private volatile long generation;
    private boolean attached, joints;
    private CoreViewCatalog.Entry selected=CoreViewCatalog.find("front");
    private Bitmap bitmap;
    private AvatarRig rig;
    private float[] vertices;
    private String message="Cargando núcleo…";
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private float head,arms,stride,blink;
    public CoreModelView(Context context){super(context);setContentDescription("Vista del núcleo de Salve");}
    public void select(String id){selected=CoreViewCatalog.find(id);if(attached)load();}
    public void showJoints(boolean value){joints=value;invalidate();}
    public void setMotion(float head,float arms,float stride,float blink){
        this.head=AvatarRig.limit(head,-7,7);this.arms=AvatarRig.limit(arms,-25,25);
        this.stride=AvatarRig.limit(stride,-1,1);this.blink=AvatarRig.limit(blink,0,1);invalidate();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;worker=Executors.newSingleThreadExecutor();load();}
    @Override protected void onDetachedFromWindow(){attached=false;generation++;if(worker!=null)worker.shutdownNow();bitmap=null;rig=null;vertices=null;super.onDetachedFromWindow();}
    private void load(){
        final long request=++generation;final CoreViewCatalog.Entry entry=selected;
        bitmap=null;rig=null;vertices=null;message="Cargando "+entry.label+"…";invalidate();
        // Superseded requests exit before decoding; at most one 1024px preview is retained.
        worker.execute(()->{
            if(request!=generation)return;
            Bitmap decoded=null;
            try(InputStream in=getContext().getAssets().open(entry.path)){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
                decoded=BitmapFactory.decodeStream(in,null,options);
                if(decoded==null || decoded.getWidth()!=entry.width || decoded.getHeight()!=entry.height)
                    throw new IOException("Ilustración inválida");
                AvatarRig loadedRig=null;
                if("front".equals(entry.id))try(Reader reader=new InputStreamReader(getContext().getAssets().open("avatar/core/rig.json"),StandardCharsets.UTF_8)){
                    loadedRig=new AvatarRig(reader);
                }
                Bitmap ready=decoded;AvatarRig readyRig=loadedRig;
                main.post(()->{
                    if(!attached||request!=generation){ready.recycle();return;}
                    bitmap=ready;rig=readyRig;vertices=rig==null?null:new float[rig.vertexCount()*2];message="";
                    setContentDescription(entry.label+(rig==null?", ilustración de referencia":", malla frontal animable"));invalidate();
                });
            }catch(IOException|RuntimeException|OutOfMemoryError error){
                if(decoded!=null)decoded.recycle();
                main.post(()->{if(attached&&request==generation){message="No pude cargar esta vista. Selecciona otra para continuar.";invalidate();}});
            }
        });
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(0xFF182332);
        if(bitmap==null){paint.setColor(Color.WHITE);paint.setTextSize(32);canvas.drawText(message,16,48,paint);return;}
        float scale=Math.min(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());
        canvas.save();canvas.translate((getWidth()-bitmap.getWidth()*scale)/2,(getHeight()-bitmap.getHeight()*scale)/2);canvas.scale(scale,scale);
        paint.setColor(Color.WHITE);
        if(rig==null)canvas.drawBitmap(bitmap,0,0,paint);
        else {
            AvatarRig.Frame frame=rig.frame(head,0,0,blink,0,0,.5f,arms,-arms,stride,0,0);frame.fillVertices(vertices);
            canvas.drawBitmapMesh(bitmap,rig.columns,rig.rows,vertices,0,null,0,paint);
            if(joints){
                paint.setColor(0xFFFFCC55);paint.setStrokeWidth(3);
                float[][] chain={rig.headPivot,rig.leftShoulder,rig.leftElbow,rig.leftPalm,rig.leftHip,rig.leftKnee,rig.rightShoulder,rig.rightElbow,rig.rightPalm,rig.rightHip,rig.rightKnee};
                int[][] bones={{0,1},{1,2},{2,3},{1,4},{4,5},{0,6},{6,7},{7,8},{6,9},{9,10},{4,9}};
                float[][] points=new float[chain.length][2];for(int i=0;i<chain.length;i++)frame.point(chain[i][0],chain[i][1],points[i]);
                for(int[] b:bones)canvas.drawLine(points[b[0]][0],points[b[0]][1],points[b[1]][0],points[b[1]][1],paint);
                for(float[] p:points)canvas.drawCircle(p[0],p[1],6,paint);
            }
        }
        canvas.restore();
    }
}
