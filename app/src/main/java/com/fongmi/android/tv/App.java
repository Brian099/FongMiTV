package com.fongmi.android.tv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;

import com.google.gson.Gson;
import com.fongmi.android.tv.event.EventIndex;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.ui.activity.CrashActivity;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.hook.Hook;

import com.github.catvod.Init;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Prefers;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.LogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import org.greenrobot.eventbus.EventBus;

import cat.ereza.customactivityoncrash.config.CaocConfig;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private final ExecutorService searchExecutor;
    private final ExecutorService executor;
    private final Handler handler;
    private static App instance;
    private Activity activity;
    private final Gson gson;
    private final long time;
    private Hook hook;

    public App() {
        instance = this;
        gson = new Gson();
        time = System.currentTimeMillis();
        executor = Executors.newFixedThreadPool(5);
        searchExecutor = Executors.newFixedThreadPool(20);
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    public void setHook(Hook hook) {
        this.hook = hook;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Init.set(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 新增：每次启动都清除所有数据 by brian
        clearAllApplicationData();
        Notify.createChannel();
        Logger.addLogAdapter(getLogAdapter());
        OkHttp.get().setDoh(Doh.objectFrom(Setting.getDoh()));
        EventBus.builder().addIndex(new EventIndex()).installDefaultEventBus();
        CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();
		
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public PackageManager getPackageManager() {
        return hook != null ? hook : getBaseContext().getPackageManager();
    }

    @Override
    public String getPackageName() {
        return hook != null ? hook.getPackageName() : getBaseContext().getPackageName();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity != activity()) this.activity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity == activity()) this.activity = null;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    public static App get() {
        return instance;
    }

    public static Gson gson() {
        return get().gson;
    }

    public static long time() {
        return get().time;
    }

    public static Activity activity() {
        return get().activity;
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return get().executor.submit(task);
    }

    public static Future<?> submit(Runnable task) {
        return get().executor.submit(task);
    }

    public static Future<?> submitSearch(Runnable task) {
        return get().searchExecutor.submit(task);
    }

    public static void execute(Runnable runnable) {
        get().executor.execute(runnable);
    }

    public static void post(Runnable runnable) {
        get().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        get().handler.removeCallbacks(runnable);
        if (delayMillis >= 0) get().handler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        get().handler.removeCallbacks(runnable);
    }

    public static void removeCallbacks(Runnable... runnable) {
        for (Runnable r : runnable) get().handler.removeCallbacks(r);
    }

    /**
     * 清除所有应用数据，恢复到初始状态
     */
    private void clearAllApplicationData() {
		try {
			// 1. 清空数据库所有配置
			AppDatabase.get().getConfigDao().delete("0"); // 点播
			AppDatabase.get().getConfigDao().delete("1"); // 直播
			AppDatabase.get().getConfigDao().delete("2"); // 壁纸

			// 2. 清空SharedPreferences中的配置标记
			Prefers.remove("config_0");
			Prefers.remove("config_1");
			Prefers.remove("config_2");

			// 3. 清空内存配置缓存
			VodConfig.get().clear();
			LiveConfig.get().clear();
			// WallConfig.get().clear(); // 如果 WallConfig 没有 clear 方法，可以注释掉
			OkHttp.get().clear();

			// 4. 清除壁纸缓存文件
			clearWallpaperCache();

		} catch (Exception e) {
			Logger.e("Clear application data error: " + e.getMessage());
		}
	}
    
    /**
     * 清除壁纸缓存
     */
    private void clearWallpaperCache() {
        try {
            File wallDir = new File(getCacheDir(), "wall");
            if (wallDir.exists() && wallDir.isDirectory()) {
                deleteDirectory(wallDir);
            }
        } catch (Exception e) {
            Logger.e("Clear wallpaper cache error: " + e.getMessage());
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    private LogAdapter getLogAdapter() {
        return new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().methodCount(0).showThreadInfo(false).tag("").build()) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return true;
            }
        };
    }
}