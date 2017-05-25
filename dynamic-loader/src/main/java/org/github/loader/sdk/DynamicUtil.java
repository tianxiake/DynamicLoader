package org.github.loader.sdk;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by niyongliang on 2017/5/16.
 */

public class DynamicUtil {
    private static final String TAG="DynamicUtil";
    public static final String public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDTALK7WxEK0q79k355T1WWyqPiIEMstWgDt9Z/zRXrjjTrbha4XePkjeG0ssn8sqpT9yIIHD3II/7LHki8uixgmWogWaAGUArtJ24ICllCUcIytEG5eWQ9SPK3NTgT8w1jOxmKTosWqrTPUA98fbz8o+mfnfjruHN80BxWSzCpBwIDAQAB";
    public static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String SIGN_ALGORITHMS = "SHA1WithRSA";
    private static final String KEY_FACTORY = "RSA";
    private static final String KEY_CIPHER = "RSA/ECB/PKCS1Padding";
    private static final int MAX_ENCRYPT_BLOCK = 117;
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * RSA验签名检查
     * @param content			待签名数据
     * @param sign				签名值
     * @param public_key		公钥
     * @return					布尔值
     */
    public static boolean verify(String content, String sign, String public_key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode(public_key,Base64.DEFAULT);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
            signature.initVerify(pubKey);
            signature.update(content.getBytes(CHARSET));
            return signature.verify(Base64.decode(sign,Base64.DEFAULT));
        } catch (Exception e) {
            DynamicLogger.error(TAG,"error: ",e);
        }
        return false;
    }

    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param data 源数据
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPublicKey(byte[] data)
            throws Exception {
        PublicKey key = getPublicKey(public_key);
        // 对数据加密
        Cipher cipher = Cipher.getInstance(KEY_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        int inputLen = data.length;
        int offSet = 0;
        byte[] cache;
        int i = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * 得到公钥
     * @param key	公钥（经过base64编码）
     * @return
     * @throws Exception
     */
    private static PublicKey getPublicKey(String key) throws Exception {
        byte[] keyBytes = Base64.decode(key,Base64.DEFAULT);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY);
        return keyFactory.generatePublic(x509KeySpec);
    }

    /**
     * 用公钥解密
     * @param data		密文（经过base64编码）
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] data)
            throws Exception {
        PublicKey key = getPublicKey(public_key);
        Cipher cipher = Cipher.getInstance(KEY_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key);
        int inputLen = data.length;
        int offSet = 0;
        byte[] cache;
        int i = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    public static String hash(String name) {
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
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException nsae) {
            return "unknown";
        }
    }
}
