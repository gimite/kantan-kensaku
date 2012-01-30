// Copyright 2012 Google Inc. All Rights Reserved.
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

import android.app.Activity;
import android.content.Context;
import android.view.Display;

public class SizedResources {
    
    public enum Size {
        BIG, SMALL, AUTO,
    }
    
    public static int BIG_MIN_DISPLAY_WIDTH = 1280;
    public static int BIG_MIN_DISPLAY_HEIGHT = 700;
    public static int SMALL_MIN_DISPLAY_WIDTH = 1024;
    public static int SMALL_MIN_DISPLAY_HEIGHT = 600;
    
    public boolean supported;
    public int homeLayout;
    public int browserLayout;
    public int gojuonKeyboard;
    public int asciiKeyboard;
    public int prevButtonEnabledDrawable;
    public int prevButtonDisabledDrawable;
    public int selectedCandidateDrawable;
    public int candidatePadding;
    public int candidateTextSize;
    
    public SizedResources(Activity activity) {
        Size size;
        if (Config.RESOURCE_SIZE == Size.AUTO) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            if (display.getWidth() >= BIG_MIN_DISPLAY_WIDTH &&
                    display.getHeight() >= BIG_MIN_DISPLAY_HEIGHT) {
                supported = true;
                size = Size.BIG;
            } else if (display.getWidth() >= SMALL_MIN_DISPLAY_WIDTH &&
                    display.getHeight() >- SMALL_MIN_DISPLAY_HEIGHT) {
                supported = true;
                size = Size.SMALL;
            } else {
                supported = false;
                size = Size.SMALL;
            }
        } else {
            supported = true;
            size = Config.RESOURCE_SIZE;
        }
        
        switch (size) {
        case BIG:
            homeLayout = R.layout.home_big;
            browserLayout = R.layout.browser_big;
            gojuonKeyboard = R.xml.gojuon_keyboard;
            asciiKeyboard = R.xml.simple_ascii_keyboard;
            prevButtonEnabledDrawable = R.drawable.button_big_result_prev;
            prevButtonDisabledDrawable = R.drawable.button_big_result_prev_disabled;
            selectedCandidateDrawable = R.drawable.big_selected_candidate;
            candidatePadding = 35;
            candidateTextSize = 44;
            break;
        case SMALL:
            homeLayout = R.layout.home_small;
            browserLayout = R.layout.browser_small;
            gojuonKeyboard = R.xml.gojuon_keyboard_small;
            asciiKeyboard = R.xml.ascii_keyboard_small;
            prevButtonEnabledDrawable = R.drawable.button_result_prev;
            prevButtonDisabledDrawable = R.drawable.button_result_prev_disabled;
            selectedCandidateDrawable = R.drawable.selected_candidate;
            candidatePadding = 28;
            candidateTextSize = 29;
            break;
        default:
            throw new RuntimeException("unknown size");
        }
    }

}
