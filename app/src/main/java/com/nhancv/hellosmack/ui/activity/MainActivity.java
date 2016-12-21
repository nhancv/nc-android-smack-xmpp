package com.nhancv.hellosmack.ui.activity;

import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.nhancv.hellosmack.R;
import com.nhancv.hellosmack.bus.InvitationBus;
import com.nhancv.hellosmack.bus.MessageBus;
import com.nhancv.hellosmack.bus.RosterBus;
import com.nhancv.hellosmack.bus.XmppConnBus;
import com.nhancv.hellosmack.helper.NUtil;
import com.nhancv.hellosmack.helper.XmppService;
import com.nhancv.hellosmack.helper.XmppService_;
import com.nhancv.hellosmack.ui.fragment.GroupFragment;
import com.nhancv.hellosmack.ui.fragment.GroupFragment_;
import com.nhancv.hellosmack.ui.fragment.UsersFragment;
import com.nhancv.hellosmack.ui.fragment.UsersFragment_;
import com.nhancv.npreferences.NPreferences;
import com.nhancv.xmpp.XmppPresenter;
import com.nhancv.xmpp.model.BaseMessage;
import com.nhancv.xmpp.model.BaseRoster;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.Subscribe;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.nhancv.hellosmack.R.id.logout;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    @ViewById(R.id.vToolbar)
    Toolbar vToolbar;
    @ViewById(R.id.vTabs)
    TabLayout vTabs;
    @ViewById(R.id.vViewPager)
    ViewPager vViewPager;
    @ViewById(R.id.vDrawer)
    DrawerLayout vDrawer;
    @ViewById(R.id.vNavigation)
    NavigationView vNavigation;
    @ViewById(R.id.tvName)
    TextView tvName;

    ViewPagerAdapter adapter;
    UsersFragment usersFragment = new UsersFragment_();
    GroupFragment groupFragment = new GroupFragment_();

    @AfterViews
    void initView() {
        setupToolbar(vToolbar, "Main activity");
        setupViewPager(vViewPager);
        vTabs.setupWithViewPager(vViewPager);
        initNavigationDrawer();
    }

    @Subscribe
    public void invitationSubscribe(InvitationBus invitationBus) {
        Log.e(TAG, "invitationSubscribe: " + invitationBus.getData());
    }

    @Subscribe
    public void xmppConnSubscribe(XmppConnBus xmppConnBus) {
        Log.e(TAG, "xmppConnSubscribe: " + xmppConnBus.getType());
        switch (xmppConnBus.getType()) {
            case CLOSE_ERROR:
                NUtil.showToast(this, ((Exception) xmppConnBus.getData()).getMessage());
                logout();
                break;
            default:
                NUtil.showToast(this, xmppConnBus.getType().name());
                break;

        }
    }

    @Subscribe
    public void messageSubscribe(MessageBus messageBus) {
        BaseMessage baseMessage = (BaseMessage) messageBus.getData();
        if (baseMessage != null) {
            Log.e(TAG, "messageSubscribe: " + baseMessage);
        }
        usersFragment.updateAdapterList();
    }

    @Subscribe
    public void rosterSubscribe(RosterBus rosterBus) {
        BaseRoster baseRoster = ((BaseRoster) rosterBus.getData());
        String status = (baseRoster != null ? baseRoster.getName() + " -> " + baseRoster.getPresence().getType() : null);
        if (status != null) {
            Log.e(TAG, "rosterSubscribe: " + status);
        }
        usersFragment.updateAdapterList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        XmppService.getBus().register(this);
        if (XmppPresenter.getInstance().isConnected()) {
            usersFragment.updateAdapterList();
        } else {
            logout();
        }
    }

    @Override
    protected void onPause() {
        XmppService.getBus().unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        XmppService_.intent(getApplication()).stop();
        super.onDestroy();
    }

    private void setupToolbar(Toolbar toolbar, String title) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    public void initNavigationDrawer() {
        vNavigation.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            switch (id) {
                case logout:
                    vDrawer.closeDrawers();
                    logout();
                    break;
            }
            return true;
        });
        View header = vNavigation.getHeaderView(0);
        TextView tvName = (TextView) header.findViewById(R.id.tvName);
        tvName.setText(XmppStringUtils.parseBareJid(XmppPresenter.getInstance().getCurrentUser()));

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, vDrawer, vToolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        vDrawer.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private void logout() {
        NUtil.aSyncTask(subscriber -> {
            //Clear preference
            NPreferences.getInstance().edit().clear();
            //Terminal current connection
            XmppPresenter.getInstance().logout();
            //Stop service
            XmppService_.intent(getApplication()).stop();
            //Transmit to login screen
            LoginActivity_.intent(MainActivity.this).start();
            finish();
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(usersFragment, "Users");
        adapter.addFragment(groupFragment, "Group");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

}
