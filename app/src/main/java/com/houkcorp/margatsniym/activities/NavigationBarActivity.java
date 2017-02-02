package com.houkcorp.margatsniym.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.houkcorp.margatsniym.R;
import com.houkcorp.margatsniym.fragments.FollowedUserImageList;
import com.houkcorp.margatsniym.fragments.MyUserFragment;

public class NavigationBarActivity extends AppCompatActivity {
    private static final String SELECTED_MENU_ITEM = "SELECTED_MENU_ITEM_ID";
    private static final String MY_USER_FRAGMENT = "MY_USER_FRAGMENT";
    private static final String FOLLOWING_FRAGMENT = "FOLLOWING_FRAGMENT";

    private int mSelectedMenuItemId;
    private BottomNavigationView mNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_navigation_bar);

        mNavigationView = (BottomNavigationView) findViewById(R.id.bottom_nav_bar);
        mNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectMenuItem(item);

                return false;
            }
        });

        if (savedInstanceState != null) {
            selectMenuItem(mNavigationView.getMenu().findItem(savedInstanceState.getInt(SELECTED_MENU_ITEM, 0)));
        } else {
            selectMenuItem(mNavigationView.getMenu().getItem(0));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_MENU_ITEM, mSelectedMenuItemId);
        super.onSaveInstanceState(outState);
    }

    public void selectMenuItem(MenuItem menuItem) {
        Fragment fragment = null;
        String fragmentTag = null;


        switch (menuItem.getItemId()) {
            case R.id.my_user:
                fragment = getSupportFragmentManager().findFragmentByTag(MY_USER_FRAGMENT);
                fragmentTag = MY_USER_FRAGMENT;

                if (fragment == null) {
                    fragment = MyUserFragment.newInstance();
                }

                setActivityTitle(getString(R.string.my_user));

                break;

            case R.id.following:
                fragment = getSupportFragmentManager().findFragmentByTag(FOLLOWING_FRAGMENT);
                fragmentTag = FOLLOWING_FRAGMENT;

                if (fragment == null) {
                    fragment = FollowedUserImageList.newInstance();
                }

                setActivityTitle(getString(R.string.following));

                break;
        }

        mSelectedMenuItemId = menuItem.getItemId();

        menuItem.setChecked(true);

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.navigation_bar_frame_layout, fragment, fragmentTag);
            ft.commit();
        }
    }

    private void setActivityTitle(CharSequence text) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(text);
        }
    }
}