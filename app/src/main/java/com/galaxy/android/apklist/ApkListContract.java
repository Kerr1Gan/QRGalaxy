package com.galaxy.android.apklist;

import android.content.Context;

import com.common.mvp.IPresenter;
import com.common.mvp.IView;

import java.io.File;
import java.util.List;

public class ApkListContract {

    public interface View extends IView<ApkListContract.Presenter> {

        void onStorageApkLoaded(List<File> apkList);
    }

    public interface Presenter extends IPresenter<ApkListContract.View> {
        void loadStorageApk(Context context);
    }
}
