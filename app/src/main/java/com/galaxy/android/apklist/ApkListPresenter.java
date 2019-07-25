package com.galaxy.android.apklist;

import android.content.Context;

import com.common.utils.file.FileUtil;

import java.io.File;
import java.util.List;

public class ApkListPresenter implements ApkListContract.Presenter {

    private ApkListContract.View view;

    @Override
    public void loadStorageApk(Context context) {
        if (context != null) {
            List<File> apkList = FileUtil.INSTANCE.getAllApkFile(context, null);
            if (view != null) {
                view.onStorageApkLoaded(apkList);
            }
        }
    }

    @Override
    public void takeView(ApkListContract.View view) {
        this.view = view;
    }

    @Override
    public void dropView() {
        this.view = null;
    }
}
