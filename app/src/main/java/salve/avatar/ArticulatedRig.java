package salve.avatar;

import static salve.avatar.ArticulatedPose.Joint.*;

/** Forward kinematics and continuous two-bone skinning for separately calibrated frontal layers. */
public final class ArticulatedRig {
    public enum Part { HAIR, LEFT_LEG, RIGHT_LEG, TORSO, LEFT_ARM, RIGHT_ARM, HEAD }
    public static final class Transform {
        public final float a,b,c,d,tx,ty;
        Transform(float a,float b,float c,float d,float tx,float ty) { this.a=a;this.b=b;this.c=c;this.d=d;this.tx=tx;this.ty=ty; }
        static Transform identity() { return new Transform(1,0,0,1,0,0); }
        static Transform rotate(float degrees,float x,float y) {
            float rad=(float)Math.toRadians(degrees),c=(float)Math.cos(rad),s=(float)Math.sin(rad);
            return new Transform(c,s,-s,c,x-c*x+s*y,y-s*x-c*y);
        }
        Transform then(Transform child) { return new Transform(a*child.a+c*child.b,b*child.a+d*child.b,
                a*child.c+c*child.d,b*child.c+d*child.d,a*child.tx+c*child.ty+tx,b*child.tx+d*child.ty+ty); }
        public float x(float x,float y) { return a*x+c*y+tx; }
        public float y(float x,float y) { return b*x+d*y+ty; }
    }
    public static final class Frame {
        public final Transform torso,head,leftUpper,leftForearm,rightUpper,rightForearm,leftThigh,leftCalf,rightThigh,rightCalf;
        public final float groundOffset;
        public Frame(ArticulatedPose p) {
            torso=Transform.rotate(p.angle(TORSO),480,1000);
            head=torso.then(Transform.rotate(p.angle(HEAD),479,390));
            leftUpper=torso.then(Transform.rotate(p.angle(LEFT_SHOULDER),363,444));
            leftForearm=leftUpper.then(Transform.rotate(p.angle(LEFT_ELBOW),310,630));
            rightUpper=torso.then(Transform.rotate(p.angle(RIGHT_SHOULDER),591,443));
            rightForearm=rightUpper.then(Transform.rotate(p.angle(RIGHT_ELBOW),645,630));
            leftThigh=Transform.rotate(p.angle(LEFT_HIP),399,1035);
            leftCalf=leftThigh.then(Transform.rotate(p.angle(LEFT_KNEE),410,1190));
            rightThigh=Transform.rotate(p.angle(RIGHT_HIP),535,1035);
            rightCalf=rightThigh.then(Transform.rotate(p.angle(RIGHT_KNEE),567,1190));
            // Keep the lowest sole at the reference floor; this is not a collision/physics solver.
            groundOffset=1536-Math.max(leftCalf.y(390,1536),rightCalf.y(610,1536));
        }
        public void point(Part part,float x,float y,float[] out,int i) {
            Transform first=torso,second=torso;float blend=0;
            switch(part) {
                case HAIR: case HEAD: first=head;second=head;break;
                case LEFT_ARM: first=leftUpper;second=leftForearm;blend=smooth(565,695,y);break;
                case RIGHT_ARM:first=rightUpper;second=rightForearm;blend=smooth(565,695,y);break;
                case LEFT_LEG:first=leftThigh;second=leftCalf;blend=smooth(1125,1255,y);break;
                case RIGHT_LEG:first=rightThigh;second=rightCalf;blend=smooth(1125,1255,y);break;
                default:break;
            }
            out[i]=first.x(x,y)*(1-blend)+second.x(x,y)*blend;
            out[i+1]=first.y(x,y)*(1-blend)+second.y(x,y)*blend+groundOffset;
        }
        public float[] joints() {
            float[] out=new float[22];int i=0;
            float[][] points={{479,390},{363,444},{310,630},{255,810},{591,443},{645,630},{695,815},{399,1035},{410,1190},{535,1035},{567,1190}};
            Transform[] t={torso,torso,leftUpper,leftForearm,torso,rightUpper,rightForearm,leftThigh,leftThigh,rightThigh,rightThigh};
            for(int n=0;n<points.length;n++){out[i++]=t[n].x(points[n][0],points[n][1]);out[i++]=t[n].y(points[n][0],points[n][1])+groundOffset;}
            return out;
        }
    }
    /** Initial artwork calibration is explicit, separate from animated joint rotations. */
    public static void bind(Part part,float u,float v,float[] out,int i) {
        switch(part) {
            case HAIR: out[i]=130+(u-7)*740/1016;out[i+1]=35+(v-43)*785/1443;break;
            case TORSO:out[i]=232+(u-28)*476/969;out[i+1]=385+(v-164)*650/1178;break;
            case LEFT_ARM:align(u,v,372,461,238,988,363,444,255,810,out,i);break;
            case RIGHT_ARM:align(u,v,626,412,767,972,591,443,695,815,out,i);break;
            case LEFT_LEG:out[i]=u+260;out[i+1]=v+1035;break;
            case RIGHT_LEG:out[i]=u+485;out[i+1]=v+1035;break;
            default:out[i]=u;out[i+1]=v;break;
        }
    }
    private static void align(float x,float y,float sx,float sy,float ex,float ey,float tx,float ty,float ux,float uy,float[] out,int i) {
        float dx=ex-sx,dy=ey-sy,den=dx*dx+dy*dy;
        float a=((ux-tx)*dx+(uy-ty)*dy)/den,b=((uy-ty)*dx-(ux-tx)*dy)/den;
        out[i]=tx+a*(x-sx)-b*(y-sy);out[i+1]=ty+b*(x-sx)+a*(y-sy);
    }
    static float smooth(float a,float b,float value) {float t=Math.max(0,Math.min(1,(value-a)/(b-a)));return t*t*(3-2*t);}
    private ArticulatedRig() { }
}
