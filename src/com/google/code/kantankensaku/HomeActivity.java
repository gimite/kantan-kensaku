// Copyright 2011 Google Inc. All Rights Reserved.
// Author: Hiroshi Ichikawa

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

package com.google.code.kantankensaku;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class HomeActivity extends Activity {

    private enum KanaType {
        SEION, DAKUON, HANDAKUON, YOUON,
    }
    
    private static final int DIALOG_ABOUT = 0;
    
    private TextView queryField;
    private KeyboardView keyboardView;
    private Button searchButton;
    
    private Keyboard gojuonKeyboard;
    private Keyboard simpleAsciiKeyboard;
    private HashMap<KanaType, HashMap<Character, Character>> charMap;
    private SizedResources sizedResources;
    
    // WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED in API Level 11
    public static final int FLAG_HARDWARE_ACCELERATED = 0x01000000;
    // ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE in API Level 9
    public static final int SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomExceptionHandler.setHandler();
        if (Build.VERSION.SDK_INT >= 9) {
            setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        sizedResources = new SizedResources(this);
        
        setContentView(sizedResources.homeLayout);
        queryField = (TextView)findViewById(R.id.queryField);
        keyboardView = (KeyboardView)findViewById(R.id.keyboardView);
        searchButton = (Button)findViewById(R.id.searchButton);
        
        gojuonKeyboard = new Keyboard(this, sizedResources.gojuonKeyboard);
        simpleAsciiKeyboard = new Keyboard(this, sizedResources.asciiKeyboard);
        keyboardView.setKeyboard(gojuonKeyboard);
        keyboardView.setOnKeyboardActionListener(onKeyboardAction);
        searchButton.setOnClickListener(onSearchButtonClick);
        createCharMap();
        checkDisplaySize();
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
    
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_ABOUT:
            return new AboutDialog(this);
        default:
            return null;
        }
    }
    
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
                            // Gives chance to go back to other home apps, in case this app is registered
                            // as default home app.
                            startOtherHome();
                        }
                    });
            builder.create().show();
        }
    }
    
    private void backSpace() {
        String query = queryField.getText().toString();
        if (query.length() > 0) {
            queryField.setTextKeepState(query.substring(0, query.length() - 1));
        }
    }
    
    private OnClickListener onSearchButtonClick = new OnClickListener() {
		public void onClick(View arg0) {
		    search();
		}
    };
    
    private OnClickListener onHomeButtonClick = new OnClickListener() {
        public void onClick(View arg0) {
            queryField.setText("");
        }
    };
    
    private void search() {
        String query = queryField.getText().toString();
        if (query.length() == 0) return;
        Intent intent = new Intent(HomeActivity.this, BrowserActivity.class);
        intent.putExtra("q", queryField.getText().toString());
        // Use startActivityForResult() instead of startAcrivity() to clear the input when
        // user come back here (i.e. on onActivityResult()).
        startActivityForResult(intent, 0);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        queryField.setText("");
        keyboardView.setKeyboard(gojuonKeyboard);
    }
    
    
    private void modifyChar(KanaType type) {
        String query = queryField.getText().toString();
        if (query.length() == 0) return;
        Character converted = charMap.get(type).get(query.charAt(query.length() - 1));
        if (converted != null) {
            queryField.setTextKeepState(query.substring(0, query.length() - 1) + converted);
        }
    }
    
    private void createCharMap() {
        HashMap<KanaType, String> typeToChars = new HashMap<KanaType, String>();
        String seionChars =
            "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん";
        typeToChars.put(KanaType.SEION, seionChars);
        typeToChars.put(KanaType.DAKUON,
                "あいゔえおがぎぐげござじずぜぞだぢづでどなにぬねのばびぶべぼまみむめもやゆよらりるれろわをん");
        typeToChars.put(KanaType.HANDAKUON,
                "あいうえおかきくけこさしすせそたちつてとなにぬねのぱぴぷぺぽまみむめもやゆよらりるれろわをん");
        typeToChars.put(KanaType.YOUON,
                "ぁぃぅぇぉかきくけこさしすせそたちってとなにぬねのはひふへほまみむめもゃゅょらりるれろゎをん");
        Set<KanaType> types = typeToChars.keySet();
        charMap = new HashMap<KanaType, HashMap<Character, Character>>();
        for (KanaType targetType : types) {
            if (targetType == KanaType.SEION) continue;
            HashMap<Character, Character> map = new HashMap<Character, Character>();
            for (int i = 0; i < seionChars.length(); ++i) {
                if (typeToChars.get(targetType).charAt(i) == seionChars.charAt(i)) continue;
                for (KanaType sourceType : types) {
                    char source = typeToChars.get(sourceType).charAt(i);
                    // Toggle.
                    char target = sourceType == targetType ?
                            seionChars.charAt(i) : typeToChars.get(targetType).charAt(i);
                    map.put(source, target);
                }
            }
            charMap.put(targetType, map);
        }
    }
    
    private OnKeyboardActionListener onKeyboardAction = new OnKeyboardActionListener() {
        
        public void swipeUp() {
        }
        
        public void swipeRight() {
        }
        
        public void swipeLeft() {
        }
        
        public void swipeDown() {
        }
        
        public void onText(CharSequence arg0) {
        }
        
        public void onRelease(int arg0) {
        }
        
        public void onPress(int arg0) {
        }
        
        public void onKey(int primaryCode, int[] keyCodes) {
            switch (primaryCode) {
            case 0:
                // Do nothing.
                break;
            case -400:
                modifyChar(KanaType.DAKUON);
                break;
            case -401:
                modifyChar(KanaType.HANDAKUON);
                break;
            case -402:
                modifyChar(KanaType.YOUON);
                break;
            case -403:
                queryField.setText("");    // will be removed
                break;
            case -100:
                backSpace();
                break;
            case -101:
                search();
                break;
            case -230:
                keyboardView.setKeyboard(
                        keyboardView.getKeyboard() == gojuonKeyboard ?
                                simpleAsciiKeyboard : gojuonKeyboard);
                break;
            default:
                queryField.append(new String(new char[] { (char)primaryCode }));
                break;
            }
        }
    };
    
    private void startOtherHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(Intent.createChooser(intent, null));
    }
    
    private void log(String format, Object... args) {
        Log.i("kantankensaku", String.format(format, (Object[])args));
    }
    
}
