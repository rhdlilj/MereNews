package androidnews.kiloproject.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.blankj.utilcode.util.CacheDiskUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.bumptech.glide.Glide;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;
import com.jude.swipbackhelper.SwipeBackHelper;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import androidnews.kiloproject.R;
import androidnews.kiloproject.bean.data.TypeArrayBean;
import androidnews.kiloproject.bean.net.PhotoCenterData;
import androidnews.kiloproject.fragment.GuoKrRvFragment;
import androidnews.kiloproject.fragment.ZhihuRvFragment;
import androidnews.kiloproject.fragment.MainRvFragment;
import androidnews.kiloproject.receiver.MessageEvent;
import androidnews.kiloproject.system.base.BaseActivity;
import butterknife.BindView;
import butterknife.ButterKnife;

import static androidnews.kiloproject.activity.ChannelActivity.SELECT_RESULT;
import static androidnews.kiloproject.activity.SettingActivity.SETTING_RESULT;
import static androidnews.kiloproject.bean.data.CacheNews.CACHE_COLLECTION;
import static androidnews.kiloproject.bean.data.CacheNews.CACHE_HISTORY;
import static androidnews.kiloproject.system.AppConfig.CONFIG_BACK_EXIT;
import static androidnews.kiloproject.system.AppConfig.CONFIG_NIGHT_MODE;
import static androidnews.kiloproject.system.AppConfig.CONFIG_RANDOM_HEADER;
import static androidnews.kiloproject.system.AppConfig.CONFIG_TYPE_ARRAY;
import static androidnews.kiloproject.system.AppConfig.isNightMode;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.materialViewPager)
    MaterialViewPager mViewPager;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    ActionBarDrawerToggle mDrawerToggle;
    @BindView(R.id.navigation)
    NavigationView navigation;

    PhotoCenterData photoData;
    int bgPosition = 0;
    TypeArrayBean typeArrayBean;

    public static final int DEFAULT_PAGE = 4;

    public static final int TYPE_ZHIHU = 38;
    public static final int TYPE_GUOKR = 39;
    public static final int TYPE_V_HOT = 40;
    public static final int TYPE_V_ENTERTAINMENT = 41;
    public static final int TYPE_V_FUNNY = 42;
    public static final int TYPE_V_EXCELLENT = 43;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 0, 0);
        drawerLayout.addDrawerListener(mDrawerToggle);
        final Toolbar toolbar = mViewPager.getToolbar();
        initToolbar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_add_lib:
                        startActivityForResult(new Intent(mActivity, ChannelActivity.class), SELECT_RESULT);
                        break;
                }
                return false;
            }
        });
        SwipeBackHelper.getCurrentPage(this).setSwipeBackEnable(false);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void initSlowly() {

        typeArrayBean = CacheDiskUtils.getInstance().getParcelable(CONFIG_TYPE_ARRAY, TypeArrayBean.CREATOR);
        if (typeArrayBean == null) {
            typeArrayBean = new TypeArrayBean();
            typeArrayBean.setTypeArray(new ArrayList<>());
            for (int i = 0; i < DEFAULT_PAGE; i++) {
                typeArrayBean.getTypeArray().add(i);
            }
            CacheDiskUtils.getInstance().put(CONFIG_TYPE_ARRAY, typeArrayBean);
        }

        navigation.setNavigationItemSelectedListener(this);

        ColorStateList csl = getBaseContext().getResources().getColorStateList(R.color.navigation_menu_item_color);
        navigation.setItemTextColor(csl);

        mViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                int type = typeArrayBean.getTypeArray().get(position);
                if (type >= TYPE_ZHIHU)
                    switch (type) {
                        case TYPE_ZHIHU:
                            return new ZhihuRvFragment();
                        case TYPE_GUOKR:
                            return new GuoKrRvFragment();
                    }
                return MainRvFragment.newInstance(typeArrayBean.getTypeArray().get(position));
            }

            @Override
            public int getCount() {
                return typeArrayBean.getTypeArray().size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                String[] tags = getResources().getStringArray(R.array.address_tag);
                return tags[typeArrayBean.getTypeArray().get(position)];
            }
        });

        mViewPager.getViewPager().setOffscreenPageLimit(mViewPager.getViewPager().getAdapter().getCount());
        mViewPager.getPagerTitleStrip().setViewPager(mViewPager.getViewPager());

