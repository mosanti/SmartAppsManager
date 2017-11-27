package com.tcl.smartapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import com.tcl.smartapp.R;
import com.tcl.smartapp.token.JrdChooseLockPIN;
import com.tcl.smartapp.token.SetupPwdActivity;
import com.tcl.smartapp.utils.Constants;

/**
 * Created by user on 5/5/16.
 */
public class PasswordSettingsActivity extends PreferenceActivity implements
        Preference.OnPreferenceClickListener {
    private PreferenceScreen mPatternType;
    private PreferenceScreen mPINType;
    private static final String PATTERN_LOCK_KEY = "pattern_lock_type";
    private static final String PIN_LOCK_KEY = "pin_lock_type";
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.password_preference);
        mPatternType = (PreferenceScreen) findPreference(PATTERN_LOCK_KEY);
        mPINType = (PreferenceScreen) findPreference(PIN_LOCK_KEY);

        mPatternType.setOnPreferenceClickListener(this);
        mPINType.setOnPreferenceClickListener(this);

        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        editor = sp.edit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        int style = sp.getInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
        if(style == Constants.PATTERN_TYPE) {
            mPatternType.setSummary(R.string.current_lock_type);
            mPINType.setSummary("");
        }
        else {
            mPatternType.setSummary("");
            mPINType.setSummary(R.string.current_lock_type);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.equals(mPatternType))
        {
            editor.putString("status", "1");
            editor.commit();
            Intent intent = new Intent(this, SetupPwdActivity.class);
            startActivity(intent);
            return true;
        }
        else if(preference.equals(mPINType))
        {
            Intent intent = new Intent(this, JrdChooseLockPIN.class);
            intent.putExtra("change_lock_type", true);
            startActivity(intent);
            return true;
        }
        return false;
    }
}
