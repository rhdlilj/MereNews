package androidnews.kiloproject.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.WindowManager;

import com.blankj.utilcode.util.SPUtils;

import androidnews.kiloproject.system.AppConfig;

import static androidnews.kiloproject.system.AppConfig.CONFIG_NIGHT_MODE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_SWIPE_BACK;
import static androidnews.kiloproject.system.AppConfig.isNightMode;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SPUtils spUtils = SPUtils.getInstance();
        AppConfig.isSwipeBack = spUtils.getBoolean(CONFIG_SWIPE_BACK);
        AppConfig.isNightMode = spUtils.getBoolean(CONFIG_NIGHT_MODE);
        if (isNightMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
