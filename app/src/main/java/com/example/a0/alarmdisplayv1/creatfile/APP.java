package com.example.a0.alarmdisplayv1.creatfile;

import android.app.Application;

import org.xutils.x;

/**
 * Created by yangpengfei10 on 2018/8/8.
 */

public class APP extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        x.Ext.init(this);
        x.Ext.setDebug(BuildConfig.DEBUG); // 是否输出debug日志, 开启debug会影响性能.

    }
}
