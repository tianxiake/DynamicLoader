package org.github.loader.intf;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by yum on 2015/12/28.
 */
public class ProxyUtil {

    public static Object invoke(Object src,Object proxy, Method method, Object[] args,boolean log) throws Throwable {
        Method m = null;
        try {
            m = src.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (Exception e) {
            if(log) {
                Log.e("invoke", "error:" + e);
            }
        }
        if (m != null) {
            return m.invoke(src, args);
        } else {
            return returnPrimitiveNull(method.getReturnType());
        }
    }

    /**
     * @param c 原生类型 class
     * @return 原生类型的返回值
     */
    public static Object returnPrimitiveNull(Class<?> c){
        if (c == int.class || c == Integer.class) {
            return 0;
        } else if (c == long.class || c == Long.class) {
            return 0L;
        } else if (c == double.class || c == Double.class) {
            return 0.0D;
        } else if (c == float.class || c == Float.class) {
            return 0.0F;
        } else if (c == boolean.class || c == Boolean.class) {
            return false;
        } else if (c == byte.class || c == Byte.class) {
            return (byte) 0;
        } else if (c == char.class || c == Character.class) {
            return (char) 0;
        } else if (c == short.class || c == Short.class) {
            return 0;
        } else {
            return null;
        }
    }
}
