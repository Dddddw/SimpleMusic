package com.example.w.musicbroadcast;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 *
 * Created by W on 2016/9/6.
 */
public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfos){
            if (pid == appProcessInfo.pid){
                if (TextUtils.equals(appProcessInfo.processName, getPackageName())){
                    Log.i("well", "主进程初始化");
                }else if (TextUtils.equals(appProcessInfo.processName,getPackageName() + ":remoteService")){
                    Log.i("well", "服务进程被初始化");
                }
            }
        }
    }
}
