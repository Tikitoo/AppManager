package com.tikitoo.appmanager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String SEARCH_NULL_PACKAGE = "search_null_package";
    private  ListView app_list_view;
    private List<AppInfo> app_list, stack_all_app;
    List<AppInfo> stack_search_app = new ArrayList<AppInfo>();
    List<AppInfo> suspendAppInfoList = new ArrayList<AppInfo>();

    Stack<List<AppInfo>> stack = new Stack<List<AppInfo>>();
    private AppListAdapter app_list_adapter;
    private Context mCtx;
    public static final int GET_ALL_APP_FINISH = 123;
    public static final int GET_SEARCH_APP_FINISH = 124;

    private EditText searchText;
    private Button searchBtn;


    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_ALL_APP_FINISH:
                    /*List<AppInfo> app_list_old = (List<AppInfo>) msg.obj;
                    app_list.addAll(app_list_old);*/
                    AppInfo app_list_old = (AppInfo) msg.obj;
                    app_list.add(app_list_old);
                    // app_list_adapter.notifyDataSetChanged();
                    break;
                case GET_SEARCH_APP_FINISH:

                    List<AppInfo> app_list_old2 = (List<AppInfo>) msg.obj;
                    app_list.addAll(app_list_old2);
                    app_list_adapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCtx = getApplicationContext();

        initViews();
        app_list = new ArrayList<AppInfo>();
        app_list_view = (ListView) findViewById(R.id.app_list_view);
        app_list_adapter = new AppListAdapter(app_list, mCtx);
        app_list_view.setAdapter(app_list_adapter);

        getAllApps();
        app_list_view.setOnItemClickListener(new ListItemClickListener());
    }

    private void initViews() {
        searchText = (EditText) findViewById(R.id.app_search_text);
        searchBtn = (Button) findViewById(R.id.app_search_btn);
        searchBtn.setOnClickListener(this);
    }

    public  List<AppInfo> getAllApps() {
        stack_all_app = new ArrayList<AppInfo>();
        final PackageManager pm = mCtx.getPackageManager();

        List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        AppInfo myAppInfo;
        for (PackageInfo packageInfo : packageInfos) {
            myAppInfo = new AppInfo();
            String packageName = packageInfo.packageName;
            final ApplicationInfo appInfo = packageInfo.applicationInfo;
            Drawable icon = appInfo.loadIcon(pm);
            String appName = appInfo.loadLabel(pm).toString();

            myAppInfo.setAppName(appName);
            myAppInfo.setPkgName(packageName);
            myAppInfo.setIcon(icon);
            myAppInfo.setSystemApp(!filterApp(appInfo));

            if (!myAppInfo.isSystemApp()) {
                stack_all_app.add(myAppInfo);
                sendMsgHandler(myAppInfo, GET_ALL_APP_FINISH);
            }
        }
        stack.add(stack_all_app);

        return stack_all_app;
    }

    private void sendMsgHandler(final AppInfo myAppInfo, final int MSG_WHAT) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.obj = myAppInfo;
                msg.what = MSG_WHAT;
                handler.sendMessage(msg);
            }
        }).start();
    }

    private void sendMsgHandler(final List<AppInfo> myAppInfo, final int MSG_WHAT) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.obj = myAppInfo;
                msg.what = MSG_WHAT;
                handler.sendMessage(msg);
            }
        }).start();
    }

    public static boolean filterApp(ApplicationInfo appInfo) {
        boolean flag = false;
        if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            flag = true;
        } else if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            flag = true;
        }
        return flag;
    }


    class ListItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final String pkgName = app_list.get(i).getPkgName();

            AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                /*builderSingle.setIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                builderSingle.setTitle("Select One Name:-");*/
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    MainActivity.this,
                    android.R.layout.select_dialog_item);
            arrayAdapter.add("Open App");
            arrayAdapter.add("AppInfo");
            arrayAdapter.add("UnInstall");
            builderSingle.setNegativeButton("cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builderSingle.setAdapter(arrayAdapter,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // showToast("which: " + which);
                            switch (which) {
                                case 0:
                                    try {
                                        Intent launcherIntent = getPackageManager().getLaunchIntentForPackage(pkgName);
                                        startActivity(launcherIntent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 1:
                                    //redirect user to app Settings
                                    Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    i.addCategory(Intent.CATEGORY_DEFAULT);
                                    i.setData(Uri.parse("package:" + pkgName));
                                    startActivity(i);
                                    break;
                                case 2:
                                    Intent unInstallIntent = new Intent(Intent.ACTION_DELETE);
                                    unInstallIntent.setData(Uri.parse("package:" + pkgName));
                                    startActivity(unInstallIntent);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
            if (pkgName != SEARCH_NULL_PACKAGE) {
                builderSingle.show();
            }
        }
    }

    private void searchApp() {
        String searchStr = searchText.getText().toString().toLowerCase();
        searchText.setText("");
        if (stack_search_app != null) {
            stack_search_app.clear();
        }
        for (AppInfo appInfo : app_list) {
            if (appInfo.getPkgName().toLowerCase().contains(searchStr) || appInfo.getAppName().toLowerCase().contains(searchStr)) {
                stack_search_app.add(appInfo);
                // sendMsgHandler(appInfo, GET_SEARCH_APP_FINISH);
            }
        }

        if (app_list != null) {
            app_list.clear();
        }

        if (stack_search_app.size() == 0) {

            AppInfo appInfo = new AppInfo();
            appInfo.setPkgName(SEARCH_NULL_PACKAGE);
            /*appInfo.setAppName("Search is null");
            showToast("Search is null");*/
            stack_search_app.add(appInfo);
        }

        stack.add(stack_search_app);
        sendMsgHandler(stack_search_app, GET_SEARCH_APP_FINISH);
    }

    public void showToast(String toastMsg) {
        Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.app_search_btn:
                searchApp();
                break;
            default:
                break;
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        showToast("onBackPressed");
        if (app_list != null) {
            app_list.clear();
        }

        stack.pop();
        if (!stack.isEmpty()) {
            List<AppInfo> appInfoList = stack.peek();
            sendMsgHandler(appInfoList, GET_SEARCH_APP_FINISH);
        } else {
            // super.onBackPressed();
            super.moveTaskToBack(true);
            /*super.onStop();
            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);*/
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        stack.add(suspendAppInfoList);
            sendMsgHandler(suspendAppInfoList, GET_SEARCH_APP_FINISH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!stack_all_app.isEmpty()) {
            suspendAppInfoList = stack_all_app;
        } else {
            // suspendAppInfoList = stack.peek();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        overridePendingTransition(R.anim.abc_fade_in,
                R.anim.abc_fade_out);
    }

}

class AppListAdapter extends BaseAdapter {
    private List<AppInfo> list;
    private Context context;

    public AppListAdapter() {
    }

    public AppListAdapter(List<AppInfo> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public void add(AppInfo appInfo) {
        list.add(appInfo);
        // notifyDataSetChanged();
        notifyDataSetInvalidated();
    }

    public void addAll(List<AppInfo> appInfo) {
        list.addAll(appInfo);
        // notifyDataSetChanged();
        notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        AppInfo appInfo = list.get(i);

        if (convertView == null) {
            ViewHolder viewHolder = new ViewHolder();
            View view = View.inflate(context, R.layout.list_view_item, null);
            viewHolder.icon = (ImageView) view.findViewById(R.id.app_icon);
            viewHolder.appName = (TextView) view.findViewById(R.id.app_name);
            viewHolder.appPkgName = (TextView) view.findViewById(R.id.app_pkg_name);

            viewHolder.icon.setImageDrawable(appInfo.getIcon());
            viewHolder.appName.setText(appInfo.getAppName());
            viewHolder.appPkgName.setText(appInfo.getPkgName());
            view.setTag(viewHolder);
            return view;
        } else {
            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.icon.setImageDrawable(appInfo.getIcon());
            viewHolder.appName.setText(appInfo.getAppName());
            viewHolder.appPkgName.setText(appInfo.getPkgName());
            return convertView;
        }
    }

    private class ViewHolder {
        ImageView icon;
        TextView appName;
        TextView appPkgName;
    }
}
