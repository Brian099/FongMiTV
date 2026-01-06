package com.fongmi.android.tv.utils;

public class Github {

    // public static final String URL = "https://raw.githubusercontent.com/FongMi/Release/fongmi";
	public static final String URL = "https://tvboxadmin.com:2026";

    private static String getUrl(String name) {
        return URL + "/apk/" + name;
    }

    public static String getJson(String name) {
        return getUrl(name + ".json");
    }

    public static String getApk(String name) {
        return getUrl(name + ".apk");
    }
}
