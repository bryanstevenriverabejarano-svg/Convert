package salve.avatar;

/** One shared gait clock, driven by real scene displacement rather than arbitrary device uptime. */
public final class AvatarLocomotion {
    private long lastTime;
    private float previousX=Float.NaN, phase, blend;
    public void advance(long now,float x,boolean walking) {
        if(now<=lastTime || !Float.isFinite(x))return;
        float elapsed=lastTime==0?0:Math.min(.1f,(now-lastTime)/1000f);
        if(Float.isFinite(previousX) && walking) {
            float distance=Math.min(.03f,Math.abs(x-previousX));
            phase=(phase+distance/.10f*(float)(Math.PI*2))%(float)(Math.PI*2);
        }
        // Pauses and drags do not advance a fictitious walk cycle.
        previousX=x;lastTime=now;
        float target=walking?1:0;
        blend+= (target-blend)*(1-(float)Math.exp(-elapsed*(walking?10:14)));
        if(!walking&&blend<.0005f)blend=0;
    }
    public float stride() { return (float)Math.sin(phase)*blend; }
    public float lift() { return Math.abs((float)Math.sin(phase))*blend; }
    public float blend() { return blend; }
    public float phase() { return phase; }
}
