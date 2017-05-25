package org.github.loader.sdk;

import android.content.res.AssetManager;

import org.github.loader.intf.IOperationCallback;
import org.github.loader.intf.IOperationPartner;

import java.lang.reflect.InvocationHandler;

/**
 * Created by yum on 16/9/20.
 */
public interface IDynamicLoader {
    /**
     * 加载运营
     * @param  dynamicClass
     * @param callback
     */
    void loadDex(Class dynamicClass,InvocationHandler callback);

    /**
     * 加载运营
     * @param dynamicClass
     * @param callback
     * @param assetManager
     * @param name
     */
    void loadDex(Class dynamicClass,InvocationHandler callback, AssetManager assetManager,String name);

    /**
     * 通过zip包做离线跟新操作
     * @param zipPath
     */
    void updateDynamic(String zipPath);

    /**
     * 获取运营
     * @return
     */
    Object getDynamicObject();
}
