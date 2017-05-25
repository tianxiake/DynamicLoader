package org.github.loader.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.widget.Toast;

import org.github.loader.intf.IOperationCallback;
import org.github.loader.intf.IOperationPartner;
import org.github.loader.intf.SdkMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author  NiYongliang on 2016/4/12.
 */
public class DynamicLoader implements IDynamicLoader {
    private static final String TAG ="DynamicLoader";
    private static volatile IDynamicLoader loadPyramidney;
    private Context context;
    private IOperationPartner operationPartner;
    private boolean fromServer=true;
    private int module_version=0;
    private DynamicDownload dynamicDownload = new DynamicDownload();
    /**
     * 150 支持服务器推送运营差价,修复再升级bug
     * 160 加载新模块.支持回滚机制.加载后不再修改路径.
     * 170 支持A加载B独立协议
     * 200 修改了新包名包名
     */
    //此版本号,在每次修改jar的代码的时候,需要更新并记录修改日志.每次涨10.
    public static final int JAR_VERSION =200;

    //当前使用的dex路径
    private String operationPath;
    //当前使用dex的备份路径
    private final String operationBackPath;
    //当前使用的oDex路径
    private final String oDexPath;

    private static final String OPERATION_SUB_PATH = "/ua/ua_56av5asd.dat";

    private DynamicLoader(Context context){
        this.context =context;
        operationPath = context.getFilesDir() + OPERATION_SUB_PATH;
        operationBackPath = context.getFilesDir() + "/ua/ua_56av5asd.back";
        oDexPath = context.getFilesDir() + "/ua/ua_54asdd3.dat";
    }

    public static IDynamicLoader getInstance(Context context){
        if(loadPyramidney ==null){
            synchronized (DynamicLoader.class){
                if(loadPyramidney ==null){
                    loadPyramidney =new DynamicLoader(context);
                }
            }
        }
        return loadPyramidney;
    }

    @Override
    public IOperationPartner getOperationPartner(){
        return operationPartner;
    }

    @Override
    public void loadDex(IOperationCallback operationCallback){
        loadDex(operationCallback,context.getAssets());
    }

    @Override
    public void loadDex(IOperationCallback operationCallback, AssetManager assetManager) {
        loadDex(operationCallback,assetManager,"dynamic");
    }

    /**
     *
     * @param operationCallback
     * @param assetManager  支持第三方assets
     */
    @Override
    public void loadDex(IOperationCallback operationCallback, AssetManager assetManager, String name){
        try {
            DynamicLogger.info(TAG, "loadDex operationCallback:" + operationCallback + "jar_version:" + JAR_VERSION);
            if (operationCallback == null) {
                return;
            }

            if (loadOperationPartnerInstall(context,operationCallback, JAR_VERSION)) {
                DynamicLogger.info(TAG, "loadOperationPartnerInstall success");
                // 加载的如果是手机上已经安装的运营main包，那么则弹出toast提示：使用已安装插件+运营版本号,主要是为了提示一下开发和测试人员.
                if (operationPartner != null) {
                    Toast.makeText(context, "使用已安装插件", Toast.LENGTH_LONG).show();
                }
                fromServer=false;
                return;
            }
            //加载安装插件不成功.下面走加载assets插件
            SharedPreferences dynamic = context.getSharedPreferences("dynamic", 0);
            //当前运营路径
            final String newOperationPath = dynamic.getString("dynamic", "");
            //外部版本号
            final String out_module_version = dynamic.getString("out_module_version", "");

            if (!TextUtils.isEmpty(operationPath)) {
                //清理掉临时路径.单次有效
                SharedPreferences.Editor edit = dynamic.edit();
                edit.putString("dynamic", null);
                edit.commit();
            }

            String softVersion = operationCallback.getSoftVersion();
            DynamicLogger.info(TAG, "softVersion:" + softVersion + " out_module_version:" + out_module_version);
            boolean result = false;
            //版本一致走普通加载逻辑
            if (TextUtils.equals(softVersion, out_module_version)) {
                //有新的运营模块加载新模块
                if (!TextUtils.isEmpty(newOperationPath)) {
                    DynamicLogger.info(TAG, "load next loadNewDexFile operationPath:"+newOperationPath);

                    File newOperationFile = new File(newOperationPath);
                    if (testAndUse(newOperationFile, operationCallback)) {
                        return;
                    }
                }

                File operationBackFile=new File(this.operationBackPath);
                if(operationBackFile.isFile()&&operationBackFile.exists()&&operationBackFile.canRead()){
                    DynamicFileUtil.rename(this.operationBackPath,this.operationPath);
                }
                File operationFile = new File(this.operationPath);
                if (operationFile.isFile() && operationFile.exists() && operationFile.canRead()) {
                    result = loadOperationInner(this.operationPath, this.oDexPath, operationCallback, JAR_VERSION);
                }
                DynamicLogger.info(TAG, "load Partner result:" + result);
            }
            if (!result) {
                DynamicLogger.info(TAG, "use assets dex!!!");
                DynamicFileUtil.copyAssetDexToData(name,this.operationPath, assetManager);
                result = loadOperationInner(this.operationPath, this.oDexPath, operationCallback, JAR_VERSION);
                DynamicLogger.info(TAG, "loadAssetsDexResult:" + result);
                if (result) {
                    SharedPreferences.Editor edit = dynamic.edit();
                    edit.putString("out_module_version", softVersion);
                    edit.putString("dynamic", null);
                    edit.commit();
                }
            }
            if (!result) {
                downloadFromServer(true, operationCallback,name);
            }
        }finally {
            downloadFromServer(false,operationCallback,name);
        }
    }

