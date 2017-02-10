package com.houkcorp.margatsniym.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

import com.houkcorp.margatsniym.R;
import com.houkcorp.margatsniym.fragments.InstagramDetailFragment;
import com.houkcorp.margatsniym.models.Media;

public class InstagramDetailActivity extends AppCompatActivity {
    public static final String MEDIA_EXTRA = "MEDIA_EXTRA";

    public static Intent newIntent(Context context, Media media) {
        Intent intent = new Intent(context, InstagramDetailActivity.class);
        intent.putExtra(MEDIA_EXTRA, media);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_instagram_detail);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Media media = null;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            media = bundle.getParcelable(MEDIA_EXTRA);
        }

        InstagramDetailFragment instagramDetailFragment = InstagramDetailFragment.newInstance(media);

    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }
}