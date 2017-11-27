package com.tcl.smartapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import android.view.View;

import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;

import android.widget.RelativeLayout;
import android.widget.TabHost;


import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.db.SmartappDBOperator;

import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AppUtil;
import com.tcl.smartapp.view.SwitchButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This activity  for selecting Apps Activity.
 */
public class AppsListActivity extends Activity {
    // Debugging
    private static final String TAG = "AppsListActivity";

    // Tab tag enum
    private static final String TAB_TAG_PERSONAL_APP = "personal_app";
    private static final String TAB_TAG_SYSTEM_APP = "system_app";
    
    // View item filed
    public static final String VIEW_ITEM_ICON = "package_icon";
    public static final String VIEW_ITEM_TEXT = "package_text";
    public static final String VIEW_ITEM_CHECKBOX = "package_checkbox";
    public static final String VIEW_ITEM_PACKAGE_NAME = "package_name";    // Only for save to selected apps list
    
    // These two array should be consistent 
    private static final String[] VIEW_TEXT_ARRAY = new String[] { VIEW_ITEM_ICON, VIEW_ITEM_TEXT, VIEW_ITEM_CHECKBOX };
    private static final int[] VIEW_RES_ID_ARRAY = new int[] { R.id.package_icon, R.id.package_text, R.id.package_checkbox };
    
    // For save tab widget
    private TabHost mTabHost = null;
    
    // For personal app list
    private List<Map<String, Object>> mPersonalAppList = null;
    private AppListAdapter mPersonalAppAdapter = null;
    
    // For system app list
    private List<Map<String, Object>> mSystemAppList = null;
    private AppListAdapter mSystemAppAdapter = null;

