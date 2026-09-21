import salve.avatar.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
/** Desktop visual check using the same bind coordinates, skinning and grid as Android. */
public class RenderArticulatedPose {
 public static void main(String[] args)throws Exception{
  File root=new File(args[0]);BufferedImage original=ImageIO.read(new File(root,"app/src/main/res/drawable/salve_imagen.png"));
  BufferedImage sheet=new BufferedImage(1500,380,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(25,38,55));g.fillRect(0,0,1500,380);int n=0;
  for(ArticulatedPose.Preset preset:ArticulatedPose.Preset.values()){
   Graphics2D panel=(Graphics2D)g.create();panel.translate(n++*300,0);panel.setColor(Color.WHITE);panel.drawString(preset.name(),20,25);panel.translate(60,55);panel.scale(.18,.18);
   ArticulatedRig.Frame frame=new ArticulatedRig.Frame(ArticulatedPose.preset(preset));
   for(ArticulatedRig.Part part:ArticulatedRig.Part.values()){
    BufferedImage img;
    switch(part){case HEAD:img=original.getSubimage(0,0,1024,390);break;case LEFT_LEG:img=original.getSubimage(260,1035,225,501);break;case RIGHT_LEG:img=original.getSubimage(485,1035,225,501);break;default:String name=part==ArticulatedRig.Part.HAIR?"hair":part==ArticulatedRig.Part.TORSO?"torso":part==ArticulatedRig.Part.LEFT_ARM?"arm-left":"arm-right";img=ImageIO.read(new File(root,"app/src/main/assets/avatar/articulated/"+name+".png"));}
    float[] dst=new float[33*49*2],bind=new float[2];
    for(int y=0;y<=48;y++)for(int x=0;x<=32;x++){ArticulatedRig.bind(part,x*img.getWidth()/32f,y*img.getHeight()/48f,bind,0);frame.point(part,bind[0],bind[1],dst,2*(y*33+x));}
    for(int y=0;y<48;y++)for(int x=0;x<32;x++){triangle(panel,img,dst,x,y,0);triangle(panel,img,dst,x,y,1);}
   }panel.dispose();
  }g.dispose();ImageIO.write(sheet,"png",new File(args[1]));
 }
 static void triangle(Graphics2D target,BufferedImage img,float[] dst,int x,int y,int half)throws Exception{
  int[] xs=half==0?new int[]{x,x+1,x}:new int[]{x+1,x+1,x};int[] ys=half==0?new int[]{y,y,y+1}:new int[]{y,y+1,y+1};
  double[] u=new double[3],v=new double[3],a=new double[3],b=new double[3];for(int i=0;i<3;i++){u[i]=xs[i]*img.getWidth()/32.;v[i]=ys[i]*img.getHeight()/48.;a[i]=dst[2*(ys[i]*33+xs[i])];b[i]=dst[2*(ys[i]*33+xs[i])+1];}
  AffineTransform src=new AffineTransform(u[1]-u[0],v[1]-v[0],u[2]-u[0],v[2]-v[0],u[0],v[0]);AffineTransform transform=new AffineTransform(a[1]-a[0],b[1]-b[0],a[2]-a[0],b[2]-b[0],a[0],b[0]);transform.concatenate(src.createInverse());
  Path2D path=new Path2D.Double();path.moveTo(a[0],b[0]);path.lineTo(a[1],b[1]);path.lineTo(a[2],b[2]);path.closePath();Graphics2D g=(Graphics2D)target.create();g.clip(path);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(img,transform,null);g.dispose();
 }
}
