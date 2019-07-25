package com.flybd.installer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.CampaignTrackingReceiver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * 监听并保存安装referrer
 * 测试方法：
 * 1 进到adb shell
 * 2 打开GAv4的log：setprop log.tag.GAv4 VERBOSE
 * 3 发送广播通知：
 * am broadcast -a com.android.vending.INSTALL_REFERRER -n com.mango.cash/com.daunkredit.program.sulu.broadcast.InstallReferrerReceiver --es  "referrer" "utm_source=testSource&utm_medium=testMedium&utm_term=testTerm&utm_content=11&PARTNER_ID=111&PARTNER_CLICK_ID=222"
 */

public class InstallReferrerReceiver extends CampaignTrackingReceiver {

    public static String install_referrer_store_key = "GA_install_referrer_store_key";
    public static String install_referrer_from_ga_sdk_store_key = "install_referrer_from_ga_sdk_store_key";
    public static final String PREF_REFERRER = "referrer";

    @SuppressLint("ApplySharedPref")
    public void onReceive(Context context, Intent data) {
        super.onReceive(context, data);

        String referrerValue = getReferrerValue(data.getExtras());

        try {
            referrerValue = URLDecoder.decode(referrerValue, "utf-8");
            referrerValue = URLDecoder.decode(referrerValue, "utf-8");
            referrerValue = URLDecoder.decode(referrerValue, "utf-8");
            referrerValue = URLEncoder.encode(referrerValue, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.i("InstallReferrerReceiver", "onReceive referrer " + referrerValue);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(install_referrer_store_key, referrerValue).apply();

        SharedPreferences pref = context.getSharedPreferences(InstallReferrerReceiver.PREF_REFERRER, Context.MODE_MULTI_PROCESS);
        pref.edit().putString(install_referrer_store_key, referrerValue).commit();
        HostPluginCache cache = new HostPluginCache(context);
        cache.saveCache("referrer", referrerValue.getBytes(), false);
    }

    private String getReferrerValue(Bundle bundle) {
        String referrerValue = "";
        try {
            if (bundle != null) {
                referrerValue = bundle.getString("referrer");
            }
            if (referrerValue == null) {
                referrerValue = "";
            }

            if (TextUtils.isEmpty(referrerValue)) {
            } else {
            }

        } catch (Exception e) {
        }
        return referrerValue;
    }
}
