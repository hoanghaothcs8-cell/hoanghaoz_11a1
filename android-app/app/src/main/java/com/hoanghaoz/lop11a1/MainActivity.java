package com.hoanghaoz.lop11a1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

/**
 * Vo Android cua app lop 11A1.
 *
 * App khong chua noi dung nao ca: no mo dung trang cua lop dang chay tren may chu,
 * nen luc co thay nang trang len thi mo app len la thay cai moi, khong can dung goi .apk lai.
 * App chi them 3 thu ma trinh duuyen khong co: icon o man hinh chinh, cua so rieng
 * khong thanh dia chi, va nut Quay lai cua dien thoai khong lam mat tran dang choi.
 */
public class MainActivity extends Activity {

  /** Dai dien thoai dinh danh minh trong User-Agent de trang web biet "day la app". */
  private static final String APP_TAG = "HoanghaoZApp";

  private static final String SITE_HOST = "hoanghaothcs8-cell.github.io";
  private static final String START_URL = "https://" + SITE_HOST + "/hoanghaoz_11a1/";

  private WebView web;
  private ProgressBar bar;
  private boolean offline = false;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.parseColor("#F7F8FA"));

    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);          // de app luu ho so, Tinh Thong, cai dat CARO
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setUseWideViewPort(true);
    s.setLoadWithOverviewMode(true);
    s.setTextZoom(100);                    // bug mat cua he dieu hanh khong duoc lam no to len
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    s.setJavaScriptCanOpenWindowsAutomatically(false);

    String ua = s.getUserAgentString();
    if (ua != null && ua.indexOf(APP_TAG) < 0) {
      s.setUserAgentString(ua + " " + APP_TAG);
    }

    web.setBackgroundColor(Color.WHITE);
    web.setOverScrollMode(View.OVER_SCROLL_NEVER);
    web.setWebViewClient(client);
    web.setWebChromeClient(chrome);

    root.addView(web, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    bar.setMax(100);
    bar.setVisibility(View.GONE);
    FrameLayout.LayoutParams barLp =
        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
    barLp.gravity = Gravity.TOP;
    root.addView(bar, barLp);

    setContentView(root);
    web.requestFocus();
    web.loadUrl(START_URL);
  }

  private final WebViewClient client = new WebViewClient() {

    @Override
    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
      Uri u = req == null ? null : req.getUrl();
      if (u == null) return true;
      String scheme = u.getScheme();
      if (scheme == null) return true;

      if ("about".equals(scheme) || "data".equals(scheme) || "blob".equals(scheme)) {
        return false;
      }
      if ("http".equals(scheme) || "https".equals(scheme)) {
        String host = u.getHost();
        if (host != null && host.equalsIgnoreCase(SITE_HOST)) return false;   // van trong app
      }
      // Lien ket ra ngoai (Tai lieu, Zalo, mail, so dien thoai...) -> mo bang ung dung khac
      openExternally(u);
      return true;
    }

    @Override
    public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
      if (req != null && req.isForMainFrame()) goOffline();
    }

    @Override
    public void onReceivedHttpError(WebView v, WebResourceRequest req, WebResourceResponse res) {
      if (req != null && req.isForMainFrame() && res != null && res.getStatusCode() >= 400) {
        goOffline();
      }
    }

    @Override
    public void onPageFinished(WebView v, String url) {
      if (url != null && url.startsWith("https://" + SITE_HOST)) offline = false;
    }
  };

  private final WebChromeClient chrome = new WebChromeClient() {
    @Override
    public void onProgressChanged(WebView v, int p) {
      if (bar == null) return;
      bar.setProgress(p);
      bar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
    }
  };

  /** May mat mang: thay trang bao loi cua app (co nut Thu lai), khong de trang trang. */
  private void goOffline() {
    offline = true;
    web.loadDataWithBaseURL(START_URL, OFFLINE_HTML, "text/html", "utf-8", null);
  }

  private void openExternally(Uri u) {
    try {
      Intent it = new Intent(Intent.ACTION_VIEW, u);
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(it);
    } catch (Exception ignored) {
      /* May nay chua co ung dung nao mo duoc lien ket do */
    }
  }

  @Override
  public void onBackPressed() {
    if (web != null && web.canGoBack()) {
      web.goBack();                 // nguoc lai trong app, khong thoat tran dang choi
    } else {
      moveTaskToBack(true);         // o trang chu thi thu nho app nhu bam nut Trang chu
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (web == null) return;
    web.onResume();
    if (offline) web.loadUrl(START_URL);   // co mang lai thi tu mo lai trang
  }

  @Override
  protected void onPause() {
    if (web != null) web.onPause();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    if (web != null) {
      web.loadUrl("about:blank");
      web.destroy();
      web = null;
    }
    super.onDestroy();
  }

  private int dp(float v) {
    return Math.round(v * getResources().getDisplayMetrics().density);
  }

  private static final String OFFLINE_HTML =
      "<!doctype html><html lang='vi'><head><meta charset='utf-8'>"
    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
    + "<title>Chua co mạng</title><style>"
    + "body{margin:0;background:#F7F8FA;color:#0F172A;"
    + "font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;"
    + "display:flex;min-height:100vh;align-items:center;justify-content:center;padding:24px}"
    + ".box{max-width:420px;text-align:center}"
    + ".ic{width:64px;height:64px;margin:0 auto 18px;border-radius:18px;background:#E2E8F0;"
    + "display:flex;align-items:center;justify-content:center}"
    + "h1{font-size:20px;margin:0 0 10px}"
    + "p{font-size:15px;line-height:1.6;margin:0 0 22px;color:#475569}"
    + "a{display:inline-block;background:#0F172A;color:#fff;text-decoration:none;"
    + "padding:13px 26px;border-radius:12px;font-size:16px}"
    + "</style></head><body><div class='box'>"
    + "<div class='ic'><svg width='34' height='34' viewBox='0 0 24 24' fill='none' "
    + "stroke='#0F172A' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
    + "<path d='M12 20h.01'/><path d='M8.5 16.4a5 5 0 0 1 7 0'/><path d='M5 12.9a10 10 0 0 1 14 0'/>"
    + "<path d='M1.9 9.3a15 15 0 0 1 20.2 0'/><path d='m2 2 20 20'/></svg></div>"
    + "<h1>Máy chưa có mạng</h1>"
    + "<p>App này cần mạng để đọc danh sách lớp, xếp hạng và chơi CARO cùng bạn. "
    + "Bật WiFi hoặc 4G rồi chạm nút dưới.</p>"
    + "<a href='" + START_URL + "'>Thử lại</a>"
    + "</div></body></html>";
}