    private Context mContext;
    private PackageManager mPm;
    public AppsListActivity() {
        Log.i(TAG, "AppsListActivity(), AppsListActivity constructed!");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate(), Create SelectAppInfoActivity ui!");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps_list_disabled_apps);                

        initTabHost();
        mContext = this;
        mPm = mContext.getPackageManager();
        // Load package in background
        LoadPackageTask loadPackageTask = new LoadPackageTask(this);
        loadPackageTask.execute("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        TokenUtils.startSelfLock(this);
    }

    private void initTabHost() {
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();      
        mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_PERSONAL_APP).setContent(
                R.id.LinearLayout001).setIndicator(getString(R.string.personal_apps_title)));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_SYSTEM_APP).setContent(
                R.id.LinearLayout002).setIndicator(getString(R.string.system_apps_title)));
    }


    private void initUiComponents() {
        Log.i(TAG, "initUiComponents()");

        // Initialize personal app list view
        ListView mPersonalAppListView = (ListView) findViewById(R.id.list_personal_app);
        mPersonalAppAdapter = new AppListAdapter();
        mPersonalAppListView.setAdapter(mPersonalAppAdapter);
        mPersonalAppListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick position:" + position);
                if (position < mPersonalAppList.size()) {
                    Map<String, Object> item = mPersonalAppList.get(position);
                    String packageName = (String)item.get(VIEW_ITEM_PACKAGE_NAME);
                    boolean isDisabled = (Boolean) item.get(VIEW_ITEM_CHECKBOX);
                    if (!isDisabled) {
                        try {
                            mPm.setApplicationEnabledSetting(packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0);
                            item.remove(VIEW_ITEM_CHECKBOX);
                            item.put(VIEW_ITEM_CHECKBOX, !isDisabled);
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(mContext, R.string.disable_app_permission_tips, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        try {
                            mPm.setApplicationEnabledSetting(packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
                            item.remove(VIEW_ITEM_CHECKBOX);
                            item.put(VIEW_ITEM_CHECKBOX, !isDisabled);
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(mContext, R.string.disable_app_permission_tips, Toast.LENGTH_LONG).show();
                        }
                    }

                    // update list data
                    if (isPersonalAppTabSelected()) {
                        mPersonalAppAdapter.notifyDataSetChanged();
                    } else {
                        mSystemAppAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
        
        // Initialize system app list view
        ListView mSystemAppListView = (ListView) findViewById(R.id.list_system_app);
        mSystemAppAdapter = new AppListAdapter();
        mSystemAppListView.setAdapter(mSystemAppAdapter);
        mSystemAppListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < mSystemAppList.size()) {
                    Map<String, Object> item = mSystemAppList.get(position);
                    String packageName = (String)item.get(VIEW_ITEM_PACKAGE_NAME);

                    boolean isDisabled = (Boolean) item.get(VIEW_ITEM_CHECKBOX);
                    if (!isDisabled) {
                        try {
                            mPm.setApplicationEnabledSetting(packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0);
                            item.remove(VIEW_ITEM_CHECKBOX);
                            item.put(VIEW_ITEM_CHECKBOX, !isDisabled);
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(mContext, R.string.disable_app_permission_tips, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        try {
                            mPm.setApplicationEnabledSetting(packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
                            item.remove(VIEW_ITEM_CHECKBOX);
                            item.put(VIEW_ITEM_CHECKBOX, !isDisabled);
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(mContext, R.string.disable_app_permission_tips, Toast.LENGTH_LONG).show();
                        }
                    }


                    // update list data
                    if (isPersonalAppTabSelected()) {
                        mPersonalAppAdapter.notifyDataSetChanged();
                    } else {
                        mSystemAppAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

    }

    private class AppListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return isPersonalAppTabSelected() ? mPersonalAppList.size() : mSystemAppList.size();
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
            if (convertView != null && convertView instanceof RelativeLayout) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = View.inflate(mContext, R.layout.package_list_layout, null);
                holder = new ViewHolder();
                holder.app_icon = (ImageView) view.findViewById(R.id.package_icon);
                holder.app_name = (TextView) view.findViewById(R.id.package_text);
                holder.app_summary = (TextView) view.findViewById(R.id.disabled_app_summary);
                holder.disabled = (SwitchButton)view.findViewById(R.id.package_checkbox);
                view.setTag(holder);
            }
            if (isPersonalAppTabSelected()) {
                if (position < mPersonalAppList.size()) {
                    Map<String, Object> item = mPersonalAppList.get(position);
                    holder.app_icon.setImageDrawable((Drawable)item.get(VIEW_ITEM_ICON));
                    holder.app_name.setText((String)item.get(VIEW_ITEM_TEXT));
                    boolean isDisabled = (Boolean)item.get(VIEW_ITEM_CHECKBOX);
                    if (isDisabled) {
                        holder.app_summary.setText(R.string.app_disable_flag_text);
                        holder.disabled.setChecked(true);
                    }else{
                        holder.app_summary.setText(null);
                        holder.disabled.setChecked(false);
                    }
                } else {
                    Log.e(TAG, "MyAdapter-getView Error position:" + position);
                }
            }else {
                if (position < mSystemAppList.size()) {
                    Map<String, Object> item = mSystemAppList.get(position);
                    holder.app_icon.setImageDrawable((Drawable)item.get(VIEW_ITEM_ICON));
                    holder.app_name.setText((String)item.get(VIEW_ITEM_TEXT));
                    boolean isDisabled = (Boolean)item.get(VIEW_ITEM_CHECKBOX);
                    if (isDisabled) {
                        holder.app_summary.setText(R.string.app_disable_flag_text);
                        holder.disabled.setChecked(true);
                    }else{
                        holder.app_summary.setText(null);
                        holder.disabled.setChecked(false);
                    }
                } else {
                    Log.e(TAG, "MyAdapter-getView Error position:" + position);
                }
            }
            return view;
        }
    }

    private static class ViewHolder {
        ImageView app_icon;
        TextView app_name;
        TextView app_summary;
        SwitchButton disabled;
    }

    
    private boolean isPersonalAppTabSelected() {        
        return (mTabHost.getCurrentTabTag() == TAB_TAG_PERSONAL_APP);
    }
    
   
    /**
     * This class is used for sorting package list.
     */
    private class PackageItemComparator implements Comparator<Map<String, Object>> {

        private final String mKey;

        public PackageItemComparator() {
            mKey = AppsListActivity.VIEW_ITEM_TEXT;
        }


        /**
         * Compare package in alphabetical order.
         * @see Comparator#compare(Object, Object)
         */
        @Override
        public int compare(Map<String, Object> packageItem1, Map<String, Object> packageItem2) {

            String packageName1 = (String) packageItem1.get(mKey);
            String packageName2 = (String) packageItem2.get(mKey);
            return packageName1.compareToIgnoreCase(packageName2);
        }
    } 
    
    private class LoadPackageTask extends AsyncTask<String, Integer, Boolean> {

		private ProgressDialog mProgressDialog;
        private final Context mContext;

        
        public LoadPackageTask(Context context) {
            Log.i(TAG, "LoadPackageTask(), Create LoadPackageTask!");
            
            mContext = context;    
  
            createProgressDialog();
        }

        /*
         * Show a ProgressDialog to prompt user to wait
         */
        private void createProgressDialog() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle(R.string.progress_dialog_title);
            mProgressDialog.setMessage(mContext.getString(R.string.progress_dialog_message));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
            Log.i(TAG, "createProgressDialog(), ProgressDialog shows");
        }
                
        @Override
        protected Boolean doInBackground(String... arg0) {
            Log.i(TAG, "doInBackground(), Begin load and sort package list!");
            
            // Load and sort package list
            loadPackageList();
            sortPackageList();
           if(mProgressDialog != null){
              mProgressDialog.dismiss();
           }
            return true;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(TAG, "onPostExecute(), Load and sort package list complete!");
            
            // Do the operation after load and sort package list completed
            initUiComponents();

        }
        
        private void loadPackageList() {
            mPersonalAppList = new ArrayList<Map<String, Object>>();
            mSystemAppList = new ArrayList<Map<String, Object>>();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoLists = getPackageManager()
                    .queryIntentActivities(mainIntent, 0);

            for(ResolveInfo resolveInfo : resolveInfoLists){
                String pkgName = resolveInfo.activityInfo.packageName;
               if (pkgName.equals("com.tcl.smartapp")){
                   continue;
               }
                ApplicationInfo appInfo;
                try{
                    appInfo = getPackageManager().getApplicationInfo(pkgName, 0);
                }catch (NameNotFoundException e){
                    Log.e(TAG, "can not found appInfo about package:" + pkgName);
                    continue;
                }
            if(appInfo == null){
                    Log.e(TAG, "appInfo is null about package:" + pkgName);
                    continue;
                }
                PackageInfo packageInfo = null;
				try {
					packageInfo = getPackageManager().getPackageInfo(pkgName, 0);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
                if (packageInfo != null) {
                    //Add this package to package list
                    Map<String, Object> packageItem = new HashMap<String, Object>();
                    // Add app icon
                    Drawable icon = mContext.getPackageManager()
                            .getApplicationIcon(packageInfo.applicationInfo);
                    packageItem.put(VIEW_ITEM_ICON, icon);

                    // Add app name
                    String appName = mContext.getPackageManager()
                            .getApplicationLabel(packageInfo.applicationInfo).toString();
                    packageItem.put(VIEW_ITEM_TEXT, appName);
                    packageItem.put(VIEW_ITEM_PACKAGE_NAME, packageInfo.packageName);

                    // Add if app is disabled.
                    boolean isDisabled = !appInfo.enabled;
                    packageItem.put(VIEW_ITEM_CHECKBOX, isDisabled);

                    // Add to package list
                    if (AppUtil.isSystemApp(packageInfo.applicationInfo)) {
                        mSystemAppList.add(packageItem);
                    } else {
                        mPersonalAppList.add(packageItem);
                    }
                }
            }
            getAllDisabledAppsFromAppList();
            Log.i(TAG, "loadPackageList(), PersonalAppList=" + mPersonalAppList);
            Log.i(TAG, "loadPackageList(), SystemAppList=" + mSystemAppList);
        }        

        private void getAllDisabledAppsFromAppList(){
            List<ApplicationInfo> appsLists = getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);

            for (ApplicationInfo applicationInfo : appsLists){
                if(!applicationInfo.enabled){

                    PackageInfo packageInfo = null;
                    try {
                        packageInfo = getPackageManager().getPackageInfo(applicationInfo.packageName, 0);
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (packageInfo != null) {
                        //Add this package to package list
                        Map<String, Object> packageItem = new HashMap<String, Object>();
                        // Add app icon
                        Drawable icon = mContext.getPackageManager()
                                .getApplicationIcon(packageInfo.applicationInfo);
                        packageItem.put(VIEW_ITEM_ICON, icon);

                        // Add app name
                        String appName = mContext.getPackageManager()
                                .getApplicationLabel(packageInfo.applicationInfo).toString();
                        packageItem.put(VIEW_ITEM_TEXT, appName);
                        packageItem.put(VIEW_ITEM_PACKAGE_NAME, packageInfo.packageName);

                        // Add if app is disabled.
                        packageItem.put(VIEW_ITEM_CHECKBOX, true);

                        // Add to package list
                        if (AppUtil.isSystemApp(packageInfo.applicationInfo)) {
                            mSystemAppList.add(packageItem);
                        } else {
                            mPersonalAppList.add(packageItem);
                        }
                    }
                }
            }
        }
        private void sortPackageList() {
            // Sort package list in alphabetical order.
            PackageItemComparator comparator = new PackageItemComparator();
            
            // Sort personal app list
            if (mPersonalAppList != null) {
                Collections.sort(mPersonalAppList, comparator);
            }
            
            // Sort system app list
            if (mSystemAppList != null) {
                Collections.sort(mSystemAppList, comparator);
            }
            
            Log.i(TAG, "sortPackageList(), PersonalAppList=" + mPersonalAppList);
            Log.i(TAG, "sortPackageList(), SystemAppList=" + mSystemAppList);
        }        
    }    
}