package jp.wasabeef.richeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Copyright (C) 2017 Wasabeef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class RichViewer extends WebView {

  public interface AfterInitialLoadListener {

    void onAfterInitialLoad(boolean isReady);
  }

  private static final String SETUP_HTML = "file:///android_asset/viewer.html";
  private static final String CALLBACK_SCHEME = "re-callback://";
  private static final String STATE_SCHEME = "re-state://";
  private boolean isReady = false;
  private String mContents;
  private AfterInitialLoadListener mLoadListener;

  public RichViewer(Context context) {
    this(context, null);
  }

  public RichViewer(Context context, AttributeSet attrs) {
    this(context, attrs, android.R.attr.webViewStyle);
  }

  @SuppressLint("SetJavaScriptEnabled")
  public RichViewer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    setVerticalScrollBarEnabled(false);
    setHorizontalScrollBarEnabled(false);
    getSettings().setJavaScriptEnabled(true);
    setWebChromeClient(new WebChromeClient());
    setWebViewClient(createWebviewClient());
    loadUrl(SETUP_HTML);
  }

  protected EditorWebViewClient createWebviewClient() {
    return new EditorWebViewClient();
  }


  public void setOnInitialLoadListener(AfterInitialLoadListener listener) {
    mLoadListener = listener;
  }

  public void setHtml(String contents) {
    if (contents == null) {
      contents = "";
    }
    try {
      exec("javascript:RE.setHtml('" + URLEncoder.encode(contents, "UTF-8") + "');");
    } catch (UnsupportedEncodingException e) {
      // No handling
    }
    mContents = contents;
  }

  public String getHtml() {
    return mContents;
  }

  protected void exec(final String trigger) {
    if (isReady) {
      load(trigger);
    } else {
      postDelayed(new Runnable() {
        @Override public void run() {
          exec(trigger);
        }
      }, 100);
    }
  }

  private void load(String trigger) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      evaluateJavascript(trigger, null);
    } else {
      loadUrl(trigger);
    }
  }

  protected class EditorWebViewClient extends WebViewClient {
    @Override public void onPageFinished(WebView view, String url) {
      isReady = url.equalsIgnoreCase(SETUP_HTML);
      if (mLoadListener != null) {
        mLoadListener.onAfterInitialLoad(isReady);
      }
    }

    @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
      String decode;
      try {
        decode = URLDecoder.decode(url, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        // No handling
        return false;
      }
      return super.shouldOverrideUrlLoading(view, url);
    }
  }

  public void loadCSS(String cssFile) {
    String jsCSSImport = "(function() {" +
            "    var head  = document.getElementsByTagName(\"head\")[0];" +
            "    var link  = document.createElement(\"link\");" +
            "    link.rel  = \"stylesheet\";" +
            "    link.type = \"text/css\";" +
            "    link.href = \"" + cssFile + "\";" +
            "    link.media = \"all\";" +
            "    head.appendChild(link);" +
            "}) ();";
    exec("javascript:" + jsCSSImport + "");
  }

  public void setEditorFontSize(int px) {
    exec("javascript:RE.setBaseFontSize('" + px + "px');");
  }

  @Override public void setPadding(int left, int top, int right, int bottom) {
    super.setPadding(left, top, right, bottom);
    exec("javascript:RE.setPadding('" + left + "px', '" + top + "px', '" + right + "px', '" + bottom
            + "px');");
  }
}
