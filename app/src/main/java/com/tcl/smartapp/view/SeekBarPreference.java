package com.tcl.smartapp.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;


public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private Context mContext;

    private int mMax, mValue = 0;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mMax = attrs.getAttributeIntValue(androidns, "max", 100);

    }

    private List<View> getAllChildView(View view) {
        List<View> allChildView = new ArrayList<View>();
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View viewChild = vg.getChildAt(i);
                allChildView.add(viewChild);
                allChildView.addAll(getAllChildView(viewChild));
            }
        }
        return allChildView;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected View onCreateView(ViewGroup parent) {
        View parentView = super.onCreateView(parent);
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        /*LayoutParams paramsParent = parentView.getLayoutParams();
        paramsParent.height = 95;*/

        layout.addView(parentView);

        LinearLayout layoutSeekBar = new LinearLayout(mContext);
        layoutSeekBar.setOrientation(LinearLayout.HORIZONTAL);
        View leftBlankView = new View(mContext);
        View rightBlankView = new View(mContext);

        LinearLayout.LayoutParams paramsBlank = new LinearLayout.
                LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        paramsBlank.weight = 5;

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        LinearLayout.LayoutParams paramsSeekBar = new LinearLayout.
                LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsSeekBar.weight = 1;

        layoutSeekBar.addView(leftBlankView, paramsBlank);
        layoutSeekBar.addView(mSeekBar, paramsSeekBar);
        layoutSeekBar.addView(rightBlankView, paramsBlank);
        layout.addView(layoutSeekBar);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
        return layout;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onBindView(View v) {
        super.onBindView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        mValue = restore ? getPersistedInt(mValue) : (Integer) defaultValue;
    }

    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        if (fromTouch) {
            mValue = value;
            if (shouldPersist())
                persistInt(value);
            callChangeListener(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getMax() {
        return mMax;
    }

    @SuppressWarnings("deprecation")
    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }

    public int getProgress() {
        return mValue;
    }
}

