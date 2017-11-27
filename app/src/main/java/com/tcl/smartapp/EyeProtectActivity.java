package com.tcl.smartapp;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.view.SeekBarPreference;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created  on 4/25/16.
 */
public class EyeProtectActivity extends FragmentActivity {
    protected EyeProtectService.EyeProtectBinder mBinder;
    protected AtomicBoolean mBound = new AtomicBoolean(false);
    private ActionBar mActionBar;
    private final ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            if (mBound.compareAndSet(false, true))
                mBinder = (EyeProtectService.EyeProtectBinder) service;
        }

        public void onServiceDisconnected(ComponentName className) {
            if (mBound.compareAndSet(true, false))
                mBinder = null;
        }
    };

    public void bindEyeProtectService() {
        if (!mBound.get()) {
            bindService(new Intent(this, EyeProtectService.class), this.mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindEyeProtectService() {
        if (mBound.get()) {
            unbindService(mConnection);
            mBound.compareAndSet(true, false);
        }
    }

    public EyeProtectService.EyeProtectBinder getEyeProtectBinder() {
        return mBinder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            mActionBar.setDisplayShowHomeEnabled(false);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(EyeProtect.PREF_EYE_PROTECT, false)) {
            if (EyeProtect.checkSystemAlertPermission(this)) {
                EyeProtect.startEyeProtect(this);
                bindEyeProtectService();
            } else {
                sharedPreferences.edit().putBoolean(EyeProtect.PREF_EYE_PROTECT, false).apply();
            }
        } else {
            EyeProtect.stopEyeProtect(this);
            unbindEyeProtectService();
        }
        TokenUtils.startSelfLock(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindEyeProtectService();
    }

    public static class PrefsFragment extends PreferenceFragment implements
            Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.eye_protect_settings);
            init();
        }

        @Override
        public void onResume() {
            super.onResume();
            SwitchPreference eyeProtect = (SwitchPreference) findPreference(EyeProtect.PREF_EYE_PROTECT);
            eyeProtect.setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getBoolean(EyeProtect.PREF_EYE_PROTECT, false));
        }

        private void init() {
            SwitchPreference eyeProtect = (SwitchPreference) findPreference(EyeProtect.PREF_EYE_PROTECT);
            eyeProtect.setOnPreferenceChangeListener(this);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int transparency = sharedPreferences.getInt(EyeProtect.PREF_FILTER_TRANSPARENCY, EyeProtect.DEFAULT_FILTER_TRANSPARENCY);
            int red = sharedPreferences.getInt(EyeProtect.PREF_FILTER_RED, EyeProtect.DEFAULT_FILTER_RED);
            int green = sharedPreferences.getInt(EyeProtect.PREF_FILTER_GREEN, EyeProtect.DEFAULT_FILTER_GREEN);
            int blue = sharedPreferences.getInt(EyeProtect.PREF_FILTER_BLUE, EyeProtect.DEFAULT_FILTER_BLUE);

            SeekBarPreference filterTransparency = (SeekBarPreference) findPreference(EyeProtect.PREF_FILTER_TRANSPARENCY);
            filterTransparency.setProgress(transparency);
            filterTransparency.setOnPreferenceChangeListener(this);

            SeekBarPreference filterRed = (SeekBarPreference) findPreference(EyeProtect.PREF_FILTER_RED);
            filterRed.setProgress(red);
            filterRed.setOnPreferenceChangeListener(this);

            SeekBarPreference filterGreen = (SeekBarPreference) findPreference(EyeProtect.PREF_FILTER_GREEN);
            filterGreen.setProgress(green);
            filterGreen.setOnPreferenceChangeListener(this);

            SeekBarPreference filterBlue = (SeekBarPreference) findPreference(EyeProtect.PREF_FILTER_BLUE);
            filterBlue.setProgress(blue);
            filterBlue.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Context context = getActivity();
            switch (preference.getKey()) {
                case EyeProtect.PREF_EYE_PROTECT:
                    if ((boolean) newValue) {
                        EyeProtect.startEyeProtect(context);
                        ((EyeProtectActivity) context).bindEyeProtectService();
                    } else {
                        EyeProtect.stopEyeProtect(context);
                        ((EyeProtectActivity) context).unbindEyeProtectService();
                    }
                    break;
                case EyeProtect.PREF_FILTER_TRANSPARENCY:
                case EyeProtect.PREF_FILTER_RED:
                case EyeProtect.PREF_FILTER_GREEN:
                case EyeProtect.PREF_FILTER_BLUE:
                    EyeProtectService.EyeProtectBinder binder = getEyeProtectBinder();
                    if (binder != null) {
                        int color = binder.getService().getFilterColor();
                        switch (preference.getKey()) {
                            case EyeProtect.PREF_FILTER_TRANSPARENCY:
                                int alpha = EyeProtect.transparencyToAlpha((int) newValue);
                                color = color & 0x00FFFFFF | (alpha << 24);
                                break;
                            case EyeProtect.PREF_FILTER_RED:
                                color = color & 0xFF00FFFF | ((int) newValue << 16);
                                break;
                            case EyeProtect.PREF_FILTER_GREEN:
                                color = color & 0xFFFF00FF | ((int) newValue << 8);
                                break;
                            case EyeProtect.PREF_FILTER_BLUE:
                                color = color & 0xFFFFFF00 | ((int) newValue);
                                break;
                        }
                        binder.getService().setFilterColor(color);
                    }
                    break;
            }
            return true;
        }

        private EyeProtectService.EyeProtectBinder getEyeProtectBinder() {
            return ((EyeProtectActivity) getActivity()).getEyeProtectBinder();
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            return true;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
