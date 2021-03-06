package at.tacticaldevc.oat.ui.FeaturePage.InstantPhoto;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;

import at.tacticaldevc.oat.R;
import at.tacticaldevc.oat.utils.Prefs;

public class InstantPhotoActivity extends AppCompatActivity {

    private CheckBox accept;
    private Switch activate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instant_photo);

        accept = findViewById(R.id.instant_photo_checkbox);
        accept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                acceptCondition();
            }
        });

        activate = findViewById(R.id.instant_photo_switch);
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
        if(Prefs.fetchConditionAccepted(this, getString(R.string.oat_features_key_trigger_instant_photo))){
            accept.setChecked(true);
        }else{
            accept.setChecked(false);
        }
    }

    public void featureActiveCheck(){
        if(Prefs.fetchFeatureEnabledStatus(this,getString(R.string.oat_features_key_trigger_instant_photo))) {
            activate.setChecked(true);
            activate.setClickable(true);
        }else{
            activate.setChecked(false);
        }
    }

    public void acceptCondition(){
        if(accept.isChecked()){
            Prefs.saveConditionAccepted(this, getString(R.string.oat_features_key_trigger_instant_photo), true);
            activate.setClickable(true);
        }else{
            Prefs.saveConditionAccepted(this, getString(R.string.oat_features_key_trigger_instant_photo), false);
            activate.setChecked(false);
            activate.setClickable(false);
        }
    }

    public void activateFeature(){
        if(activate.isChecked()){
            Prefs.saveFeatureEnabledStatus(this, getString(R.string.oat_features_key_trigger_instant_photo), true);
        }else{
            Prefs.saveFeatureEnabledStatus(this, getString(R.string.oat_features_key_trigger_instant_photo), false);
        }
    }
}
