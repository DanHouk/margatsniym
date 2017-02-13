package com.houkcorp.margatsniym.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.houkcorp.margatsniym.R;
import com.houkcorp.margatsniym.activities.InstagramDetailActivity;
import com.houkcorp.margatsniym.events.ExpiredOAuthEvent;
import com.houkcorp.margatsniym.events.MediaChangedEvent;
import com.houkcorp.margatsniym.models.Media;
import com.houkcorp.margatsniym.models.MediaResponse;
import com.houkcorp.margatsniym.services.ServiceFactory;
import com.houkcorp.margatsniym.services.UserService;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InstagramDetailFragment extends Fragment {
    private Media mMedia;
    private String mAccessToken;

    @BindView(R.id.image_detail_scroll_view) ScrollView mDetailScrollView;
    @BindView(R.id.image_detail_image_view) ImageView mDetailImageView;
    @BindView(R.id.image_detail_video_view) VideoView mDetailVideoView;
    @BindView(R.id.image_detail_progress_bar) ProgressBar mDetailProgressBar;
    @BindView(R.id.image_detail_liked_image_view) ImageView mDetailLikedImageView;
    @BindView(R.id.image_detail_unliked_image_view) ImageView mDetailUnLikedImageView;

    public static InstagramDetailFragment newInstance(Media media, String accessToken) {
        Bundle extras = new Bundle();
        extras.putParcelable(InstagramDetailActivity.MEDIA_EXTRA, media);
        extras.putString(InstagramDetailActivity.ACCESS_TOKEN_EXTRA, accessToken);

        InstagramDetailFragment instagramDetailFragment = new InstagramDetailFragment();
        instagramDetailFragment.setArguments(extras);

        return instagramDetailFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_instagram_detail, container, false);

        ButterKnife.bind(this, root);

        Bundle extras = getArguments();
        if (extras != null) {
            mMedia = extras.getParcelable(InstagramDetailActivity.MEDIA_EXTRA);
            mAccessToken = extras.getString(InstagramDetailActivity.ACCESS_TOKEN_EXTRA);
        }

        if (!TextUtils.isEmpty(mMedia.getCaption().getText())) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(mMedia.getCaption().getText());
        }

        if (mMedia.getType().equals("image")) {
            Picasso
                    .with(getContext())
                    .load(mMedia.getImages().getStandardResolution().getUrl())
                    .into(mDetailImageView);
            mDetailImageView.setVisibility(View.VISIBLE);
            mDetailVideoView.setVisibility(View.GONE);
        } else if (mMedia.getType().equals("video")) {
            MediaController mediaController = new MediaController(getContext());
            mediaController.setAnchorView(mDetailVideoView);
            mDetailVideoView.setMediaController(mediaController);
            mDetailVideoView.setVideoURI(Uri.parse(mMedia.getVideos().getLowResolution().getUrl()));
            mDetailVideoView.start();

            mDetailImageView.setVisibility(View.GONE);
            mDetailVideoView.setVisibility(View.VISIBLE);
        }

        if (mMedia.isHasLiked()) {
            mDetailLikedImageView.setVisibility(View.VISIBLE);
            mDetailUnLikedImageView.setVisibility(View.GONE);
        } else {
            mDetailLikedImageView.setVisibility(View.GONE);
            mDetailUnLikedImageView.setVisibility(View.VISIBLE);
        }

        mDetailLikedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unLikeMedia();
            }
        });

        mDetailUnLikedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeMedia();
            }
        });

        mDetailProgressBar.setVisibility(View.GONE);
        mDetailScrollView.setVisibility(View.VISIBLE);

        return root;
    }

    private void likeMedia() {
        UserService service = ServiceFactory.getInstagramUserService();
        service.likeMedia(mMedia.getId(), mAccessToken, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(20)
                .subscribe(new Subscriber<Response<MediaResponse>>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("This completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("There was an error: ");
                        System.out.println("There was an error: " + e.toString());
                    }

                    @Override
                    public void onNext(Response<MediaResponse> response) {
                        ResponseBody errorBody = response.errorBody();
                        if (!response.isSuccessful() && errorBody != null) {
                            try {
                                Gson gson = new GsonBuilder().create();
                                MediaResponse instagramErrorResponse = gson.fromJson(errorBody.string(), MediaResponse.class);
                                Toast.makeText(getContext(), instagramErrorResponse.getMeta().getErrorMessage(), Toast.LENGTH_LONG).show();

                                if (instagramErrorResponse.getMeta().getErrorType().equals("OAuthAccessTokenException")) {
                                    EventBus.getDefault().post(new ExpiredOAuthEvent(true));
                                }
                            } catch (IOException e) {
                                Log.e("MyUserFragment", "There was a problem parsing the error response.");
                            }
                        } else {
                            if (response.body().getMeta().getCode() == 200) {
                                Toast.makeText(getContext(), getString(R.string.media_success, "liked"), Toast.LENGTH_SHORT).show();
                                mMedia.setHasLiked(true);
                                mDetailLikedImageView.setVisibility(View.VISIBLE);
                                mDetailUnLikedImageView.setVisibility(View.GONE);
                                EventBus.getDefault().post(new MediaChangedEvent(mMedia));
                            } else {
                                Toast.makeText(getContext(), getString(R.string.server_failure, "Like Media"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void unLikeMedia() {
        UserService service = ServiceFactory.getInstagramUserService();
        service.unLikeMedia(mMedia.getId(), mAccessToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(20)
                .subscribe(new Subscriber<Response<MediaResponse>>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("This completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("There was an error: ");
                        System.out.println("There was an error: " + e.toString());
                    }

                    @Override
                    public void onNext(Response<MediaResponse> response) {
                        ResponseBody errorBody = response.errorBody();
                        if (!response.isSuccessful() && errorBody != null) {
                            try {
                                Gson gson = new GsonBuilder().create();
                                MediaResponse instagramErrorResponse = gson.fromJson(errorBody.string(), MediaResponse.class);
                                Toast.makeText(getContext(), instagramErrorResponse.getMeta().getErrorMessage(), Toast.LENGTH_LONG).show();

                                if (instagramErrorResponse.getMeta().getErrorType().equals("OAuthAccessTokenException")) {
                                    EventBus.getDefault().post(new ExpiredOAuthEvent(true));
                                }
                            } catch (IOException e) {
                                Log.e("MyUserFragment", "There was a problem parsing the error response.");
                            }
                        } else {
                            if (response.body().getMeta().getCode() == 200) {
                                Toast.makeText(getContext(), getString(R.string.media_success, "UnLiked"), Toast.LENGTH_SHORT).show();
                                mMedia.setHasLiked(false);
                                mDetailLikedImageView.setVisibility(View.GONE);
                                mDetailUnLikedImageView.setVisibility(View.VISIBLE);
                                EventBus.getDefault().post(new MediaChangedEvent(mMedia));
                            } else {
                                Toast.makeText(getContext(), getString(R.string.server_failure, "UnLike Media"), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
}