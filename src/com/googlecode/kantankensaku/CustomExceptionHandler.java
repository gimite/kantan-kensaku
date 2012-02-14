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

package com.googlecode.kantankensaku;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

import android.os.Environment;

public class CustomExceptionHandler implements UncaughtExceptionHandler {

    private static boolean handlerIsSet = false;
    private UncaughtExceptionHandler defaultHandler;
    
    public static void setHandler() {
        if (handlerIsSet) return;
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
        handlerIsSet = true;
    }

    public CustomExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
        if (Config.SAVE_CRASH_REPORT) {
            String timestamp = new Date().toString();
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            String stacktrace = result.toString();
            printWriter.close();
            String path = Environment.getExternalStorageDirectory().getPath() + "/" +
                    "com.google.code.kantankensaku." + timestamp + ".stacktrace";
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(path));
                writer.write(stacktrace);
                writer.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        defaultHandler.uncaughtException(t, e);
    }

}
