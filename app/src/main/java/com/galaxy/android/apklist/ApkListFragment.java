package com.galaxy.android.apklist;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.common.componentes.fragment.LazyInitFragment;
import com.common.utils.file.FileUtil;
import com.flybd.installer.RequestInstallActivity;
import com.galaxy.android.async.AppThumbTask;
import com.galaxy.asia.R;

import java.io.File;
import java.util.List;

public class ApkListFragment extends LazyInitFragment implements ApkListContract.View {

    private static final int CACHE_SIZE = 5 * 1024 * 1024;

    private static LruCache<String, Bitmap> sLruCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private RecyclerView recyclerView;

    private List<File> apkList;

    private static final String permission[] = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int REQUEST_CODE = 110;

    private ApkListContract.Presenter presenter = new ApkListPresenter();

    private SwipeRefreshLayout refreshLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new SimpleAdapter());

        refreshLayout = view.findViewById(R.id.refresh_layout);

        presenter.takeView(this);
        Activity act = getActivity();
        if (act != null) {
            boolean hasPermission = true;
            for (String pmi : permission) {
                if (ActivityCompat.checkSelfPermission(act, pmi) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(act, permission, REQUEST_CODE);
                    hasPermission = false;
                    break;
                }
            }

            if (hasPermission) {
                new Thread(() -> presenter.loadStorageApk(getContext())).start();
                refreshLayout.setRefreshing(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermission = true;
        for (int grant : grantResults) {
            if (grant == PackageManager.PERMISSION_DENIED) {
                hasPermission = false;
            }
        }
        if (hasPermission) {
            // grant permission
            new Thread(() -> presenter.loadStorageApk(getContext())).start();
            refreshLayout.setRefreshing(true);
        }
    }

    @Override
    public void onStorageApkLoaded(List<File> apkList) {
        this.apkList = apkList;

        getHandler().post(() -> {
            refreshLayout.setRefreshing(false);
            if (recyclerView.getAdapter() != null) {
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.takeView(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.takeView(this);
    }

    private class Holder extends RecyclerView.ViewHolder {

        TextView tvName;

        ImageView ivLogo;

        Button btnInstall;

        public Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            ivLogo = itemView.findViewById(R.id.iv_app_logo);
            btnInstall = itemView.findViewById(R.id.btn_install);
        }
    }

    private class SimpleAdapter extends RecyclerView.Adapter<Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View item = getLayoutInflater().inflate(R.layout.layout_app_list_item, viewGroup, false);
            return new Holder(item);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            Bitmap b = sLruCache.get(apkList.get(i).getAbsolutePath());
            Context ctx = getContext();
            String apkPath = apkList.get(i).getAbsolutePath();
            if (b == null && ctx != null) {
                AppThumbTask task = new AppThumbTask(sLruCache, ctx, holder.ivLogo);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new File(apkPath));
            } else {
                holder.ivLogo.setImageBitmap(b);
            }
            if (ctx != null) {
                String name = FileUtil.INSTANCE.getApkInfo(ctx, apkList.get(i).getAbsolutePath());
                holder.tvName.setText(name);
            }
            View.OnClickListener listener = v -> {
                Intent intent = RequestInstallActivity.getIntent(v.getContext(), apkPath);
                startActivity(intent);
            };
            holder.itemView.setOnClickListener(listener);
            holder.btnInstall.setOnClickListener(listener);
        }

        @Override
        public int getItemCount() {
            return apkList == null ? 0 : apkList.size();
        }
    }
}
