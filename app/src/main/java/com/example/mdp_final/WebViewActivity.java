package com.example.mdp_final;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewActivity extends AppCompatActivity {

    private boolean paymentFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        String url = getIntent().getStringExtra("url");

        if (url != null) {
            if (url.startsWith("http://http://")) {
                url = url.replace("http://http://", "http://");
            } else if (url.startsWith("https://https://")) {
                url = url.replace("https://https://", "https://");
            }
        }
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String url = uri.toString();
                String scheme = uri.getScheme();

                Log.i("url", url);

                if ("intent".equals(scheme)) {
                    String tossUrl = url.replace("intent://", "supertoss://");
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tossUrl));
                    try {
                        paymentFlag = true;
                        Log.i("dawdwadaw", "결제성공까지감");
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        // 토스 앱이 설치되어 있지 않을 때의 처리
                        Toast.makeText(getApplicationContext(), "토스 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                    return true; // 웹뷰에서 URL 로딩을 막음
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i("onPageFini", "onPageFini");
                if(paymentFlag){
                    new Handler().postDelayed(() -> {
                        String jsCode = "javascript:(function() { " +
                                "var element = document.querySelector('p.text.text--word-break.typography-t6.text--display-inline-block.text--as'); " +
                                "if (element) { " +
                                "   return element.innerText; " +
                                "} else { " +
                                "   return 'Element not found'; " +
                                "} " +
                                "})()";

                        webView.evaluateJavascript(jsCode, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                Log.i("시발시발", value);

                                String urlPattern = "https?://[^\\s\"]+";
                                Pattern pattern = Pattern.compile(urlPattern);
                                Matcher matcher = pattern.matcher(value);
                                String successUrl;
                                // URL 찾기

                                if (matcher.find()) {
                                        successUrl = matcher.group();
                                        Log.i("successUrl", successUrl);
                                    // 이후 처리
                                } else {
                                    successUrl = "";
                                    Log.e("WebViewActivity", "No URL found in the value");
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("url", "abcdxfae.af1312aw.com");
                                    setResult(RESULT_OK, resultIntent);
                                    finish(); // 액티비티 종료
                                    // 적절한 에러 처리
                                }

                                Log.i("successUrl", successUrl);

                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("url", successUrl);
                                setResult(RESULT_OK, resultIntent);
                                finish(); // 액티비티 종료
                            }
                        });
                    }, 2000);
                }

            }
    });
        Log.i("",url);
        webView.loadUrl(url);
    }
}