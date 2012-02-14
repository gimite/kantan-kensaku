// Copyright 2011 Google Inc. All Rights Reserved.

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlecode.kantankensaku;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity shown on app launch. Shows query input field and keyboard.
 * @author Hiroshi Ichikawa
 */
public class HomeActivity extends Activity {

    private static final int DIALOG_ABOUT = 0;
    
    private TextView queryField;
    private Button searchButton;
    private CustomKeyboardView keyboardView;
    
    private SizedResources sizedResources;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomExceptionHandler.setHandler();
        setRequestedOrientation(Compatibility.getScreenOrientationSensorLandscapeOrLandscape());
        sizedResources = new SizedResources(this);
        
        setContentView(sizedResources.homeLayout);
        queryField = (TextView)findViewById(R.id.queryField);
        keyboardView = (CustomKeyboardView)findViewById(R.id.keyboardView);
        searchButton = (Button)findViewById(R.id.searchButton);
        
        keyboardView.initialize(this, queryField);
        searchButton.setOnClickListener(onSearchButtonClick);
        checkDisplaySize();
    }

    /**
     * Called when the user goes back from the search result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        queryField.setText("");
        keyboardView.setMode(CustomKeyboardView.Mode.GOJUON);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        switch (item.getItemId()) {
        case R.id.originalHomeItem:
            startOtherHome();
            break;
        case R.id.settingsItem:
            startActivity(new Intent("android.settings.SETTINGS"));
            break;
        case R.id.aboutItem:
            showDialog(DIALOG_ABOUT);
            break;
        default:
            throw new RuntimeException("unimplemented item");    
        }
        return true;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_ABOUT:
            return new AboutDialog(this);
        default:
            return null;
        }
    }
    
    /**
     * Event handler for search button click.
     */
    private OnClickListener onSearchButtonClick = new OnClickListener() {
        public void onClick(View arg0) {
            search();
        }
    };
    
    /**
     * Checks the display size, and shows error message if the display size is not supported.
     */
    private void checkDisplaySize() {
        if (!sizedResources.supported) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.app_name));
            builder.setMessage(String.format(
                    "このデバイスでは%sを利用できません。\n" +
                    "%d×%d以上の解像度が必要です。",
                    getResources().getString(R.string.app_name),
                    SizedResources.SMALL_MIN_DISPLAY_WIDTH,
                    SizedResources.SMALL_MIN_DISPLAY_HEIGHT));
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Suggests to go to other home apps. This is useful in case this app is
                            // registered as default home app.
                            startOtherHome();
                        }
                    });
            builder.create().show();
        }
    }
    
    /**
     * Performs search.
     */
    private void search() {
        String query = queryField.getText().toString();
        if (query.length() == 0) return;
        Intent intent = new Intent(HomeActivity.this, BrowserActivity.class);
        intent.putExtra("q", queryField.getText().toString());
        // Use startActivityForResult() instead of startAcrivity() to clear the input when
        // user come back here (i.e. on onActivityResult()).
        startActivityForResult(intent, 0);
    }
    
    /**
     * Starts another home app.
     */
    private void startOtherHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(Intent.createChooser(intent, null));
    }
    
    @SuppressWarnings("unused")
    private void log(String format, Object... args) {
        Log.i("kantankensaku", String.format(format, (Object[])args));
    }
    
}
