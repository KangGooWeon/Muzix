package com.example.muzix;

import android.app.Activity;
import android.app.Application;

import com.kakao.auth.KakaoSDK;

public class GlobalApplication extends Application {

    private static GlobalApplication instance;
    private static volatile Activity currentActivity = null;
    public static GlobalApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("This Application does not inherit com.kakao.GlobalApplication");
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalApplication.instance = this;

        // Kakao Sdk 초기화
        KakaoSDK.init(new KaKaoSDKAdapter());
    }

    public static Activity getCurrentActivity(){return currentActivity;}
    public static void setCurrentActivity(Activity currentActivity){
        GlobalApplication.currentActivity = currentActivity;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        GlobalApplication.instance = null;
    }
}