package com.khazanah.ig_frames;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

//This activity displays the instagram login screen - a user can login to generate an access token,
//which can then be used to fetch the logged in user's media
//Once the user is authenticated (i.e. token generated), the user is returned to the main activity
public class Login extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        WebView webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        // replace with actual client id and redirect URI
        String authString = "https://api.instagram.com/oauth/authorize/?" +
                "client_id=CLIENT-ID" +
                "redirect_uri=REDIRECT-URI" +
                "response_type=token";
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView wView, String url) {
                return false;
            }

            public void onPageFinished(WebView view, String url) {
                if (view.getUrl().contains("#access_token")) {
                    MainActivity.myLink = view.getUrl();
                    Toast.makeText(Login.this, getResources().getString(R.string.login_success),
                            Toast.LENGTH_SHORT).show();
                    view.loadUrl(getResources().getString(R.string.logout_url));
                    finish();
                }
            }
        });
        webView.loadUrl(authString);
    }
}
