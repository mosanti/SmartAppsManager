package com.tcl.smartapp;

import com.slidingmenu.lib.SlidingMenu;
import com.tcl.smartapp.db.SmartappDBOperator;
import com.tcl.smartapp.domain.AppInfo;
import com.tcl.smartapp.domain.DisabledAppInfo;
import com.tcl.smartapp.token.TokenUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DisabledAppsActivity extends Activity implements OnClickListener{
    private ActionBar mActionBar;
    private Context mContext;
    private static final String TAG = "DisabledAppsActivity";
    private Button mStartDisabledAppBtn;
    private LinearLayout mDisabledAppEmptyLayout;
    private LinearLayout mDisabledAppLayout;
    private int mDisabledAppNum = 0;
    private GridView mDisabledAppGridView;
    private PackageManager mPm;
    private List<DisabledAppInfo> mDisabledAppsList = new ArrayList<DisabledAppInfo>();
    private GridViewAdapter mGridViewAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_disabled_app);
        mStartDisabledAppBtn = (Button) findViewById(R.id.btn_start_disabled_app);
        mStartDisabledAppBtn.setOnClickListener(this);
        mDisabledAppEmptyLayout = (LinearLayout) findViewById(R.id.empty_disabled_app_Layout);
        mDisabledAppLayout = (LinearLayout) findViewById(R.id.disabled_app_Layout);
        mDisabledAppGridView = (GridView)findViewById(R.id.gridView);
        mContext = this;
        mPm = mContext.getPackageManager();
        mGridViewAdapter = new GridViewAdapter();

        mDisabledAppGridView.setAdapter(mGridViewAdapter);
        mDisabledAppGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemLongClick: position = " + position);
                final DisabledAppInfo appInfo = mDisabledAppsList.get(position);
                if(appInfo != null && appInfo.getPackageName().equals("add")){
                    Intent intent = new Intent(mContext,AppsListActivity.class);
                    startActivity(intent);
                    Log.d(TAG,"onClick: start AppsListActivity");
                    return true;
                }
                final PopupMenu popup =
                        new PopupMenu(mContext, view == null ? parent : view);
                popup.getMenuInflater().inflate(R.menu.popup_disabled_app_menu,popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getItemId() == R.id.enable_app_item){
                            Log.d(TAG, "onItemLongClick: enabel app = " + appInfo.getPackageName());
                            try {
                                mPm.setApplicationEnabledSetting(appInfo.getPackageName(), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
                            }catch (Exception e){
                                e.printStackTrace();
                                Toast.makeText(mContext,R.string.disable_app_permission_tips,Toast.LENGTH_LONG).show();
                            }

                            getDisabledAppsList();
                        }else if(item.getItemId() == R.id.app_inspect_item){
                            Log.d(TAG, "onItemLongClick: get app info");
                            startApplicationDetailsActivity(appInfo.getPackageName());
                        }else {
                            return false;
                        }
                        return true;
                    }
                });

                setIconEnabled(popup.getMenu());
                popup.show();
                return false;
            }
        });
        mDisabledAppGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DisabledAppInfo appInfo = mDisabledAppsList.get(position);
                if(appInfo != null && appInfo.getPackageName().equals("add")){
                    Intent intent = new Intent(mContext,AppsListActivity.class);
                    startActivity(intent);
                    Log.d(TAG,"onClick: start AppsListActivity");
                }
            }
        });
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            mActionBar.setDisplayShowHomeEnabled(false);
        }
	}

    private void getDisabledAppsList(){
        mDisabledAppsList.clear();
        List<ApplicationInfo> appsLists = getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        PackageManager manager = this.getPackageManager();

        for (ApplicationInfo applicationInfo : appsLists) {
            if (!applicationInfo.enabled) {
                mDisabledAppsList.add(new DisabledAppInfo(applicationInfo.packageName,
                        (String) applicationInfo.loadLabel(manager),
                        applicationInfo.loadIcon(manager),
                        true));
            }

        }
        Drawable icon = getResources().getDrawable(R.drawable.add_disabled_app_icon);
        mDisabledAppsList.add(new DisabledAppInfo("add",getString(R.string.add_disabled_app),icon,false));
        mGridViewAdapter.notifyDataSetChanged();
    }
    /**
     * enable pop up menu icon
     * @param menu
     */
    private void setIconEnabled(Menu menu) {
        try {
            Class clazz=Class.forName("com.android.internal.view.menu.MenuBuilder");
            Method m = clazz.getDeclaredMethod("setOptionalIconsVisible",boolean.class);
            m.setAccessible(true);
            m.invoke(menu,true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void startApplicationDetailsActivity(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setComponent(intent.resolveActivity(mContext.getPackageManager()));
        startActivity(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();
        getDisabledAppsList();
        mDisabledAppNum = mDisabledAppsList.size();
        Log.d(TAG, "onResume: mDisabledAppNum = "+mDisabledAppNum);
        if (mDisabledAppNum <= 1){
            mDisabledAppLayout.setVisibility(View.GONE);
            mDisabledAppEmptyLayout.setVisibility(View.VISIBLE);
        }else{
            mDisabledAppLayout.setVisibility(View.VISIBLE);
            mDisabledAppEmptyLayout.setVisibility(View.GONE);
        }
        TokenUtils.startSelfLock(this);
    }

    @Override
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.btn_start_disabled_app:
                Intent intent = new Intent(this,AppsListActivity.class);
                startActivity(intent);
                Log.d(TAG,"onClick: start AppsListActivity");
                break;
		default:
			break;
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

    private class GridViewAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mDisabledAppsList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView != null && convertView instanceof LinearLayout) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = View.inflate(mContext, R.layout.grid_item, null);
                holder = new ViewHolder();
                holder.app_icon = (ImageView) view.findViewById(R.id.item_image);
                holder.app_name = (TextView) view.findViewById(R.id.item_text);
                view.setTag(holder);
                Log.e(TAG, "GridViewAdapter-getView app_icon=" + holder.app_icon);
            }
            if (position < mDisabledAppsList.size()) {
                DisabledAppInfo info = mDisabledAppsList.get(position);

                holder.app_icon.setImageDrawable(info.getAppIcon());
               holder.app_name.setText(info.getAppName());
            } else {
                Log.e(TAG, "GridViewAdapter-getView Error position:" + position);
            }

            return view;
        }
    }

    private static class ViewHolder {
        ImageView app_icon;
        TextView app_name;
    }

}
