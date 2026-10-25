package salve.core.synergy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import com.google.gson.Gson;

/** Persistent controller; no background service or privileged permissions. */
public final class SynergyRuntime {
    private static final String PREFS = "salve_synergy_v1";
    private static volatile SynergyRuntime instance;
    private final Context app;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private SynergyRuntime(Context app) {
        this.app=app.getApplicationContext();
        this.prefs=this.app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    public static SynergyRuntime get(Context app) {
        if (instance==null) synchronized(SynergyRuntime.class) {
            if(instance==null) instance=new SynergyRuntime(app);
        }
        return instance;
    }
    public synchronized SynergyCore.State state() {
        try {
            String data=prefs.getString("state","");
            SynergyCore.State loaded=data.isEmpty()?null:gson.fromJson(data,SynergyCore.State.class);
            return SynergyCore.clean(loaded);
        } catch (RuntimeException broken) { return new SynergyCore.State(); }
    }
    public synchronized SynergyCore.State record(SynergyCore.Event event) {
        SynergyCore.State next=SynergyCore.advance(state(),event,device().charging,System.currentTimeMillis());
        prefs.edit().putString("state",gson.toJson(next)).apply();
        return next;
    }
    public boolean allowCuriosity(boolean paused,boolean userBusy) {
        return SynergyCore.mayExplore(state(),device(),paused,userBusy);
    }
    public boolean allowReflection(boolean paused,boolean userBusy) {
        return SynergyCore.mayReflect(state(),device(),paused,userBusy);
    }
    public String summary() {
        SynergyCore.State s=state();
        return "REGULACION INTERNA (no emoción): energía="+band(s.energy)+
                "; incertidumbre="+band(s.uncertainty)+
                "; curiosidad="+band(s.curiosity)+
                "; aprendizaje="+band(s.learning)+".";
    }
    /** Observable values, with no claim of feelings or physical life. */
    public String status() {
        SynergyCore.State s=state(); SynergyCore.Device d=device();
        return "Mi sinergia computacional: energía de trabajo "+(int)(s.energy*100)+
            "/100, curiosidad "+(int)(s.curiosity*100)+
            "/100, incertidumbre "+(int)(s.uncertainty*100)+
            "/100 y prioridad de aprendizaje "+(int)(s.learning*100)+
            "/100. Batería del teléfono "+(d.battery>0?d.battery+"%":"no disponible")+
            ". Investigación autónoma "+(SynergyCore.mayExplore(s,d,false,false)?"permitida":"limitada por estado o recursos")+
            ". Son indicadores de ejecución, no emociones.";
    }
    private static String band(double score) {
        return score<.33?"baja":score>.67?"alta":"media";
    }
    public SynergyCore.Device device() {
        int level=0; boolean charging=false,save=true,lowRam=true,hot=true;
        try {
            Intent power=app.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(power!=null) {
                int value=power.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                int scale=power.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                if(value>=0&&scale>0)level=100*value/scale;
                int status=power.getIntExtra(BatteryManager.EXTRA_STATUS,0);
                charging=status==BatteryManager.BATTERY_STATUS_CHARGING
                        ||status==BatteryManager.BATTERY_STATUS_FULL;
            }
            PowerManager pm=(PowerManager)app.getSystemService(Context.POWER_SERVICE);
            if(pm!=null) {
                save=pm.isPowerSaveMode();
                hot=Build.VERSION.SDK_INT>=29&&pm.getCurrentThermalStatus()
                        >=PowerManager.THERMAL_STATUS_SEVERE;
            }
            ActivityManager am=(ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
            if(am!=null) {
                ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();
                am.getMemoryInfo(info); lowRam=info.lowMemory;
            }
        } catch (RuntimeException denied) { return new SynergyCore.Device(0,false,true,true,true); }
        return new SynergyCore.Device(level,charging,save,lowRam,hot);
    }
}
