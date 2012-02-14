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

import java.util.HashMap;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * KeyboardView which provides gojuon and ASCII keyboard.
 * @author Hiroshi Ichikawa
 */
public class CustomKeyboardView extends KeyboardView {

    public enum Mode {
        GOJUON, ASCII,
    }
    
    private enum KanaType {
        SEION, DAKUON, HANDAKUON, YOUON,
    }
    
    private TextView targetField;
    private SizedResources sizedResources;
    private Mode mode;
    private HashMap<Mode, Keyboard> keyboards;
    
    // modificationMap[type][ch1] == ch2 means:
    // When the user hits type button, ch1 is converted to ch2.
    // e.g. modificationMap[KanaType.DAKUON]["か"] == "が"
    //      modificationMap[KanaType.DAKUON]["が"] == "か"  // toggle
    private HashMap<KanaType, HashMap<Character, Character>> modificationMap;
    
    public CustomKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    /**
     * Needs to call this just once in Activity.onCreate().
     * @param activity The activity which this view belongs to.
     * @param targetField The TextView edited by this keyboard.
     */
    public void initialize(Activity activity, TextView targetField) {
        createModificationMap();
        this.targetField = targetField;
        sizedResources = new SizedResources(activity);
        keyboards = new HashMap<Mode, Keyboard>();
        keyboards.put(Mode.GOJUON, new Keyboard(activity, sizedResources.gojuonKeyboard));
        keyboards.put(Mode.ASCII, new Keyboard(activity, sizedResources.asciiKeyboard));
        setOnKeyboardActionListener(onKeyboardAction);
        setMode(Mode.GOJUON);
    }
    
    /**
     * Gets the current mode.
     */
    public Mode getMode() { return mode; }
    
    /**
     * Sets the mode.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        setKeyboard(keyboards.get(mode));
    }

    /**
     * Event handler for keyboard action.
     */
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
            case -100:
                backSpace();
                break;
            case -230:
                setMode(mode == Mode.GOJUON ? Mode.ASCII : Mode.GOJUON);
                break;
            default:
                targetField.append(new String(new char[] { (char)primaryCode }));
                break;
            }
        }
    };
    
    /**
     * Creates modificationMap.
     */
    private void createModificationMap() {
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
        modificationMap = new HashMap<KanaType, HashMap<Character, Character>>();
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
            modificationMap.put(targetType, map);
        }
    }
    
    /**
     * Applies dakuon/handakuon/youon modification to the last character.
     * @param type
     */
    private void modifyChar(KanaType type) {
        String text = targetField.getText().toString();
        if (text.length() == 0) return;
        Character converted = modificationMap.get(type).get(text.charAt(text.length() - 1));
        if (converted != null) {
            targetField.setTextKeepState(text.substring(0, text.length() - 1) + converted);
        }
    }
    
    /**
     * Applies backspace.
     */
    private void backSpace() {
        String text = targetField.getText().toString();
        if (text.length() > 0) {
            targetField.setTextKeepState(text.substring(0, text.length() - 1));
        }
    }
    
}
