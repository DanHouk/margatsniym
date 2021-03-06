package com.houkcorp.margatsniym.utils;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.Toast;

import com.houkcorp.margatsniym.R;
import com.houkcorp.margatsniym.dialogs.LoginDialog;
import com.houkcorp.margatsniym.events.LoginEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * A WebClient for displaying a list of images from Instagram for a User.
 */
public class MargatsniYmWebViewClient extends android.webkit.WebViewClient {
    private static final String ACCESS_TOKEN = "access_token=";

    private LoginDialog mLoginDialog;

    /**
     * The web client for showing the login view.
     *
     * @param loginDialog The dialog that called the WebClient
     */
    public MargatsniYmWebViewClient(LoginDialog loginDialog) {
        super();
        mLoginDialog = loginDialog;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.contains(ACCESS_TOKEN)) {
            String[] splitString = url.split(ACCESS_TOKEN);
            mLoginDialog.setAccessToken(splitString[1]);
            EventBus.getDefault().post(new LoginEvent(splitString[1]));
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (request.getUrl().toString().contains(ACCESS_TOKEN)) {
            String[] splitString = request.getUrl().toString().split(ACCESS_TOKEN);
            mLoginDialog.setAccessToken(splitString[1]);
            EventBus.getDefault().post(new LoginEvent(splitString[1]));
        }

        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        String errorMessage = mLoginDialog.getString(R.string.login_failed) + ": " + description;
        Toast.makeText(mLoginDialog.getContext(), errorMessage, Toast.LENGTH_LONG).show();

        super.onReceivedError(view, errorCode, description, failingUrl);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        String errorMessage = mLoginDialog.getString(R.string.login_failed) + ": " + error.toString();
        Toast.makeText(mLoginDialog.getContext(), errorMessage, Toast.LENGTH_LONG).show();

        super.onReceivedError(view, request, error);
    }
}