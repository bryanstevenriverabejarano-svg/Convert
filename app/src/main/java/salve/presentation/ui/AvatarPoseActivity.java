package salve.presentation.ui;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.widget.*;
import java.util.EnumMap;
import salve.avatar.ArticulatedPose;
import salve.avatar.ArticulatedPoseView;

/** Explicit posture workshop; saved experiments do not change the conversation avatar. */
public final class AvatarPoseActivity extends Activity {
    private ArticulatedPose pose=new ArticulatedPose();
    private ArticulatedPoseView preview;
    private ValueAnimator animator;
    private final EnumMap<ArticulatedPose.Joint,SeekBar> sliders=new EnumMap<>(ArticulatedPose.Joint.class);
    private final EnumMap<ArticulatedPose.Joint,TextView> labels=new EnumMap<>(ArticulatedPose.Joint.class);
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setTitle("Taller de posturas");
        if(state!=null)try{pose=ArticulatedPose.fromJson(state.getString("pose"));}catch(IllegalArgumentException ignored){toast("No se pudo restaurar la postura.");}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        preview=new ArticulatedPoseView(this);preview.setPose(pose);
        root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controls.setPadding(16,8,16,8);scroll.addView(controls);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView note=new TextView(this);note.setText("Posturas frontales experimentales. Las capas aún necesitan ajustes en uniones y pliegues. Izquierda y derecha corresponden a la pantalla.");controls.addView(note);
        CheckBox joints=new CheckBox(this);joints.setText("Mostrar articulaciones");joints.setOnCheckedChangeListener((b,v)->preview.showJoints(v));controls.addView(joints);
        String[] names={"Reposo","Brazos arriba","Saludo","Doblar codos","Flexionar piernas"};
        int index=0;for(ArticulatedPose.Preset preset:ArticulatedPose.Preset.values()) {Button button=new Button(this);button.setText(names[index++]);button.setOnClickListener(v->animateTo(ArticulatedPose.preset(preset)));controls.addView(button);}
        for(ArticulatedPose.Joint joint:ArticulatedPose.Joint.values()) {
            TextView label=new TextView(this);labels.put(joint,label);controls.addView(label);
            SeekBar slider=new SeekBar(this);slider.setMax(joint.max-joint.min);sliders.put(joint,slider);controls.addView(slider);
            slider.setContentDescription(joint.label);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){cancelAnimation();pose=pose.with(joint,value+joint.min);refresh();}}
                public void onStartTrackingTouch(SeekBar bar){cancelAnimation();}
                public void onStopTrackingTouch(SeekBar bar){}
            });
        }
        Button save=new Button(this);save.setText("Guardar postura");save.setOnClickListener(v->{getSharedPreferences("pose_workshop",MODE_PRIVATE).edit().putString("pose",pose.toJson()).apply();toast("Postura guardada en el taller.");});controls.addView(save);
        Button load=new Button(this);load.setText("Recuperar postura");load.setOnClickListener(v->{String json=getSharedPreferences("pose_workshop",MODE_PRIVATE).getString("pose",null);if(json==null){toast("Todavía no hay una postura guardada.");return;}try{animateTo(ArticulatedPose.fromJson(json));}catch(IllegalArgumentException error){toast("La postura guardada no es válida.");}});controls.addView(load);
        setContentView(root);refresh();
    }
    private void animateTo(ArticulatedPose target){cancelAnimation();ArticulatedPose start=pose;animator=ValueAnimator.ofFloat(0,1);animator.setDuration(550);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(a->{pose=ArticulatedPose.interpolate(start,target,(float)a.getAnimatedValue());refresh();});animator.start();}
    private void refresh(){preview.setPose(pose);for(ArticulatedPose.Joint joint:sliders.keySet()){int value=Math.round(pose.angle(joint));sliders.get(joint).setProgress(value-joint.min);labels.get(joint).setText(joint.label+": "+value+"°");}}
    private void cancelAnimation(){if(animator!=null){animator.cancel();animator=null;}}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("pose",pose.toJson());super.onSaveInstanceState(state);}
    @Override protected void onStop(){cancelAnimation();super.onStop();}
}
