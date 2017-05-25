package org.github.loader.sdk;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

/**
 * @author  NiYongliang on 2016/4/13.
 */
public class DynamicClassLoader extends ClassLoader{
    private final String originalPath;
    private DexFile dexFile;
    private final ZipFile zipFile;
    private String libraryPath;

    public DynamicClassLoader(String apkPath, String oDexFile, String libraryPath, ClassLoader parent) throws IOException{
        super(parent);
        this.originalPath = apkPath;
        this.libraryPath =libraryPath;
        this.zipFile=new ZipFile(apkPath);
        if(oDexFile!=null) {
            DynamicFileUtil.createDir(new File(oDexFile).getParentFile());
            new File(oDexFile).delete();
        }
        dexFile = DexFile.loadDex(apkPath,oDexFile,0);
        if(libraryPath!=null){
            unzipLibrary();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class clazz = this.dexFile.loadClass(name,this);
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    @Override
    protected URL findResource(String name) {
        if(zipFile.getEntry(name) != null){
            try{
                return new URL("jar:" + new File(originalPath).toURL() + "!/" + name);
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }else {
            return null;
        }
    }

    @Override
    protected String findLibrary(String libName) {
        String fileName = "ua_"+hash(System.mapLibraryName(libName))+".dat";
        File soFile = new File(this.libraryPath, fileName);
        if (soFile.exists() && soFile.isFile() && soFile.canRead()) {
            DynamicLogger.info("DynamicClassLoader", libName+ " findLibrary:" + soFile.getPath());
            return soFile.getPath();
        }else {
            return null;
        }
    }

    private void unzipLibrary() {
        Enumeration<? extends ZipEntry> entries=zipFile.entries();
        String cpuABI = getCpuAbi();
        // 目前armeabi 和armeabi-v7a都看做是armeabi
        if (cpuABI.startsWith("armeabi")) {
            cpuABI = "armeabi";
        }
        String startPath="lib/"+cpuABI+"/";
        while(entries.hasMoreElements()){
            ZipEntry zipEntry=entries.nextElement();
            String name=zipEntry.getName();
            if(name.startsWith(startPath)&&name.endsWith(".so")){
                String soPath=libraryPath+"ua_"+hash(name.substring(name.lastIndexOf("/")+1))+".dat";
                if(!new File(soPath).exists()){
                    try {
                        InputStream is = this.zipFile.getInputStream(zipEntry);
                        DynamicFileUtil.copyFileTo(is, soPath,false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



    /**
     * 获取cpu类型
     *
     * @return
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("DefaultLocale")
    public static final String getCpuAbi() {
        // 记录CPU的类型，注意android2.2以后，会有两个CPU类型
        String aCPU_ABI = android.os.Build.CPU_ABI;
        String aCPU_ABI2 = android.os.Build.CPU_ABI2;
        // 判断CPU的架构
        if (aCPU_ABI == null || aCPU_ABI.isEmpty()) {
            aCPU_ABI = "armeabi";
        }
        if (aCPU_ABI.indexOf("x86_64") != -1
                || aCPU_ABI2.indexOf("x86_64") != -1) {
            return "x86_64";
        } else if (aCPU_ABI.indexOf("x86") != -1
                || aCPU_ABI2.indexOf("x86") != -1) {
            return "x86";
        } else if (aCPU_ABI.indexOf("arm64-v8a") != -1
                || aCPU_ABI2.indexOf("arm64-v8a") != -1) {
            return "arm64-v8a";
        } else if (aCPU_ABI.indexOf("armeabi-v7a") != -1
                || aCPU_ABI2.indexOf("armeabi-v7a") != -1) {
            return "armeabi-v7a";
        } else if (aCPU_ABI.indexOf("mips") != -1
                || aCPU_ABI2.indexOf("mips") != -1) {
            return "mips";
        } else {
            return "armeabi";
        }
    }

    public String hash(String name) {
        try{
            MessageDigest digest = MessageDigest.getInstance("md5");
            // Now, compute hash.
            digest.update(name.getBytes());
            byte[] bytes=digest.digest();
            StringBuilder buf = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                if (((int)bytes[i] & 0xff) < 0x10) {
                    buf.append("0");
                }
                buf.append(Long.toString((int)bytes[i] & 0xff, 16));
                if(i>2){
                    break;
                }
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException nsae) {
            return "unknown";
        }
    }
}
