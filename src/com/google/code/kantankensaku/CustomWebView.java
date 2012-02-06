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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;

public class CustomWebView extends WebView {

    public CustomWebView(Context context) {
        super(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public boolean onTouchEvent (MotionEvent ev) {
        try {
            // This method sometimes fails with:
            //   java.lang.IllegalArgumentException: pointerIndex out of range
            // in Android 3.2.1.
            return super.onTouchEvent(ev);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

}
