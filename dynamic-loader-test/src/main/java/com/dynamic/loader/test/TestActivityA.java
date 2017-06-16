package com.dynamic.loader.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.github.loader.intf.IOperationCallback;
import org.github.loader.intf.IOperationPartner;
import org.github.loader.intf.ProxyUtil;
import org.github.loader.sdk.IDynamicLoader;
import org.github.loader.sdk.DynamicLoader;

import java.lang.reflect.Method;

/**
 * Created by niyongliang on 2017/5/8.
 */

public class TestActivityA extends Activity implements IOperationCallback,View.OnClickListener{
    private IOperationPartner operationPartner;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IDynamicLoader loadPyramidney= DynamicLoader.getInstance(getApplicationContext());
        loadPyramidney.loadDex(IOperationPartner.class,this);
        this.operationPartner=(IOperationPartner) loadPyramidney.getDynamicObject();
        button=new Button(this);
        button.setText("发送");
        button.setOnClickListener(this);
        setContentView(button);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public String getChannelID() {
        return "998";
    }

    @Override
    public String getSoftVersion() {
        return "2.5.6";
    }

    @Override
    public String[] getUidAndPassword() {
        return new String[0];
    }

    @Override
    public String getWallpaperId() {
        return null;
    }

    @Override
    public boolean falseLockScreenIsShowing() {
        return false;
    }

    @Override
    public String getLockScreenId() {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public boolean blockService() {
        return false;
    }

    @Override
    public boolean releaseService() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return ProxyUtil.invoke(this,proxy,method,args,true);
    }
}
