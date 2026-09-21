package salve.avatar;

/** Bounded material styling. Faces, hair, skin, hands and legs are outside this color/pattern mask. */
public final class AvatarClothingStyle {
    private AvatarClothingStyle() { }
    public static int pixel(int x,int y,int argb,int palette,String pattern) {
        return pixel("original_dress",x,y,argb,palette,pattern);
    }
    public static int pixel(String template,int x,int y,int argb,int palette,String pattern) {
        if(palette==0&&"NONE".equals(pattern))return argb;
        int alpha=argb>>>24,r=(argb>>>16)&255,g=(argb>>>8)&255,b=argb&255;
        if(alpha<128||y<420||y>960)return argb;
        boolean pajamas="pajamas".equals(template);
        float bottom=pajamas?860:960,t=(y-420)/(bottom-420);
        float left=355-(pajamas?55:100)*t,right=600+(pajamas?35:90)*t;
        float weight=smooth(0,36,x-left)*smooth(0,36,right-x)*smooth(420,450,y)*smooth(0,pajamas?90:65,bottom-y);
        weight*=1-(1-smooth(555,595,y))*(1-smooth(390,425,x));
        weight*=1-(1-smooth(570,610,y))*smooth(525,548,x);
        if(weight<=0)return argb;
        if(Math.min(r,Math.min(g,b))>230 || (r>g+12&&r>b+18))return argb;
        if(palette!=0) {
            float brightness=Math.max(.24f,Math.min(1.15f,(.2126f*r+.7152f*g+.0722f*b)/150));
            r=blend(r,Math.min(255,((palette>>>16)&255)*brightness),.55f*weight);
            g=blend(g,Math.min(255,((palette>>>8)&255)*brightness),.55f*weight);
            b=blend(b,Math.min(255,(palette&255)*brightness),.55f*weight);
        }
        boolean mark=false;
        if("STRIPES".equals(pattern))mark=y%32<3;
        else if("STARS".equals(pattern)) {
            int dx=Math.abs((x+40)%80-40),dy=Math.abs((y+40)%80-40);
            mark=(dx<2&&dy<11)||(dy<2&&dx<9)||(dx+dy<7);
        }
        if(mark){r=blend(r,255,.5f*weight);g=blend(g,255,.5f*weight);b=blend(b,255,.5f*weight);}
        return alpha<<24|r<<16|g<<8|b;
    }
    /** Premultiplied alpha blending avoids dark transparent halos at the neck/hair transition. */
    public static int blendHead(int original,int garment,float weight) {
        if(weight>=1)return original;if(weight<=0)return garment;
        float a=(original>>>24)*weight,b=(garment>>>24)*(1-weight),sum=a+b;
        if(sum<.5f)return 0;
        int r=Math.round((((original>>>16)&255)*a+((garment>>>16)&255)*b)/sum);
        int g=Math.round((((original>>>8)&255)*a+((garment>>>8)&255)*b)/sum);
        int blue=Math.round(((original&255)*a+(garment&255)*b)/sum);
        return Math.round(sum)<<24|r<<16|g<<8|blue;
    }
    private static int blend(int current,float target,float amount){return Math.round(current*(1-amount)+target*amount);}
    private static float smooth(float start,float end,float value){float t=Math.max(0,Math.min(1,(value-start)/(end-start)));return t*t*(3-2*t);}
}
