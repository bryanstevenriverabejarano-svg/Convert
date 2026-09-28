package salve.presentation.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import salve.avatar.*;

/** Inspect all supplied references and exercise the same frontal rig used in conversation. */
public final class AvatarCoreActivity extends Activity {
    private CoreModelView preview;
    private Spinner selector;
    private final SeekBar[] sliders=new SeekBar[4];
    private CheckBox joints;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setTitle("Núcleo de Salve");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        preview=new CoreModelView(this);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll=new ScrollView(this);LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controls.setPadding(16,8,16,8);scroll.addView(controls);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView note=new TextView(this);note.setText("Explora las 20 vistas del nuevo núcleo. En la frontal puedes probar movimientos suaves. Las otras vistas son ilustraciones de referencia y poses, todavía sin movimiento 3D entre ellas.");controls.addView(note);
        selector=new Spinner(this);String[] labels=new String[CoreViewCatalog.VIEWS.size()];for(int i=0;i<labels.length;i++)labels[i]=CoreViewCatalog.VIEWS.get(i).label;
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);selector.setAdapter(adapter);selector.setContentDescription("Vista del núcleo");controls.addView(selector);
        joints=new CheckBox(this);joints.setText("Mostrar articulaciones frontales");joints.setOnCheckedChangeListener((b,v)->preview.showJoints(v));controls.addView(joints);
        String[] names={"Inclinación de cabeza","Brazos","Paso","Parpadeo"};
        for(int i=0;i<sliders.length;i++){
            TextView label=new TextView(this);label.setText(names[i]);controls.addView(label);
            SeekBar slider=new SeekBar(this);sliders[i]=slider;slider.setContentDescription(names[i]);slider.setMax(100);slider.setProgress(i==3?0:50);controls.addView(slider);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){updateMotion();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        }
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                CoreViewCatalog.Entry entry=CoreViewCatalog.VIEWS.get(position);preview.select(entry.id);
                boolean frontal="front".equals(entry.id);for(SeekBar slider:sliders)slider.setEnabled(frontal);joints.setEnabled(frontal);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        Button reset=new Button(this);reset.setText("Restablecer movimiento");reset.setOnClickListener(v->{for(int i=0;i<4;i++)sliders[i].setProgress(i==3?0:50);});controls.addView(reset);
        Button wear=new Button(this);wear.setText("Usar núcleo como apariencia");wear.setOnClickListener(v->AvatarWardrobeStore.get(this).wearTemplate("core",answer->Toast.makeText(this,answer,Toast.LENGTH_SHORT).show()));controls.addView(wear);
        if(state!=null){selector.setSelection(Math.max(0,Math.min(labels.length-1,state.getInt("view",0))));for(int i=0;i<4;i++)sliders[i].setProgress(state.getInt("slider"+i,i==3?0:50));joints.setChecked(state.getBoolean("joints"));}
        setContentView(root);updateMotion();
    }
    private void updateMotion(){for(SeekBar s:sliders)if(s==null)return;preview.setMotion((sliders[0].getProgress()-50)*.14f,(sliders[1].getProgress()-50)*.5f,(sliders[2].getProgress()-50)/50f,sliders[3].getProgress()/100f);}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("view",selector.getSelectedItemPosition());for(int i=0;i<4;i++)state.putInt("slider"+i,sliders[i].getProgress());state.putBoolean("joints",joints.isChecked());super.onSaveInstanceState(state);}
}
