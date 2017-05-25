package org.github.loader.intf;

/**
 * copy from DataSwitch 模块中也需要解密算法，使用相同的解密算法
 * @author  niyongliang on 2016/3/23.
 */
public class DataDecode {
    private final static byte[] key = {108,115,102,105,117,97,101,102,117,97,101,100,59,108,115,100,117,117,118,91,111,101,114,111,112,97,114,101,105,117,114,97,51,106,114,108,97,107,114,106,50,55,48,113,57,51,50,56,52,108,107,97,63,62,60,63,60,125,123,81,123,33,64,64,41,35,40,35,38,94,36,37,64,38,64,40,35,94,64,38,73,38,68,87,85,69,87,69,69,119,101,50,36,94,40,74,52,53,54,123,125,124,63,64,35,36,70,84,69,69,64,35,36,64,35,36,66,121,94,78,105,66,97,111,71,97,110,103};
    private long index;

    public void skip(long byteCount){
        index+=byteCount;
    }

    public int switchData(int b){
        return  b ^ key[(int)(index++ % key.length)];
    }

    public void switchData(byte[] data,int offset,int count){
        int end=offset+count;
        for (int i = offset; i < end; i++) {
            data[i] = (byte) (data[i] ^ key[(int)(index++ % key.length)]);
        }
    }
}
