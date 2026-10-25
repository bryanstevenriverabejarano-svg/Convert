package salve.core.synergy;

/** Bounded computational priorities, not physical energy or feelings. */
public final class SynergyCore {
    public enum Event { NONE, ANSWER, FAILED, FOUND, NO_EVIDENCE, VERIFIED, REJECTED }
    public static final class State {
        public double energy = .65, curiosity = .55, uncertainty = .35, learning = .50;
        public long at, events, verified;
    }
    public static final class Device {
        public final int battery;
        public final boolean charging, save, lowRam, hot;
        public Device(int battery, boolean charging, boolean save, boolean lowRam, boolean hot) {
            this.battery=battery; this.charging=charging; this.save=save; this.lowRam=lowRam; this.hot=hot;
        }
    }
    private SynergyCore() {}
    private static double bounded(double x) {
        return Double.isFinite(x) ? Math.max(0, Math.min(1, x)) : .5;
    }
    public static State clean(State old) {
        State s = new State();
        if (old != null) {
            s.energy=bounded(old.energy); s.curiosity=bounded(old.curiosity);
            s.uncertainty=bounded(old.uncertainty); s.learning=bounded(old.learning);
            s.at=Math.max(0,old.at); s.events=Math.max(0,old.events);
            s.verified=Math.max(0,old.verified);
        }
        return s;
    }
    public static State advance(State old, Event event, boolean charging, long now) {
        State s = new State();
        if (old != null) {
            s.energy=bounded(old.energy); s.curiosity=bounded(old.curiosity);
            s.uncertainty=bounded(old.uncertainty); s.learning=bounded(old.learning);
            s.events=Math.max(0,old.events); s.verified=Math.max(0,old.verified);
            long dt=old.at>0&&now>old.at?Math.min(now-old.at,86400000):0;
            double hours=dt/3600000d;
            s.energy+=hours*(charging?.03:.01);
            s.curiosity+=hours*.008;
            s.uncertainty-=hours*.002;
        }
        switch(event) {
            case ANSWER: s.energy-=.02; break; // delivery is not proof of correctness
            case FAILED: s.energy-=.03; s.uncertainty+=.10; s.learning+=.05; break;
            case FOUND: s.energy-=.06; s.curiosity-=.06; s.uncertainty-=.05; break;
            case NO_EVIDENCE: s.energy-=.05; s.uncertainty+=.07; break;
            case VERIFIED: s.verified++; s.learning+=.04; s.uncertainty-=.06; break;
            case REJECTED: s.uncertainty+=.09; s.learning+=.06; break;
            default: break;
        }
        s.energy=bounded(s.energy); s.curiosity=bounded(s.curiosity);
        s.uncertainty=bounded(s.uncertainty); s.learning=bounded(s.learning);
        s.events=Math.min(Long.MAX_VALUE-1,s.events)+1; s.at=Math.max(0,now);
        return s;
    }
    public static boolean mayExplore(State s, Device d, boolean paused, boolean busy) {
        return !paused&&!busy&&!d.save&&!d.lowRam&&!d.hot&&d.battery>=20
                && s.energy>=.30&&s.curiosity>=.25;
    }
    public static boolean mayReflect(State s, Device d, boolean paused, boolean busy) {
        // Reflection, maintenance and goals must NOT depend on curiosity.
        return !paused&&!busy&&!d.save&&!d.lowRam&&!d.hot&&d.battery>=20
                &&d.charging&&s.energy>=.42;
    }
}