    private void downloadFromServer(final boolean load,final IOperationCallback operationCallback,final String name){
        if("com.vlife.nubia.wallpaper".equals(context.getPackageName())){
            final ConnectivityManager connectMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo info = connectMgr.getActiveNetworkInfo();
            final int type;
            if (info == null) {
                type = -2;
            } else {
                type = info.getType();
            }
            if(ConnectivityManager.TYPE_WIFI!=type){
                DynamicLogger.warn(
                        TAG,"nubia not wifi not update");
                return;
            }
        }
        SharedPreferences dynamic = context.getSharedPreferences("dynamic", 0);
        long from_server_time=dynamic.getLong("from_server_time",0);
        long network_interval=dynamic.getLong("network_interval",6*3600*1000);
        DynamicLogger.warn(TAG,"from_server_time:"+from_server_time+" network_interval:"+network_interval);
        if(System.currentTimeMillis()-from_server_time<network_interval){
            DynamicLogger.info(TAG,"network_interval return");
            return;
        }else {
            dynamic.edit().putLong("from_server_time",System.currentTimeMillis()).commit();
        }

        DynamicLogger.info(TAG,"use download dex!!! fromServer:"+fromServer+" load:"+load);

        if (fromServer){
            fromServer=false;
            new Thread(){
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        final String serverDexPath =context.getFilesDir().getParentFile()+"/databases/server.db";
                        int operationVersion=0;
                        if(operationPartner!=null){
                            operationVersion=operationPartner.getOperationVersion();
                        }
                        if(dynamicDownload.download(serverDexPath,operationCallback.getSoftVersion(),name,operationVersion,operationCallback,context)) {
                            if (load) {
                                testAndUse(new File(serverDexPath), operationCallback);
                            } else {
                                SharedPreferences dynamic = context.getSharedPreferences("dynamic", 0);
                                dynamic.edit().putString("dynamic", serverDexPath).commit();
                                DynamicLogger.info(TAG, "download fromServer:" + serverDexPath);
                            }
                        }
                    } catch (Throwable t) {
                        DynamicLogger.warn(TAG, "downloadFromServer", t);
                    }
                }
            }.start();
        }
    }

    private boolean testAndUse(File operationFile,IOperationCallback operationCallback){
        DynamicLogger.warn(TAG,"testAndUse operationFile:"+operationFile);
        if (operationFile.isFile() && operationFile.exists()) {
            try {
                operationPath = context.getFilesDir() + OPERATION_SUB_PATH;
                DynamicFileUtil.rename(operationPath, operationBackPath);
                DynamicFileUtil.copyFileTo(new FileInputStream(operationFile), operationPath, operationFile.getName().endsWith(".db"));
                operationFile.delete();
                boolean loadResult = loadOperationInner(operationPath,oDexPath, operationCallback, JAR_VERSION);
                DynamicLogger.info(TAG,"loadNewDexFile loadResult:"+loadResult);
                if (loadResult) {
                    //加载成功删除
                    new File(operationBackPath).delete();
                    return true;
                }else {
                    //加载失败删除
                    new File(operationPath).delete();
                    DynamicFileUtil.rename(operationBackPath, operationPath);
                    DynamicLogger.info(TAG,"load new DexFile error !!! can't use!!!");
                }
            } catch (IOException e) {
                DynamicLogger.warn("TAG", "copy operation dex copyFileAndDecodeTo failed!!");
            }
        }else{
            DynamicLogger.info(TAG,"new DexFile don't exist or not a file !!!");
        }
        return false;
    }

    /**
     * 加载未安装运营
     * @param apkPath
     * @param oDexPath
     * @param operationCallback
     * @param jar_version
     * @return
     */
    private boolean loadOperationInner(String apkPath, String oDexPath, IOperationCallback operationCallback, int jar_version){
        DynamicLogger.info(TAG, "loadOperationPartner path:" + apkPath + " operationCallback:" + operationCallback + " jar_version:" + jar_version);
        File apkFile = new File(apkPath);
        DynamicLogger.info(TAG, "dexFile.length:" + apkFile.length());
        if (apkFile.isFile() && apkFile.exists()) {
            String className=null;
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA);
                className=packageInfo.applicationInfo.className;
                module_version=packageInfo.versionCode;
            }catch (Exception e){
                DynamicLogger.warn(TAG,"loadClass failed!!! filepath:"+apkPath);
            }
            try {
                String libDir=context.getFilesDir()+"/ua/"+module_version+"/";
                DynamicClassLoader classLoader = new DynamicClassLoader(apkPath,oDexPath,libDir,ClassLoader.getSystemClassLoader());
                this.operationPartner = loadClass(classLoader,className,context,jar_version,IOperationPartner.class);
                if(this.operationPartner!=null){
                    this.operationPartner.setPluginPath(apkPath);
                    boolean result=this.operationPartner.setCallback(operationCallback);
                    DynamicLogger.info(TAG, "set call back result:" + result);
                    return result;
                }else {
                    DynamicLogger.info(TAG, "operationPartner==null");
                }
            } catch (IOException e) {
                DynamicLogger.warn(TAG,"loadClass failed!!! filepath:"+apkPath);
            }
        }
        return false;
    }

    /**
     * 加载已安装运营
     * @param context
     * @param operationCallback
     * @param jar_version
     * @return
     */
    private boolean loadOperationPartnerInstall(Context context,IOperationCallback operationCallback,int jar_version){
        try {
            DynamicLogger.debug(TAG, "loadOperationPartnerInstall");
            PackageManager packageManager=context.getPackageManager();
            ApplicationInfo applicationInfo=packageManager.getApplicationInfo("com.github.dynamic.main",PackageManager.GET_META_DATA);
            String apkPath=applicationInfo.sourceDir;
            return loadOperationInner(apkPath,this.oDexPath,operationCallback,jar_version);
        } catch (PackageManager.NameNotFoundException e) {
            DynamicLogger.warn(TAG, "not_have_install_plugin");
            return false;
        }
    }

    /**
     * 加载运营类
     * @param classLoader
     * @param context
     * @param jar_version
     * @param infClass
     * @param <T>
     * @return
     */
    private <T> T loadClass(ClassLoader classLoader,String className,Context context,int jar_version,Class<T> infClass){
        try {
            if(className==null) {
                className = "com.dynamic.push.OperationPartner";
            }
            DynamicLogger.debug(TAG, classLoader+" loadClass className:"+className);
            Class<?> c = classLoader.loadClass(className);
            Constructor<?> constructor = c.getDeclaredConstructor(Context.class, Class.class, int.class);
            constructor.setAccessible(true);
            InvocationHandler invocationHandler = (InvocationHandler) constructor.newInstance(context, SdkMethod.class, jar_version);
            DynamicLogger.info(TAG, "invocationHandler invocationHandler:" + invocationHandler + "infClass" + infClass);
            Object proxy= Proxy.newProxyInstance(DynamicLoader.class.getClassLoader(), new Class[]{infClass}, invocationHandler);
            return (T)proxy;
        } catch (Exception e) {
            DynamicLogger.error(TAG, "e:" + e);
            return null;
        }
    }



    /**
     * 此接口实现待验证
     * @param zipPath
     */
    @Override
    public void updateOperationModule(String zipPath) {
        try {
            String apkPath = context.getFilesDir().getParentFile().getAbsolutePath() + "/databases/dynamic_zip.db";
            boolean result = DynamicFileUtil.findFile(new File(zipPath), "10950.vld", apkPath);
            if(result) {
                PackageInfo apkPackageInfo = context.getPackageManager().getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA);
                if (apkPackageInfo != null) {
                    DynamicLogger.info(TAG, "module_version:" + module_version + "  apk-ver-code:" + apkPackageInfo.versionCode);
                    if (apkPackageInfo.versionCode > module_version) {
                        SharedPreferences dynamic = context.getSharedPreferences("dynamic", 0);
                        SharedPreferences.Editor edit = dynamic.edit();
                        edit.putString("dynamic", apkPath);
                        edit.apply();
                    }
                }
            }
        } catch (Exception e) {
            DynamicLogger.error(TAG,"",e);
        }
    }
}
