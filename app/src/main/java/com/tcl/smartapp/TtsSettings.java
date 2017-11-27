package com.tcl.smartapp;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.ApkInstaller;

import com.tcl.smartapp.utils.SettingTextWatcher;

/**
 * Created  on 16-4-26.
 */
/**
 * 合成设置界面
 */
public class TtsSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener,SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "TtsSettings";
	public static final String PREFER_NAME = "TTSsetting";
    private ListPreference mTtsEngineTypePrefercen;
    private ListPreference mTtsVoicerPrefercen;
	private EditTextPreference mTtsSpeedPreference;
	private EditTextPreference mTtsPitchPreference;
	private EditTextPreference mTtsVolumePreference;
    private ListPreference mTtsStreamTypePreference;
    private SwitchPreference mTtsEnabledPreference;
    public static final String KEY_TTS_ENGINE_TYPE  = "tts_engine_type_preference";
    public static final String KEY_TTS_VOICER  = "tts_voicer_type_preference";
    public static final String KEY_TTS_SPEED  = "speed_preference";
    public static final String KEY_TTS_PITCH  = "pitch_preference";
    public static final String KEY_TTS_VOLUME  = "volume_preference";
    public static final String KEY_TTS_STREAM  = "stream_preference";
    public static final String KEY_TTS_ENABLE  = "tts_enable_preference";
    private ActionBar mActionBar;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 语记安装助手类
    ApkInstaller mInstaller ;
    private CharSequence[] mTtsEngineTypeEntries;
    private CharSequence[] mTtsVoicerTypeEntries;
    private CharSequence[] mTtsStreamTypeEntries;
    private SharedPreferences mSharedPreferences;
    @SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 指定保存文件名字
		getPreferenceManager().setSharedPreferencesName(PREFER_NAME);
		addPreferencesFromResource(R.xml.tts_setting);

        mTtsEnabledPreference = (SwitchPreference)findPreference(KEY_TTS_ENABLE);
        mTtsEnabledPreference.setOnPreferenceChangeListener(this);
        mTtsEnabledPreference.setOnPreferenceClickListener(this);

        mTtsEngineTypePrefercen = (ListPreference)findPreference(KEY_TTS_ENGINE_TYPE);
        mTtsEngineTypePrefercen.setOnPreferenceChangeListener(this);
        mTtsEngineTypePrefercen.setOnPreferenceClickListener(this);
        mTtsEngineTypeEntries = mTtsEngineTypePrefercen.getEntries();
        mTtsEngineTypePrefercen.setSummary(mTtsEngineTypeEntries[Integer.parseInt(mTtsEngineTypePrefercen.getValue())]);

        mTtsVoicerPrefercen = (ListPreference)findPreference(KEY_TTS_VOICER);
        mTtsVoicerPrefercen.setOnPreferenceChangeListener(this);
        mTtsVoicerPrefercen.setOnPreferenceClickListener(this);
        mTtsVoicerTypeEntries = mTtsVoicerPrefercen.getEntries();
        mTtsVoicerPrefercen.setSummary(mTtsVoicerTypeEntries[mTtsVoicerPrefercen.findIndexOfValue(mTtsVoicerPrefercen.getValue())]);

        mTtsStreamTypePreference = (ListPreference)findPreference(KEY_TTS_STREAM);
        mTtsStreamTypePreference.setOnPreferenceChangeListener(this);
        mTtsStreamTypePreference.setOnPreferenceClickListener(this);
        mTtsStreamTypeEntries = mTtsStreamTypePreference.getEntries();
        mTtsStreamTypePreference.setSummary(mTtsStreamTypeEntries[Integer.parseInt(mTtsStreamTypePreference.getValue())]);

        mTtsSpeedPreference = (EditTextPreference)findPreference(KEY_TTS_SPEED);
        mTtsSpeedPreference.getEditText().addTextChangedListener(new SettingTextWatcher(TtsSettings.this, mTtsSpeedPreference, 0, 200));
        mTtsSpeedPreference.setOnPreferenceChangeListener(this);
        mTtsSpeedPreference.setOnPreferenceClickListener(this);
        mTtsSpeedPreference.setSummary(mTtsSpeedPreference.getText());

        mTtsPitchPreference = (EditTextPreference)findPreference(KEY_TTS_PITCH);
        mTtsPitchPreference.getEditText().addTextChangedListener(new SettingTextWatcher(TtsSettings.this, mTtsPitchPreference, 0, 100));
        mTtsPitchPreference.setOnPreferenceClickListener(this);
        mTtsPitchPreference.setOnPreferenceChangeListener(this);
        mTtsPitchPreference.setSummary(mTtsPitchPreference.getText());

        mTtsVolumePreference = (EditTextPreference)findPreference(KEY_TTS_VOLUME);
        mTtsVolumePreference.getEditText().addTextChangedListener(new SettingTextWatcher(TtsSettings.this, mTtsVolumePreference, 0, 100));
        mTtsVolumePreference.setOnPreferenceClickListener(this);
        mTtsVolumePreference.setOnPreferenceChangeListener(this);
        mTtsVolumePreference.setSummary(mTtsVolumePreference.getText());

        mSharedPreferences = getPreferenceScreen().getSharedPreferences();

        mInstaller = new  ApkInstaller(TtsSettings.this);
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            mActionBar.setDisplayShowHomeEnabled(false);
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

    @Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTtsEngineTypePrefercen){
            String engineType = (String)newValue;
            Log.d(TAG, "engineType = " + engineType + ",isInstall =" + SpeechUtility.getUtility().checkServiceInstalled());
            if ("1".equals(engineType)){
                if(!SpeechUtility.getUtility().checkServiceInstalled()) {
                    Log.d(TAG, "need install package! ");
                    mInstaller.install();
                    return false;
                }else {
                    //提示用户进入讯飞语记应用进行参数设置
                    SpeechUtility.getUtility().openEngineSettings(null);
                    getPreferenceScreen().removePreference(mTtsVoicerPrefercen);
                }
            }else {
                getPreferenceScreen().addPreference(mTtsVoicerPrefercen);
            }
            mTtsEngineTypePrefercen.setSummary(mTtsEngineTypeEntries[Integer.parseInt((String) newValue)]);
        }else if (preference == mTtsVoicerPrefercen){
            mTtsVoicerPrefercen.setSummary(mTtsVoicerTypeEntries[mTtsVoicerPrefercen.findIndexOfValue((String)newValue)]);
        }else if (preference == mTtsStreamTypePreference){
            mTtsStreamTypePreference.setSummary(mTtsStreamTypeEntries[Integer.parseInt((String)newValue)]);
        }else if (preference == mTtsEnabledPreference){
            boolean isEnabled = (Boolean)newValue;
            Log.d(TAG, "onPreferenceChange: TTS enabled = " + isEnabled);
        }

        return true;
	}

    @Override
    public boolean onPreferenceClick(Preference preference) {
       if (preference == mTtsPitchPreference){
            mTtsPitchPreference.setSummary(mTtsPitchPreference.getText());
            mTtsPitchPreference.getEditText().setSelection(mTtsPitchPreference.getText().length());
        }else if (preference == mTtsSpeedPreference){
           mTtsSpeedPreference.setSummary(mTtsSpeedPreference.getText());
           mTtsSpeedPreference.getEditText().setSelection(mTtsSpeedPreference.getText().length());
       }else if (preference == mTtsVolumePreference){
           mTtsVolumePreference.setSummary(mTtsVolumePreference.getText());
           mTtsVolumePreference.getEditText().setSelection(mTtsVolumePreference.getText().length());
       }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        if ("1".equals(mTtsEngineTypePrefercen.getValue())){
            getPreferenceScreen().removePreference(mTtsVoicerPrefercen);
        }else{
            getPreferenceScreen().addPreference(mTtsVoicerPrefercen);
        }
        TokenUtils.startSelfLock(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_TTS_VOLUME.equals(key)) {
            mTtsVolumePreference.setSummary(mTtsVolumePreference.getText());
        }else if (KEY_TTS_PITCH.equals(key)){
            mTtsPitchPreference.setSummary(mTtsPitchPreference.getText());
        }else if (KEY_TTS_SPEED.equals(key)){
            mTtsSpeedPreference.setSummary(mTtsSpeedPreference.getText());
        }
    }
}