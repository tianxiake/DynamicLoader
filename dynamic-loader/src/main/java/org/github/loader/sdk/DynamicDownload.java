package org.github.loader.sdk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.github.loader.intf.IOperationCallback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

/**
 * Created by yum on 16/9/20.
 */
public class DynamicDownload {
    private static final String TAG = "DynamicDownload";
    private DynamicProtocol protocol=new DynamicProtocol();
    private String download;

    public boolean download(String path, String softVersion, String name, int operationVersion, IOperationCallback operationCallback, Context context){
        String gateway=getGatewayUrl(softVersion);
        if(gateway==null){
            return false;
        }
        DownloadData downloadData=getDownloadUrl(context,operationCallback,gateway,name,operationVersion);
        if(downloadData==null) {
            DynamicLogger.info(TAG, "downloadData is null");
            return false;
        }
        try {
            int currentType= DynamicNetUtil.getNetworkTypeStatus(context).getType();
            if(downloadData.network_type<currentType){
                DynamicLogger.info(TAG, "network type return currentType:"+currentType+" network_type:"+downloadData.network_type);
                return false;
            }
            URL url=new URL(downloadData.url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            if (200 == connection.getResponseCode()) {
                long length = connection.getContentLength();
                if (length < 10 * 1024 * 1024) {
                    InputStream is=connection.getInputStream();
                    DynamicFileUtil.copyFileTo(is, path, false);
                    if (new File(path).length() == length) {
                        DynamicLogger.info(TAG, "download success");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            DynamicLogger.error(TAG, "error: ", e);
        }
        return false;
    }

    private DownloadData getDownloadUrl(Context context,IOperationCallback callback, String gateway, String name, int operationVersion){
        try {
            JSONObject requestObject=new JSONObject();
            requestObject.put("id", new Random().nextLong());
            requestObject.put("time", System.currentTimeMillis());
            requestObject.put("version","2");
            JSONObject dataJson= new JSONObject();
            JSONObject userObject=new JSONObject();
            userObject.putOpt("soft_version",callback.getSoftVersion());
            userObject.putOpt("operation",operationVersion);
            userObject.putOpt("package",context.getPackageName());
            userObject.putOpt("channel_id",callback.getChannelID());
            userObject.putOpt("brand",Build.BRAND);
            userObject.putOpt("model",Build.MODEL);
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                userObject.putOpt("shell_version", info.versionName);
            }catch (Exception e){
                DynamicLogger.warn(TAG,"",e);
            }
            try {
                int pid = android.os.Process.myPid();
                ActivityManager mActivityManager = (ActivityManager) context
                        .getSystemService(Context.ACTIVITY_SERVICE);
                String host = "unknown";
                for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager
                        .getRunningAppProcesses()) {
                    if (appProcess.pid == pid) {
                        host = appProcess.processName;
                        break;
                    }
                }
                userObject.putOpt("host",host);
            }catch (Exception e) {
                DynamicLogger.warn(TAG,"",e);
            }
            try{
                userObject.putOpt("android_id",Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
            } catch (Exception e){
                DynamicLogger.warn(TAG,"",e);
            }
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    String operator = telephonyManager.getNetworkOperator();
                    if (operator != null && operator.length() == 5) {
                        userObject.putOpt("mcc",operator.substring(0, 3));
                        userObject.put("mnc", operator.substring(3));
                    }
                    if(PackageManager.PERMISSION_GRANTED==context.checkPermission("android.permission.READ_PHONE_STATE", Process.myPid(),Process.myUid())) {
                        userObject.putOpt("imei", telephonyManager.getDeviceId());
                    }
                }
            } catch (Throwable e) {
                DynamicLogger.warn(TAG,"",e);
            }
            dataJson.put("user",userObject);
            JSONObject upgrade=new JSONObject();
            upgrade.put("name",name);
            upgrade.put("version",operationVersion);
            dataJson.put("upgrade",upgrade);
            requestObject.put("data", dataJson);
            String request=requestObject.toString();
            DynamicLogger.info(TAG,"request:"+request);
            byte[] resBytes=protocol.send(gateway+"e/fj",request.getBytes(DynamicUtil.CHARSET));
            String response=new String(resBytes, DynamicUtil.CHARSET);
            DynamicLogger.info(TAG,"response:"+response);
            JSONObject responseObject=new JSONObject(response);
            JSONObject dataObject=responseObject.optJSONObject("data");
            if(dataObject!=null){
                long network_interval=dataObject.optLong("network_interval");
                if(network_interval>0) {
                    SharedPreferences dynamic = context.getSharedPreferences("dynamic", 0);
                    dynamic.edit().putLong("network_interval", network_interval).commit();
                }
                JSONObject upgradeObject=dataObject.optJSONObject("upgrade");
                if(upgradeObject!=null) {
                    int network_type = upgradeObject.optInt("network_type");
                    JSONObject file = upgradeObject.optJSONObject("file");
                    DownloadData downloadData = new DownloadData();
                    downloadData.network_type = network_type;
                    if (file != null) {
                        String url = file.optString("url");
                        downloadData.hash = file.optString("hash");
                        downloadData.length = file.optInt("length");
                        if (url != null && !url.toLowerCase().startsWith("http")) {
                            String downloadUrl = download + url;
                            DynamicLogger.info(TAG, "downloadUrl:" + downloadUrl);
                            downloadData.url = downloadUrl;
                            return downloadData;
                        }
                    }
                }
            }
        } catch (Exception e) {
            DynamicLogger.error(TAG, "error: ", e);
        }
        return null;
    }

    private String getGatewayUrl(String softVersion){
        String server_address="http://f1.qulingcloud.com/f/address.api";
        if(DynamicLogger.isLogEnable()){
            server_address="http://stage.3gmimo.com/front/address.api";
        }
        DynamicLogger.info(TAG,"softVersion:"+softVersion);
        try {
            StringBuilder urlBuilder=new StringBuilder(server_address).append("?");
            urlBuilder.append("p=a");
            urlBuilder.append("&v=5");
            urlBuilder.append("&pv="+ DynamicLoader.JAR_VERSION);
            urlBuilder.append("&model=").append(URLEncoder.encode(Build.MODEL,"UTF-8"));
            if(softVersion!=null) {
                urlBuilder.append("&sv=").append(URLEncoder.encode(softVersion, "UTF-8"));
            }
            String urlStr = urlBuilder.toString();
            DynamicLogger.info(TAG,"url:"+urlStr);
            URL url=new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            int code=connection.getResponseCode();
            if(code==200||code==206){
                byte[] bs= DynamicFileUtil.readBytesInputStream(connection.getInputStream(),10240);
                String json=new String(bs, DynamicUtil.CHARSET);
                String sign = connection.getHeaderField("sign");
                DynamicLogger.info(TAG,"sign:"+sign+" json:"+json);
                if(sign != null) {
                    if (json != null && DynamicUtil.verify(json, sign, DynamicUtil.public_key)) {
                        JSONObject jsonObject = new JSONObject(json);
                        JSONObject downloadObject = jsonObject.optJSONObject("download");
                        if(downloadObject!=null){
                            JSONArray addrs = downloadObject.optJSONArray("address");
                            if(addrs!=null&&addrs.length()>0){
                                this.download =addrs.optString(0);
                                DynamicLogger.info(TAG, "download:" + this.download);
                            }
                        }
                        JSONObject gatewayObject = jsonObject.optJSONObject("gateway");
                        if(gatewayObject!=null) {
                            JSONArray addrs = gatewayObject.optJSONArray("address");
                            if (addrs != null && addrs.length() > 0) {
                                String gateway = addrs.optString(0);
                                DynamicLogger.info(TAG, "gateway:" + gateway);
                                return gateway;
                            }
                        }
                    }
                }else{
                    DynamicLogger.warn(TAG,"sign is null");
                }
            }else {
                DynamicLogger.warn(TAG,"error code:"+code);
            }
        } catch (Exception e) {
            DynamicLogger.error(TAG,"error: ",e);
        }
        return null;
    }

    private class DownloadData{
        private String url;
        private int network_type;
        private int length;
        private String hash;
    }
}
