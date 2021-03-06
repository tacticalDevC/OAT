package at.tacticaldevc.oat.ui.FeaturePage.PhotoTrap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

import at.tacticaldevc.oat.R;
import at.tacticaldevc.oat.utils.Prefs;

public class PhotoTrapActivity extends AppCompatActivity {

    private CheckBox accept;
    private Switch activate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_trap);

        accept = findViewById(R.id.photo_trap_checkbox);
        accept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                acceptCondition();
            }
        });

        activate = findViewById(R.id.photo_trap_switch);
        activate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                activateFeature();
            }
        });
        activate.setClickable(false);

        featureActiveCheck();
        featureConditionCheck();
    }

    public void featureConditionCheck(){
        if(Prefs.fetchConditionAccepted(this, getString(R.string.oat_features_key_trigger_photo_trap))){
            accept.setChecked(true);
        }else{
            accept.setChecked(false);
        }
    }

    public void featureActiveCheck(){
        if(Prefs.fetchFeatureEnabledStatus(this,getString(R.string.oat_features_key_trigger_photo_trap))) {
            activate.setChecked(true);
            activate.setClickable(true);
        }else{
            activate.setChecked(false);
        }
    }

    public void acceptCondition(){
        if(accept.isChecked()){
            Prefs.saveConditionAccepted(this, getString(R.string.oat_features_key_trigger_photo_trap), true);
            activate.setClickable(true);
        }else{
            Prefs.saveConditionAccepted(this, getString(R.string.oat_features_key_trigger_photo_trap), false);
            activate.setChecked(false);
            activate.setClickable(false);
        }
    }

    public void activateFeature(){
        if(activate.isChecked()){
            Prefs.saveFeatureEnabledStatus(this, getString(R.string.oat_features_key_trigger_photo_trap), true);
        }else{
            Prefs.saveFeatureEnabledStatus(this, getString(R.string.oat_features_key_trigger_photo_trap), false);
        }
    }
}
