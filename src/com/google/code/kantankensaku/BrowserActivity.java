// Copyright 2011 Google Inc. All Rights Reserved.
// Author: Hiroshi Ichikawa

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.code.kantankensaku;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BrowserActivity extends Activity {
  
  private class FailureException extends Exception {
  }
  
  private enum Mode {
    RESULT_PAGE_WITH_CANDIDATES,
    RESULT_PAGE_WITHOUT_CANDIDATES,
    SUB_PAGE,
  }

  private static final int NUM_RESULTS_PER_PAGE = 8;
  private static final boolean TEST_REDIRECT = false;

  private View resultPageTopBar;
  private View subPageTopBar;
  private View candidatesBar;
  private LinearLayout candidatesContainer;
  private LinearLayout webViewContainer;
  private WebView webView;
  private TextView messageLabel;
  private Button prevButton;
  private Button nextButton;
  private Button backButton;
//  private Button upButton;
//  private Button downButton;
  private Button homeButton;
  private Button subPageHomeButton;
  private ImageView closeCandidatesButton;
  private TextView statusLabel;

  private Handler handler = new Handler();

  private String origQuery;
  private Vector<String> conversionCandidates;
  private int conversionIndex;

  private Vector<Result> allResults;
  private boolean searchCompleted;
  private int currentResultIndex;
  private int messageId = 0;
  private boolean candidatesClosed = false;
  private Mode currentMode = Mode.RESULT_PAGE_WITH_CANDIDATES;
  private boolean bigScreen = true;
  private int prevButtonEnabledResource;
  private int prevButtonDisabledResource;
  private int selectedCandidateResource;
  private boolean touched = false;
  private int firstPageHistoryIndex;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Needed to run Flash Player in Android 3.0 or later.
    getWindow().addFlags(HomeActivity.FLAG_HARDWARE_ACCELERATED);
    getWindow().requestFeature(Window.FEATURE_PROGRESS);
    setContentView(bigScreen ? R.layout.browser_big : R.layout.browser);
    resultPageTopBar = findViewById(R.id.resultPageTopBar);
    subPageTopBar = findViewById(R.id.subPageTopBar);
    candidatesBar = findViewById(R.id.candidatesBar);
    candidatesContainer = (LinearLayout)findViewById(R.id.candidatesContainer);
    webViewContainer = (LinearLayout)findViewById(R.id.webViewContainer);
    webView = (WebView)findViewById(R.id.webView);
    messageLabel = (TextView)findViewById(R.id.messageLabel);
    prevButton = (Button)findViewById(R.id.prevButton);
    nextButton = (Button)findViewById(R.id.nextButton);
    backButton = (Button)findViewById(R.id.backButton);
//    upButton = (Button)findViewById(R.id.upButton);
//    downButton = (Button)findViewById(R.id.downButton);
    homeButton = (Button)findViewById(R.id.homeButton);
    closeCandidatesButton = (ImageView)findViewById(R.id.closeCandidatesButton);
    subPageHomeButton = (Button)findViewById(R.id.subPageHomeButton);
    statusLabel = (TextView)findViewById(R.id.statusLabel);
    
    prevButton.setOnClickListener(onPrevButtonClick);
    nextButton.setOnClickListener(onNextButtonClick);
    backButton.setOnClickListener(onBackButtonClick);
//    upButton.setOnClickListener(onUpButtonClick);
//    downButton.setOnClickListener(onDownButtonClick);
    homeButton.setOnClickListener(onHomeButtonClick);
    subPageHomeButton.setOnClickListener(onHomeButtonClick);
    closeCandidatesButton.setOnClickListener(onCloseCandidatesButtonClick);
    statusLabel.setOnClickListener(onStatusLabelClick);

    if (bigScreen) {
      prevButtonEnabledResource = R.drawable.button_big_result_prev;
      prevButtonDisabledResource = R.drawable.button_big_result_prev_disabled;
      selectedCandidateResource = R.drawable.big_selected_candidate;
    } else {
      prevButtonEnabledResource = R.drawable.button_result_prev;
      prevButtonDisabledResource = R.drawable.button_result_prev_disabled;
      selectedCandidateResource = R.drawable.selected_candidate;
    }

    showMessage("読み込み中...");
    setTitle("かんたん検索");
    
    origQuery = getIntent().getExtras().getString("q");
    log("origQuery: %s", origQuery);
    conversionIndex = 0;
    clearResults();
    if (isExpired()) {
      showExpired();
      return;
    }
    new ConvertAndSearchTask(0).execute();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.browser_menu, menu);
    return true;
  }
  
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    super.onMenuItemSelected(featureId, item);
    switch (item.getItemId()) {
    case R.id.reloadItem:
      webView.reload();
      break;
    default:
      throw new RuntimeException("unimplemented item");  
    }
    return true;
  }
  
  protected void onResume() {
    super.onResume();
    startHistoryWatchTimer();
  }
  
  protected void onPause() {
    stopHistoryWatchTimer();
    super.onStop();
  }
  
  private void startHistoryWatchTimer() {
    handler.postDelayed(onHistoryWatchTimer, 200);
  }
  
  private void stopHistoryWatchTimer() {
    handler.removeCallbacks(onHistoryWatchTimer);
  }
  
  private Runnable onHistoryWatchTimer = new Runnable() {
    public void run() {
      updateBarsVisibility();
      startHistoryWatchTimer();
    }
  };
  
  private void updateBarsVisibility() {
    Mode newMode;
    // We don't switch to sub-page mode on page transition before any touch by the user.
    // This is to avoid situation where the page has auto-redirect (e.g. meta refresh) and
    // we automatically switch to sub-page mode.
    int historyIndex = webView.copyBackForwardList().getCurrentIndex();
    if (!touched && historyIndex > firstPageHistoryIndex) {
      // Page transition before any touch by the user.
      firstPageHistoryIndex = historyIndex;
    }
    if (webView.canGoBack() && historyIndex > firstPageHistoryIndex) {
      newMode = Mode.SUB_PAGE;
      candidatesClosed = true;
    } else if (candidatesClosed) {
      newMode = Mode.RESULT_PAGE_WITHOUT_CANDIDATES;
    } else {
      newMode = Mode.RESULT_PAGE_WITH_CANDIDATES;
    }
    if (newMode == currentMode) return;
    currentMode = newMode;
    resultPageTopBar.setVisibility(newMode != Mode.SUB_PAGE ? View.VISIBLE : View.GONE);
    candidatesBar.setVisibility(newMode == Mode.RESULT_PAGE_WITH_CANDIDATES ? View.VISIBLE : View.GONE);
    subPageTopBar.setVisibility(newMode == Mode.SUB_PAGE ? View.VISIBLE : View.GONE);
    resultPageTopBar.setBackgroundResource(newMode == Mode.RESULT_PAGE_WITH_CANDIDATES ?
        R.drawable.result_top_bar_with_candidates : R.drawable.background_repeated);
  }
  
  private DownloadListener onWebViewDownload = new DownloadListener() {
    public void onDownloadStart(String url, String userAgent,
        String contentDisposition, String mimetype, long contentLength) {
      loadMessagePage(
          "かんたん検索では、このページを表示できません。" +
          "このページを表示するには、通常のブラウザをご利用ください。");
    }
  };
  
  private OnTouchListener onWebViewTouch = new OnTouchListener() {
    public boolean onTouch(View v, MotionEvent event) {
      touched = true;
      return false;
    }
  };
  
  private boolean isExpired() {
    Calendar now = Calendar.getInstance();
    Calendar expireAt = Calendar.getInstance();
    // Transliterate API may stop working after May 26, 2014.
    // Note that month here is 0-origin and we expire one day before to consider time zone
    // difference.
    // http://code.google.com/intl/ja/apis/language/transliterate/overview.html
    expireAt.set(2014, 4, 25);
    return now.compareTo(expireAt) >= 0;
  }
  
  private void showExpired() {
    showMessage("かんたん検索を最新版にアップデートしてください。");
  }
  
  private WebChromeClient webChromeClient = new WebChromeClient() {
    public void onProgressChanged(WebView view, int progress) {
       BrowserActivity.this.setProgress(progress * 100);
   }
 };
  
  private WebViewClient webViewClient = new WebViewClient() {
    
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      URI uri = URI.create(url);
      if (uri != null && uri.getScheme() != null && uri.getScheme().equals("market")) {
        loadMessagePage(
            "かんたん検索から Android マーケットを開くことはできません。" +
            " Android マーケットを直接起動するか、通常のブラウザをご利用ください。");
      } else {
        // Forces opening all URLs in this WebView.
        webView.loadUrl(url);
      }
      return true;
    }
    
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      log("pagestart: %s, historyIndex=%d", url,
          webView.copyBackForwardList().getCurrentIndex());
      URI uri = URI.create(url);
      String title = "かんたん検索";
      if (uri.getHost() != null) {
        title += " - " + uri.getHost();
      }
      setTitle(title);
    }

  };
  
  private int getCurrentHistoryIndex() {
    return webView.copyBackForwardList().getCurrentIndex();
  }
  
  private void dumpHistory() {
    WebBackForwardList list = webView.copyBackForwardList();
    for (int i = 0; i <= list.getCurrentIndex(); ++i) {
      log("history[%d] = %s", i, list.getItemAtIndex(i).getUrl());
    }
  }
  
  private OnClickListener onPrevButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      if (currentResultIndex <= 0) return;
      showResult(currentResultIndex - 1);
    }
  };
  
  private OnClickListener onBackButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      log("back");
      webView.goBack();
    }
  };
  
  private OnClickListener onNextButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      log("currentResultIndex = %d", currentResultIndex);
      int nextIndex = currentResultIndex + 1;
      if (!searchCompleted || nextIndex < allResults.size()) {
        candidatesClosed = true;
        updateBarsVisibility();
        showResult(nextIndex);
      }
    }
  };
  
  private OnClickListener onUpButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      webView.pageUp(false);
    }
  };
  
  private OnClickListener onDownButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      webView.pageDown(false);
    }
  };
  
  private OnClickListener onHomeButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      finish();
    }
  };
  
  private OnClickListener onCloseCandidatesButtonClick = new OnClickListener() {
    public void onClick(View arg0) {
      candidatesClosed = true;
      updateBarsVisibility();
    }
  };
  
  private OnClickListener onStatusLabelClick = new OnClickListener() {
    public void onClick(View arg0) {
      if (currentMode == Mode.RESULT_PAGE_WITHOUT_CANDIDATES) {
        //candidatesClosed = false;
        //updateBarsVisibility();
        //resultPageTopBar.requestLayout();
      }
    }
  };
  
  private class OnCandidateLabelClick implements OnClickListener {
    
    private int index;
    
    public OnCandidateLabelClick(int index) {
      this.index = index;
    }
    
    public void onClick(View v) {
      conversionIndex = index;
      clearResults();
      showResult(0);
    }
    
  }
  
  private void showResult(int resultIndex) {
    log("showResult(%d)", resultIndex);
    if (searchCompleted && resultIndex >= allResults.size() && !allResults.isEmpty()) {
      resultIndex = allResults.size() - 1;
    }
    if (resultIndex < allResults.size()) {
      currentResultIndex = resultIndex;
      String url = TEST_REDIRECT ?
          "http://gimite.net/temp/redirect.html" : allResults.get(resultIndex).url();
      recreateWebView();
      webView.loadUrl(url);
      hideMessage();
    } else if (!searchCompleted) {
      // Fetches more results and tries again.
      new ConvertAndSearchTask(resultIndex).execute();
    } else {
      showMessage("見つかりませんでした。");
    }
    prevButton.setBackgroundResource(resultIndex > 0 ?
        prevButtonEnabledResource : prevButtonDisabledResource);
    // TODO prepare disabled next button image
//    nextButton.setBackgroundResource(
//        !searchCompleted || resultIndex < allResults.size() - 1 ?
//            R.drawable.button_result_next : R.drawable.button_result_next_disabled);
  }
  
  private void recreateWebView() {
    if (webView != null) webViewContainer.removeView(webView);
    webView = (WebView)getLayoutInflater().inflate(R.layout.web_view, null);
    webView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    // Enables Flash Player only for Android >=3.0.
    // Versions before has a bug that Flash content overlaps widgets outside of WebView.
    if (Build.VERSION.SDK_INT >= 11) {
      enablePlugins();
    }
    webView.setWebChromeClient(webChromeClient);
    webView.setWebViewClient(webViewClient);
    webView.setDownloadListener(onWebViewDownload);
    webView.setOnTouchListener(onWebViewTouch);
    WebSettings webSettings = webView.getSettings();
    webSettings.setBuiltInZoomControls(true);
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDefaultFontSize(28);
    webSettings.setDefaultFixedFontSize(28);
    webViewContainer.addView(webView);
    touched = false;
    firstPageHistoryIndex = 0;
  }
  
  private class ConvertAndSearchTask extends AsyncTask<Void, Void, SearchResponse> {

    private int start;
    private int resultIndexToShow;
    
    public ConvertAndSearchTask(int resultIndexToShow) {
      this.start = allResults.size();
      this.resultIndexToShow = resultIndexToShow;
    }
    
    @Override
    protected SearchResponse doInBackground(Void... args) {
      try {
        conversionCandidates = convert(origQuery);
        final String convertedQuery = getConvertedQuery(conversionCandidates);
        log("converted: %s", convertedQuery);
        handler.post(new Runnable() {
          public void run() {
            statusLabel.setText(String.format("“%s”", convertedQuery));
            renderCandidates();
          }
        });
        return search(convertedQuery, start);
      } catch (FailureException e) {
        return null;
      }
    }
    
    @Override
    protected void onPostExecute(SearchResponse response) {
      log("postexec");
      if (response != null) {
        allResults.addAll(response.results);
        searchCompleted = response.completed;
        showResult(resultIndexToShow);
      } else {
        reportFailure();
      }
    }
    
  }
  
  private SearchResponse search(String query, int start) throws FailureException {
    assert start % 8 == 0;
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("key", Config.GOOGLE_API_KEY);
    params.put("cx", Config.CUSTOM_SEARCH_ID);
    params.put("q", query);
    params.put("alt", "json");
    // Ideally we want to *prefer* Japanese instead of *restricting to* Japanese,
    // but I don't know how to do it.
    params.put("lr", "lang_ja");
    params.put("start", Integer.toString(start + 1));
    params.put("num", "10");
    params.put("safe", "high");
    String url = createUrl("https://www.googleapis.com/customsearch/v1", params);
    Log.i("kantankensaku", String.format("url: %s", url));
    String json = fetch(url);
    log("json: %s", json);
    try {
      JSONObject root = (JSONObject)new JSONTokener(json).nextValue();
      SearchResponse response = new SearchResponse();
      response.results = new Vector<Result>();
      if (root.has("items")) {
        JSONArray origResults = root.getJSONArray("items");
        for (int i = 0; i < origResults.length(); ++i) {
          response.results.add(new Result(origResults.getJSONObject(i)));
        }
        response.completed = !root.getJSONObject("queries").has("nextPage");
      } else {
        response.completed = true;
      }
      return response;
    } catch (JSONException e) {
      e.printStackTrace();
      throw new FailureException();
    } catch (ClassCastException e) {
      e.printStackTrace();
      throw new FailureException();
    }
  }
  
  private String getConvertedQuery(Vector<String> cands) throws FailureException {
    if (conversionIndex < cands.size()) {
      return cands.get(conversionIndex);
    } else if (!cands.isEmpty()) {
      return cands.get(0);
    } else {
      return origQuery;
    }
  }
  
  private Vector<String> convert(String origQuery) throws FailureException {
    assert origQuery.length() != 0;
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("langpair", "ja-Hira|ja");
    params.put("text", origQuery);
    String url = createUrl("http://www.google.com/transliterate", params);
    log("conv url: %s", url);
    String json = fetch(url);
    log("conv json: %s", json);
    try {
      JSONArray res = (JSONArray)new JSONTokener(json).nextValue();
      Vector<String> cands = getCandidatesFromConversionResponse(res, 0);
      log("cands: %s", cands.toString());
      return cands;
    } catch (JSONException e) {
      e.printStackTrace();
      throw new FailureException();
    } catch (ClassCastException e) {
      e.printStackTrace();
      throw new FailureException();
    }
  }
  
  private Vector<String> getCandidatesFromConversionResponse(JSONArray res, int index)
      throws JSONException {
    Vector<String> cands = new Vector<String>();
    if (index >= res.length()) {
      cands.add("");
    } else if (res.isNull(index)) {
      // Workaround for a bug of Google IME API that it has "," at the end of
      // arrays, which results in null element.
      return getCandidatesFromConversionResponse(res, index + 1);
    } else {
      Vector<String> cdrCands = getCandidatesFromConversionResponse(res, index + 1);
      JSONArray carCands = res.getJSONArray(index).getJSONArray(1);
      for (int i = 0; i < carCands.length(); ++i) {
        // Workaround for a bug of Google IME API that it has "," at the end of
        // arrays, which results in null element.
        if (carCands.isNull(i)) continue;
        for (String cdrCand : cdrCands) {
          cands.add(carCands.getString(i) + cdrCand);
          if (cands.size() >= 100) return cands;
        }
      }
    }
    return cands;
  }
  
  private void reportFailure() {
    statusLabel.setText("");
    showMessage("検索に失敗しました。インターネットに接続されていることを確認して、最初からやり直してください。");
  }
  
  private void showMessage(String message) {
    webView.setVisibility(View.GONE);
    messageLabel.setText(message);
    messageLabel.setVisibility(View.VISIBLE);
  }
  
  private void hideMessage() {
    messageLabel.setVisibility(View.GONE);
    webView.setVisibility(View.VISIBLE);
  }
  
  private void loadMessagePage(String message) {
    // Opening the same data twice can cause weird history. So adds dummy class
    // to generate different data every time.
    webView.loadData(
        String.format(
            "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; " +
            "charset=utf-8\"></head><body>%s<div class=\"%d\"/></body></html>",
            message, messageId),
        "text/html; charset=utf-8", "utf-8");
    ++messageId;
  }
  
  private String createUrl(String baseUrl, HashMap<String, String> params) {
    String paramsStr = null;
    try {
      for (Map.Entry<String, String> entry : params.entrySet()) {
        paramsStr = paramsStr == null ? "" : paramsStr + "&";
        paramsStr += entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return baseUrl + (paramsStr == null ? "" : "?" + paramsStr);
  }
  
  private static class SearchResponse {
    public List<Result> results;
    public boolean completed;
  }
  
  private class Result {
    
    private JSONObject original;
    
    public Result(JSONObject original) {
      this.original = original;
    }
    
    public String url() {
      try {
        return original.getString("link");
      } catch (JSONException e) {
        e.printStackTrace();
        return "about:blank";
      }
    }
    
    public String title() {
      try {
        return original.getString("title");
      } catch (JSONException e) {
        e.printStackTrace();
        return "";
      }
    }
    
  }
  
  private void clearResults() {
    allResults = new Vector<Result>();
    searchCompleted = false;
  }
  
  private void log(String format, Object... args) {
    Log.i("kantankensaku", String.format(format, (Object[])args));
  }
  
  public String fetch(String urlStr) throws FailureException {
    try {
      URL url = new URL(urlStr);
      HttpURLConnection urlconn = (HttpURLConnection)url.openConnection();
      urlconn.setRequestMethod("GET");
      urlconn.setInstanceFollowRedirects(true);
      urlconn.connect();
  
      if (urlconn.getResponseCode() < 200 || urlconn.getResponseCode() >= 300) {
        log(String.format("%03d %s",
            urlconn.getResponseCode(), urlconn.getResponseMessage()));
        throw new FailureException();
      }
  
      InputStreamReader in = new InputStreamReader(urlconn.getInputStream());
      StringBuffer result = new StringBuffer();
      int read;
      char[] buffer = new char[4096];
      while ((read = in.read(buffer, 0, buffer.length)) > 0) {
        result.append(buffer, 0, read);
      }
      in.close();
      urlconn.disconnect();
      return result.toString();
    } catch (IOException e) {
      throw new FailureException();
    }
  }
  
  private void renderCandidates() {
    int padding = bigScreen ? 35 : 28;
    int textSize = bigScreen ? 44 : 29;
    candidatesContainer.removeAllViews();
    for (int i = 0; i < conversionCandidates.size(); ++i) {      
      addCandidateSeparater();
      TextView label = new TextView(this);
      label.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
      label.setText(conversionCandidates.get(i));
      label.setTextColor(Color.BLACK);
      label.setPadding(padding, 0, padding, 0);
      label.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
      label.setGravity(Gravity.CENTER);
      // Forces single line.
      label.setInputType(InputType.TYPE_NULL);
      if (i == conversionIndex) {
        label.setBackgroundResource(selectedCandidateResource);
      }
      label.setOnClickListener(new OnCandidateLabelClick(i));
      candidatesContainer.addView(label);
    }
    addCandidateSeparater();
  }
  
  private void addCandidateSeparater() {
    ImageView separater = new ImageView(this);
    separater.setBackgroundResource(R.drawable.candidates_separater);
    separater.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
    candidatesContainer.addView(separater);
  }
  
  /**
   * Enables plugins such as Flash.
   */
  private void enablePlugins() {
    // webView.getSettings().setPluginState(WebSettings.PluginState.ON);
    // in Android 2.2 or later.
    try {
      Class<?> webSettings = Class.forName("android.webkit.WebSettings");
      Class<?> pluginState = Class.forName("android.webkit.WebSettings$PluginState");
      Field onField = pluginState.getField("ON");
      Object on = onField.get(null);
      Method setPluginState = webSettings.getMethod("setPluginState", pluginState);
      setPluginState.invoke(webView.getSettings(), on);
      log("plugin enabled");
    } catch (ClassNotFoundException e) {
    } catch (NoSuchFieldException e) {
    } catch (NoSuchMethodException e) {
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
  
}