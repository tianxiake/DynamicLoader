package org.github.loader.sdk;

import android.content.res.AssetManager;

import org.github.loader.intf.IOperationCallback;
import org.github.loader.intf.IOperationPartner;

/**
 * Created by yum on 16/9/20.
 */
public interface IDynamicLoader<T> {
    /**
     * 加载运营
     * @param  intfClass
     * @param operationCallback
     */
    void loadDex(Class<T> intfClass,IOperationCallback operationCallback);

    /**
     * 加载运营
     * @param intfClass
     * @param operationCallback
     * @param assetManager
     * @param name
     */
    void loadDex(Class<T> intfClass,IOperationCallback operationCallback, AssetManager assetManager,String name);

    /**
     * 通过zip包做离线跟新操作
     * @param zipPath
     */
    void updateOperationModule(String zipPath);

    /**
     * 获取运营
     * @return
     */
    T getT();
}
