package salve.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import com.salve.app.R;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Experimental layered preview. Texture decoding happens off the UI thread, once per attachment. */
public final class ArticulatedPoseView extends View {
    private static final int COLS=32, ROWS=48;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private List<Layer> layers=new ArrayList<>();
    private ArticulatedPose pose=new ArticulatedPose();
    private boolean showJoints;
    private int generation;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private String message="Cargando capas…";
    private static final class Layer {
        final Bitmap bitmap;
        final ArticulatedRig.Part part;
        final float[] rest=new float[(COLS+1)*(ROWS+1)*2], vertices=new float[rest.length];
        Layer(Bitmap bitmap,ArticulatedRig.Part part) {
            this.bitmap=bitmap;this.part=part;
            for(int y=0;y<=ROWS;y++)for(int x=0;x<=COLS;x++)
                ArticulatedRig.bind(part,x*bitmap.getWidth()/(float)COLS,y*bitmap.getHeight()/(float)ROWS,rest,2*(y*(COLS+1)+x));
        }
    }
    public ArticulatedPoseView(Context context) { super(context);setContentDescription("Vista experimental de la postura de Salve"); }
    public void setPose(ArticulatedPose value) { pose=value;invalidate(); }
    public void showJoints(boolean value) {showJoints=value;invalidate();}
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();final int request=++generation;
        new Thread(() -> {
            List<Layer> loaded=new ArrayList<>();Bitmap original=null;
            try {
                BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
                original=BitmapFactory.decodeResource(getResources(),R.drawable.salve_imagen,options);
                if(original==null)throw new IOException("Falta la imagen original");
                for(ArticulatedRig.Part part:ArticulatedRig.Part.values()) {
                    Bitmap bitmap;
                    switch(part) {
                        case HEAD:bitmap=Bitmap.createBitmap(original,0,0,1024,390);break;
                        case LEFT_LEG:bitmap=Bitmap.createBitmap(original,260,1035,225,501);break;
                        case RIGHT_LEG:bitmap=Bitmap.createBitmap(original,485,1035,225,501);break;
                        default:
                            String file=part==ArticulatedRig.Part.HAIR?"hair":part==ArticulatedRig.Part.TORSO?"torso":part==ArticulatedRig.Part.LEFT_ARM?"arm-left":"arm-right";
                            try(InputStream in=getContext().getAssets().open("avatar/articulated/"+file+".png")) { bitmap=BitmapFactory.decodeStream(in); }
                    }
                    if(bitmap==null)throw new IOException("Capa ilegible: "+part);
                    loaded.add(new Layer(bitmap,part));
                }
                main.post(() -> {if(request!=generation){release(loaded);return;}release(layers);layers=loaded;message="";invalidate();});
            } catch(IOException|RuntimeException error) {
                release(loaded);Log.e("SalvePose","No se pudieron cargar las capas",error);
                main.post(() -> {if(request==generation){message="No se pudieron cargar las capas. Vuelve a abrir el taller.";invalidate();}});
            } finally {if(original!=null)original.recycle();}
        },"salve-pose-textures").start();
    }
    @Override protected void onDetachedFromWindow() {generation++;release(layers);layers=new ArrayList<>();super.onDetachedFromWindow();}
    private static void release(List<Layer> items) {for(Layer layer:items)layer.bitmap.recycle();}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);canvas.drawColor(0xFF192637);
        if(layers.isEmpty()){paint.setColor(0xFFE0EEEE);paint.setTextSize(14*getResources().getDisplayMetrics().scaledDensity);canvas.drawText(message,12,40,paint);return;}
        float scale=Math.min(getWidth()/1700f,getHeight()/1800f);
        canvas.save();canvas.translate((getWidth()-1024*scale)/2,100*scale);canvas.scale(scale,scale);
        ArticulatedRig.Frame frame=new ArticulatedRig.Frame(pose);
        paint.setColor(0xFFFFFFFF);
        for(Layer layer:layers) {
            for(int i=0;i<layer.rest.length;i+=2)frame.point(layer.part,layer.rest[i],layer.rest[i+1],layer.vertices,i);
            canvas.drawBitmapMesh(layer.bitmap,COLS,ROWS,layer.vertices,0,null,0,paint);
        }
        if(showJoints) {
            float[] j=frame.joints();paint.setColor(0xFFFFCC66);paint.setStrokeWidth(5);
            int[][] bones={{0,1},{1,2},{2,3},{0,4},{4,5},{5,6},{7,8},{9,10},{7,9}};
            for(int[] b:bones)canvas.drawLine(j[b[0]*2],j[b[0]*2+1],j[b[1]*2],j[b[1]*2+1],paint);
            for(int i=0;i<j.length;i+=2)canvas.drawCircle(j[i],j[i+1],9,paint);
        }
        canvas.restore();
    }
}
