package com.galaxy.android.main;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.common.componentes.activity.ActionBarFragmentActivity;
import com.common.componentes.activity.ImmersiveFragmentActivity;
import com.common.utils.activity.ActivityUtil;
import com.galaxy.android.apklist.ApkListFragment;
import com.galaxy.android.fragment.AppListFragment;
import com.galaxy.android.qr.QRActivity;
import com.galaxy.asia.R;
import com.galaxy.asia.bean.ApkInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends ImmersiveFragmentActivity implements MainContract.View {

    private static final String KEY_LIST_FRAGMENT_SIZE = "key_list_fragment_size";

    private static final String KEY_FRAGMENT_TITLE = "key_fragment_title";

    private static final long CLOSE_TIME_INTERVAL = 3000;

    private ViewPager viewPager;

    private Bundle bundle = new Bundle();

    private long lastBackPressTime = -1;

    private List<String> titles = new ArrayList<>();

    private HashMap<String, List<ApkInfo>> map = new HashMap<>();

    private MainPresenter mainPresenter;

    private NavigationView navigationView;

    private SwipeRefreshLayout refreshLayout;

    private Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainPresenter = new MainPresenter();
        bindView();
        inflaterMenu();
        bindListener();
        Intent intent = new Intent(this, QRActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainPresenter.takeView(this);
        requestGooglePlay();
    }

    private void requestGooglePlay() {
        new Thread(() -> mainPresenter.loadGooglePlay(MainActivity.this, titles, map)).start();
        refreshLayout.setRefreshing(true);
    }

    private void bindView() {
        View content = findViewById(R.id.content);
        content.setPadding(content.getPaddingLeft(), content.getPaddingTop() + getStatusBarHeight(), content.getPaddingRight(), content.getPaddingBottom());

        toolbar = findViewById(R.id.tool_bar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerToggle.syncState();
        drawerLayout.setDrawerListener(drawerToggle);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        navigationView = findViewById(R.id.nv_menu_left);

        refreshLayout = findViewById(R.id.refresh_layout);


    }

    private void bindListener() {
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_messages) {
                Intent intent = ActionBarFragmentActivity.newInstance(MainActivity.this, ApkListFragment.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        refreshLayout.setOnRefreshListener(() -> refreshLayout.setRefreshing(false));
    }

    private void inflaterMenu() {
        Menu menu = toolbar.getMenu();
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView == null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView == null) {
            return;
        }
        searchView.setQueryHint("package or applicationId");

        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getResources().getColor(android.R.color.background_light));
        searchAutoComplete.setTextColor(getResources().getColor(android.R.color.background_light));
        final SearchView finalSearchView = searchView;
        //配置searchView...
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                finalSearchView.setQuery("", false);
                finalSearchView.clearFocus(); // 可以收起键盘
                // searchView.onActionViewCollapsed(); // 可以收起SearchView视图
                if (!TextUtils.isEmpty(query)) {
                    try {
                        ActivityUtil.jumpToMarket(MainActivity.this, query, "com.android.vending");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }


    @Override
    public void onGooglePlayLoaded() {
        runOnUiThread(() -> {
            refreshLayout.setRefreshing(false);
            int size = viewPager.getAdapter().getCount();
            for (int i = 0; i < size; i++) {
                AppListFragment fragment = new AppListFragment();
                fragment.setApkInfoList(map.get(titles.get(i)));
                ((FragmentAdapter) viewPager.getAdapter()).fragments.add(fragment);
            }
            viewPager.getAdapter().notifyDataSetChanged();
        });
    }

    private class FragmentAdapter extends FragmentPagerAdapter {

        List<AppListFragment> fragments = new ArrayList<>();

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.i("FragmentAdapter", "getItem position $position id " + fragments.get(position).toString());
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return titles.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object ret = super.instantiateItem(container, position);
            Log.i("FragmentAdapter", "instantiateItem position " + position);
            return ret;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    @Override
    public void onBackPressed() {
        if (lastBackPressTime < 0) {
            lastBackPressTime = System.currentTimeMillis();
            Toast.makeText(this, "Double click to exit app", Toast.LENGTH_SHORT).show();
            return;
        }

        if (System.currentTimeMillis() - lastBackPressTime < CLOSE_TIME_INTERVAL) {
            super.onBackPressed();
        } else {
            lastBackPressTime = -1;
        }
    }
}

