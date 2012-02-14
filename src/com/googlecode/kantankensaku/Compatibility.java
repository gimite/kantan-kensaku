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

import android.content.pm.ActivityInfo;
import android.os.Build;

/**
 * Provides constants/methods which depends on Android version.
 * @author Hiroshi Ichikawa
 */
public class Compatibility {

    /**
     * WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED in API Level 11
     */
    public static final int FLAG_HARDWARE_ACCELERATED = 0x01000000;
    
    /**
     * ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE in API Level 9
     */
    public static final int SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6;

    public static int getScreenOrientationSensorLandscapeOrLandscape() {
        if (Build.VERSION.SDK_INT >= 9) {
            return SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        } else {
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
    }
    
}
