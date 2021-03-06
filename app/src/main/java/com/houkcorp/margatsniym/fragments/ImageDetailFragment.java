package com.houkcorp.margatsniym.fragments;

import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.houkcorp.margatsniym.R;
import com.houkcorp.margatsniym.activities.ImageDetailActivity;
import com.houkcorp.margatsniym.events.ExpiredOAuthEvent;
import com.houkcorp.margatsniym.events.MediaChangedEvent;
import com.houkcorp.margatsniym.models.Media;
import com.houkcorp.margatsniym.models.MediaResponse;
import com.houkcorp.margatsniym.services.ServiceFactory;
import com.houkcorp.margatsniym.services.UserService;
import com.houkcorp.margatsniym.transformations.CircleTransformation;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ImageDetailFragment extends Fragment {
    private static String IMAGE_DETAIL_FRAGMENT = "ImageDetailFragment";

    private Media mMedia;
    private String mAccessToken;

    @BindView(R.id.image_detail_image_view) ImageView mDetailImageView;
    @BindView(R.id.image_detail_liked_image_view) ImageView mDetailLikedImageView;
    @BindView(R.id.image_detail_unliked_image_view) ImageView mDetailUnLikedImageView;
    @BindView(R.id.image_detail_users_image_view) ImageView mDetailUsersImageView;
    @BindView(R.id.image_detail_location_linear_layout) LinearLayout mDetailLocationLinearLayout;
    @BindView(R.id.image_detail_progress_bar) ProgressBar mDetailProgressBar;
    @BindView(R.id.image_detail_scroll_view) ScrollView mDetailScrollView;
    @BindView(R.id.image_detail_address_line_one) TextView mDetailAddressLineOne;
    @BindView(R.id.image_detail_address_line_two) TextView mDetailAddressLineTwo;
    @BindView(R.id.image_detail_users_text_view) TextView mDetailUsersTextView;
    @BindView(R.id.image_detail_video_view) VideoView mDetailVideoView;

    public static ImageDetailFragment newInstance(Media media, String accessToken) {
        Bundle extras = new Bundle();
        extras.putParcelable(ImageDetailActivity.MEDIA_EXTRA, media);
        extras.putString(ImageDetailActivity.ACCESS_TOKEN_EXTRA, accessToken);

        ImageDetailFragment imageDetailFragment = new ImageDetailFragment();
        imageDetailFragment.setArguments(extras);

        return imageDetailFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image_detail, container, false);

        ButterKnife.bind(this, root);

        Bundle extras = getArguments();
        if (extras != null) {
            mMedia = extras.getParcelable(ImageDetailActivity.MEDIA_EXTRA);
            mAccessToken = extras.getString(ImageDetailActivity.ACCESS_TOKEN_EXTRA);
        }

        if (!TextUtils.isEmpty(mMedia.getCaption().getText())) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(mMedia.getCaption().getText());
            }
        }

        Picasso
                .with(getContext())
                .load(mMedia.getUser().getProfilePicture())
                .transform(new CircleTransformation())
                .into(mDetailUsersImageView);

        mDetailUsersTextView.setText(mMedia.getUser().getFullName());

        //TODO: Need to add the hashtags.  Also remove hashtags from header.
        // Displays either ImageView or VideoView based of the type.
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

        // Displays the proper icon based off whether or not the Media was liked.
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

        // Display the location if it exists.
        if (mMedia.getLocation() != null) {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(mMedia.getLocation().getLatitude(), mMedia.getLocation().getLongitude(), 1);
            } catch (IOException e) {
                Log.e(IMAGE_DETAIL_FRAGMENT, e.getLocalizedMessage());
            }

            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                ArrayList<String> addressparts = new ArrayList<>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressparts.add(address.getAddressLine(i));
                }

                if (addressparts.size() > 0) {
                    mDetailAddressLineOne.setText(addressparts.get(0));
                }

                if (addressparts.size() > 1) {
                    mDetailAddressLineTwo.setText(addressparts.get(1));
                }
            }
            mDetailLocationLinearLayout.setVisibility(View.VISIBLE);
        } else {
            mDetailLocationLinearLayout.setVisibility(View.GONE);
        }

        mDetailProgressBar.setVisibility(View.GONE);
        mDetailScrollView.setVisibility(View.VISIBLE);

        return root;
    }

    /**
     * Likes the media if it is not currently liked.
     */
    private void likeMedia() {
        UserService service = ServiceFactory.getInstagramUserService();
        service.likeMedia(mMedia.getId(), mAccessToken, "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response<MediaResponse>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(IMAGE_DETAIL_FRAGMENT, e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Response<MediaResponse> response) {
                        ResponseBody errorBody = response.errorBody();
                        if (!response.isSuccessful() && errorBody != null) {
                            try {
                                Converter<ResponseBody, MediaResponse> errorConverter = ServiceFactory.getRetrofit().responseBodyConverter(MediaResponse.class, new Annotation[0]);
                                MediaResponse mediaError = errorConverter.convert(response.errorBody());
                                Toast.makeText(getContext(), mediaError.getMeta().getErrorMessage(), Toast.LENGTH_LONG).show();

                                if (mediaError.getMeta().getErrorType().equals(getString(R.string.o_auth_error))) {
                                    EventBus.getDefault().post(new ExpiredOAuthEvent(true));
                                }
                            } catch (IOException e) {
                                Log.e(IMAGE_DETAIL_FRAGMENT, "There was a problem parsing the error response.");
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

    /**
     * Unlikes the media if it is currently liked.
     */
    private void unLikeMedia() {
        UserService service = ServiceFactory.getInstagramUserService();
        service.unLikeMedia(mMedia.getId(), mAccessToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response<MediaResponse>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(IMAGE_DETAIL_FRAGMENT, e.getLocalizedMessage());
                    }

                    @Override
                    public void onNext(Response<MediaResponse> response) {
                        ResponseBody errorBody = response.errorBody();
                        if (!response.isSuccessful() && errorBody != null) {
                            try {
                                Converter<ResponseBody, MediaResponse> errorConverter = ServiceFactory.getRetrofit().responseBodyConverter(MediaResponse.class, new Annotation[0]);
                                MediaResponse mediaError = errorConverter.convert(response.errorBody());
                                Toast.makeText(getContext(), mediaError.getMeta().getErrorMessage(), Toast.LENGTH_LONG).show();

                                if (mediaError.getMeta().getErrorType().equals(getString(R.string.o_auth_error))) {
                                    EventBus.getDefault().post(new ExpiredOAuthEvent(true));
                                }
                            } catch (IOException e) {
                                Log.e(IMAGE_DETAIL_FRAGMENT, "There was a problem parsing the error response.");
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