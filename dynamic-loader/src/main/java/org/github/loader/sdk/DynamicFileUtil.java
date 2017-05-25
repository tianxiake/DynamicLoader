package org.github.loader.sdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;

import org.github.loader.intf.DataDecode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by yum on 16/9/20.
 */
public class DynamicFileUtil {

    private static final String TAG = "DynamicFileUtil";

    public static byte[] readBytesInputStream(InputStream is, int buffer)
            throws IOException {
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int size = 0;
            byte[] bs = new byte[buffer];
            while ((size = is.read(bs)) != -1) {
                bos.write(bs, 0, size);
            }
            return bos.toByteArray();
        } finally {
            is.close();
        }
    }

    /**
     * 拷贝文件
     *
     * @param srcStream
     * @param destPath
     * @param decode
     * @throws IOException
     */
    public static void copyFileTo(InputStream srcStream, String destPath, boolean decode) throws IOException {
        DynamicLogger.debug(TAG, "[copy to] :" + destPath + ", decode:" + decode);
        File desFile = new File(destPath);
        createDir(desFile.getParentFile());
        OutputStream os = new FileOutputStream(desFile);
        DataDecode dataDecode = new DataDecode();
        try {
            int realLength = 0;
            byte[] buffer = new byte[10240];
            while ((realLength = srcStream.read(buffer)) != -1) {
                if (decode) {
                    dataDecode.switchData(buffer, 0, realLength);
                }
                os.write(buffer, 0, realLength);
            }
            buffer = null;
        } finally {
            os.close();
            srcStream.close();
        }
    }

    /**
     * 创建文件夹
     *
     * @param dir
     * @return
     */
    public static boolean createDir(File dir) {
        File parentFile = dir.getParentFile();
        if (parentFile == null) {
            return false;
        }
        DynamicLogger.debug(TAG, "parentDir:" + parentFile + ", exists:" + parentFile.exists() + ", canWrite:" + parentFile.canWrite());
        if (parentFile.exists()) {
            try {
                if (!parentFile.canWrite()) {
                    parentFile.setWritable(true, false);
                }
            } catch (Exception e) {
                DynamicLogger.error(TAG, "error:" + e);
            }
        } else {
            createDir(parentFile);
        }
        if (!dir.exists()) {
            DynamicLogger.debug(TAG, "mkdir:" + dir);
            return dir.mkdir();
        } else {
            return true;
        }
    }

    /**
     * 重命名
     *
     * @param src
     * @param dest
     */
    public static boolean rename(String src, String dest) {
        DynamicLogger.debug(TAG, "src:".concat(src).concat(", dest:").concat(dest));
        File srcFile = new File(src);
        File destFile = new File(dest);
        File backupFile = new File(dest + ".backup");
        if (srcFile.isFile()) {
            if (destFile.isFile()) {
                boolean b = destFile.renameTo(backupFile);
                DynamicLogger.debug(TAG, "dest renameTo back result:" + b);
            }
            createDir(destFile.getParentFile());
            boolean result = srcFile.renameTo(destFile);
            if (result) {
                backupFile.delete();
            } else {
                boolean b = backupFile.renameTo(destFile);
                DynamicLogger.debug(TAG, "back renameTo dest result:" + b);
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * 计算哈希
     *
     * @param file 文件
     * @return
     */
    public static String hash(File file) {
        MappedByteBuffer byteBuffer = null;
        FileInputStream in = null;
        FileChannel ch = null;
        try {
            in = new FileInputStream(file);
            ch = in.getChannel();
            byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(byteBuffer);
            return encodeHex(digest.digest());
        } catch (Throwable e) {
            return "error";
        } finally {
            if (byteBuffer != null) {
                byteBuffer.clear();
            }
            if (ch != null) {
                try {
                    ch.close();
                } catch (IOException e) {
                    DynamicLogger.error(TAG, "error:" + e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    DynamicLogger.error(TAG, "error:" + e);
                }
            }
        }
    }

    public static String encodeHex(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        int i;

        for (i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * 返回 安全可用的私有路径
     *
     * @return
     */
    public static String getSafePath(Context context) {
        String path = null;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                if (isExternalStorageAvailable()) {
                    if (PackageManager.PERMISSION_GRANTED == context.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", Process.myPid(), Process.myUid())) {
                        File externalFilesDir = context.getExternalFilesDir(null);
                        if (externalFilesDir != null) {
                            path = externalFilesDir.getAbsolutePath();
                        }
                    }
                }
            } else {
                // 此方法在 android 4.4以后不再需要 sdcard权限
                if (isExternalStorageAvailable()) {
                    File externalFilesDir = context.getExternalFilesDir(null);
                    if (externalFilesDir != null) {
                        path = externalFilesDir.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            DynamicLogger.error(TAG, "e:", e);
        }
        if (TextUtils.isEmpty(path)) {
            path = context.getFilesDir().getAbsolutePath();
        }
        DynamicLogger.debug(TAG, "final path:" + path);
        return path;
    }

    private static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 拷贝assets中的文件到指定目录
     *
     * @param toBeOpen     assets 中的文件名, db or de, 不带文件类型
     * @param destPath     目标路径
     * @param assetManager AssetManager
     */
    public static void copyAssetDexToData(String toBeOpen, String destPath, AssetManager assetManager) {
        DynamicLogger.info(TAG, "copyAssetDexToData(). toBeOpen:" + toBeOpen + ", destPath:" + destPath + ", assetManager:" + assetManager);
        if (assetManager == null) {
            return;
        }
        try {
            InputStream is = assetManager.open(toBeOpen + ".db");
            DynamicFileUtil.copyFileTo(is, destPath, true);
            return;
        } catch (IOException e) {
            DynamicLogger.warn(TAG, "copyAssetDexToData e:" + e);
        }
        try {
            InputStream is = assetManager.open(toBeOpen + ".de");
            DynamicFileUtil.copyFileTo(is, destPath, false);
        } catch (IOException e) {
            DynamicLogger.warn(TAG, "copyAssetDexToData e:" + e);
        }
    }


    /**
     * 修改文件权限,对外可读
     *
     * @param apkFile
     * @return
     */
    public static boolean ensureFile(File apkFile) {
        boolean result = apkFile.setReadable(true, false);
        DynamicLogger.debug(TAG, "result1:" + result);
        if (!result) {
            return false;
        }
        result = apkFile.setExecutable(true, false);
        DynamicLogger.debug(TAG, "result2:" + result);
        if (!result) {
            return false;
        }
        result = apkFile.setWritable(true, true);
        DynamicLogger.debug(TAG, "result3:" + result);
        if (!result) {
            return false;
        }
        File parentFile = apkFile.getParentFile();
        if (parentFile != null) {
            String absolutePath = parentFile.getAbsolutePath();
            DynamicLogger.debug(TAG, "Parent Path: " + absolutePath);
            if (TextUtils.equals(absolutePath, Environment.getDataDirectory() + File.separator + "data")) {
                DynamicLogger.debug(TAG, "result4:" + result);
                return true;
            } else {
                return ensureFile(parentFile);
            }
        } else {
            return true;
        }
    }

    /**
     * 遍历查询某路径下的所有文件，找出符合名称的文件
     * @param file 查找路径的文件对象
     * @param fileName 要查找的文件的文件名
     * @return 要读取的文件的byte[]
     */
    public static boolean findFile(File file, String fileName,String destFile) throws IOException {
        if (!file.exists()) {
            return false;
        }
        ZipFile zfile = new ZipFile(file);
        try {
            Enumeration<?> zList = zfile.entries();
            while (zList.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) zList.nextElement();
                if (!ze.isDirectory()) {
                    if (ze.getName().endsWith(fileName)) {
                        InputStream is=zfile.getInputStream(ze);
                        copyFileTo(is,destFile,false);
                        return true;
                    }
                }
            }
        }catch (Exception e){
            DynamicLogger.error(TAG,"",e);
        }finally {
            zfile.close();;
        }
        return false;
    }
}
