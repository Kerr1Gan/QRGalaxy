package com.galaxy.android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.common.componentes.fragment.LazyInitFragment;
import com.common.utils.activity.ActivityUtil;
import com.galaxy.asia.R;
import com.galaxy.asia.bean.ApkInfo;

import java.util.List;

public class AppListFragment extends LazyInitFragment {

    private RecyclerView recyclerView;

    private List<ApkInfo> apkInfoList;

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
        refreshLayout.setOnRefreshListener(() -> refreshLayout.setRefreshing(false));
    }

    public void setApkInfoList(List<ApkInfo> apkInfoList) {
        this.apkInfoList = apkInfoList;
    }

    public SwipeRefreshLayout getRefreshLayout() {
        return refreshLayout;
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
            ApkInfo apkInfo = apkInfoList.get(i);
            holder.tvName.setText(apkInfo.getName());
            Context ctx = getContext();
            if (ctx != null) {
                Glide.with(ctx).load(apkInfoList.get(i).getLogo()).into(holder.ivLogo);
            }
            holder.itemView.setOnClickListener(v -> {
                try {
                    ActivityUtil.jumpToMarket(getContext(), apkInfo.getUrl(), "com.android.vending");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            holder.btnInstall.setOnClickListener(v -> {
                try {
                    ActivityUtil.jumpToMarket(getContext(), apkInfo.getUrl(), "com.android.vending");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public int getItemCount() {
            return apkInfoList.size();
        }
    }
}
