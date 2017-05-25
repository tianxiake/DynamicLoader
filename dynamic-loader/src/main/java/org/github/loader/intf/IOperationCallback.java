package org.github.loader.intf;

import java.lang.reflect.InvocationHandler;

/**
 * @author  niyongliang on 2016/3/1.
 * 为独立运营提供的常规接口
 */
public interface IOperationCallback extends InvocationHandler{

    /**
     * 获取系统渠道号码
     * @return
     */
    String getChannelID();

    /**
     * 获取宿主版本号码
     * @return
     */
    String getSoftVersion();

    /**
     * 获取用户名和密码
     * @return
     */
    String[] getUidAndPassword();

    /**
     *
     * @return 壁纸id
     */
    String getWallpaperId();

    /**
     *
     * @return 假锁屏是否正在显示
     */
    boolean falseLockScreenIsShowing();

    /**
     *
     * @return 锁屏id
     */
    String getLockScreenId();

    /**
     * 获取 用于支持的ServiceName
     */
    String getServiceName();

    /**
     * 阻塞Service
     * @return
     */
    boolean blockService();

    /**
     * 释放Service
     * @return
     */
    boolean releaseService();
}