//        final View logo = findViewById(R.id.logo_white);
//        if (logo != null) {
//            logo.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mViewPager.notifyHeaderChanged();
//                    Toast.makeText(getApplicationContext(), "Yes, the title is clickable", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }

        String dataUrl = "http://pic.news.163.com/photocenter/api/list/0001/00AN0001,00AO0001,00AP0001/0/10/cacheMoreData.json";
        EasyHttp.get(dataUrl)
                .readTimeOut(30 * 1000)//局部定义读超时
                .writeTimeOut(30 * 1000)
                .connectTimeout(30 * 1000)
                .timeStamp(true)
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onSuccess(String s) {
                        String temp = s.replace(")", "}");
                        String response = temp.replace("cacheMoreData(", "{\"cacheMoreData\":");
                        if (!TextUtils.isEmpty(response) || TextUtils.equals(response, "{}")) {
                            try {
                                photoData = gson.fromJson(response, PhotoCenterData.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                                SnackbarUtils.with(mViewPager).setMessage(getString(R.string.server_fail)).showError();
                            }
                            startBgAnimate();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Glide.with(mActivity).resumeRequests();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_items, menu);//加载menu布局
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.nav_his:
                intent = new Intent(mActivity, CacheActivity.class);
                intent.putExtra("type", CACHE_HISTORY);
                startActivity(intent);
                break;
            case R.id.nav_coll:
                intent = new Intent(mActivity, CacheActivity.class);
                intent.putExtra("type", CACHE_COLLECTION);
                startActivity(intent);
                break;
            case R.id.nav_setting:
                intent = new Intent(mActivity, SettingActivity.class);
                startActivityForResult(intent, SETTING_RESULT);
                break;
            case R.id.nav_about:
                intent = new Intent(mActivity, AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_theme:
                if (isNightMode) {
                    isNightMode = false;
                } else {
                    isNightMode = true;
                }
                SPUtils.getInstance().put(CONFIG_NIGHT_MODE, isNightMode);
                recreate();
                break;

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    private long firstTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (SPUtils.getInstance().getBoolean(CONFIG_BACK_EXIT)
                    && System.currentTimeMillis() - firstTime > 2000) {
                SnackbarUtils.with(mViewPager).setMessage(getString(R.string.click_to_exit)).show();
                firstTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_RESULT:
            case SETTING_RESULT:
                if (resultCode == RESULT_OK) {
                    initSlowly();
                }
                break;
        }
    }

    Timer timer;
    TimerTask timerTask;

    private void startBgAnimate() {
        if (SPUtils.getInstance().getBoolean(CONFIG_RANDOM_HEADER)) {
            mViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
                @Override
                public HeaderDesign getHeaderDesign(int page) {
                    int postion = page % photoData.getCacheMoreData().size();

                    return HeaderDesign.fromColorResAndUrl(
                            R.color.deepskyblue,
                            photoData.getCacheMoreData().get(postion).getCover());
                }
                //execute others actions if needed (ex : modify your header logo)
            });
            mViewPager.setImageUrl(photoData.getCacheMoreData().get(0).getCover(), 200);
        } else {
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mViewPager != null && photoData != null) {
                                if (bgPosition == photoData.getCacheMoreData().size()) {
                                    bgPosition = 0;
                                }
                                try {
                                    mViewPager.setImageUrl(photoData.getCacheMoreData().get(bgPosition).getCover(), 300);
                                    bgPosition++;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            };
            timer.schedule(timerTask, 0, 10 * 1000);
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {/* Do something */}

    @Override
    protected void onPause() {
        super.onPause();
        Glide.with(mActivity).pauseRequests();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!SPUtils.getInstance().getBoolean(CONFIG_RANDOM_HEADER)
                && timer != null
                && timerTask != null)
            cancelTimer();
        EventBus.getDefault().unregister(this);
    }
}
