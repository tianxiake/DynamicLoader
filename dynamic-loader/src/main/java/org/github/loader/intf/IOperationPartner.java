package org.github.loader.intf;

import android.content.Intent;
import android.webkit.JavascriptInterface;

import java.lang.reflect.InvocationHandler;

/**
 * @author  by yum on 16/3/2.
 */
public interface IOperationPartner extends InvocationHandler{
    /**
     * 设置插件路径
     * @param path
     * @return
     */
    @JavascriptInterface
    @SdkMethod(methodName = "setPluginPath")
    boolean setPluginPath(String path);
    /**
     * 设置回调
     * @param callback
     */
    @JavascriptInterface
    @SdkMethod(methodName = "setCallback")
    boolean setCallback(InvocationHandler callback);

    /**
     * 通知系统消息
     * @param intent
     */
    @JavascriptInterface
    @SdkMethod(methodName = "systemCall")
    void systemCall(Intent intent);
}
