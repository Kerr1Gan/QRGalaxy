package com.galaxy.android.main;

import android.content.Context;

import com.common.mvp.IPresenter;
import com.common.mvp.IView;
import com.galaxy.asia.bean.ApkInfo;

import java.util.HashMap;
import java.util.List;

public class MainContract {

    public interface View extends IView<Presenter> {
        void onGooglePlayLoaded();
    }

    public interface Presenter extends IPresenter<View> {
        void loadGooglePlay(Context context, List<String> titles, HashMap<String, List<ApkInfo>> map);
    }
}
