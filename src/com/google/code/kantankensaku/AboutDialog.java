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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AboutDialog extends Dialog {

    private Context context;
    private TextView versionLabel;
    private Button closeButton;
    
    public AboutDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(context.getResources().getString(R.string.app_name) + "について");
        setContentView(R.layout.about);
        versionLabel = (TextView)findViewById(R.id.versionLabel);
        closeButton = (Button)findViewById(R.id.closeButton);
        closeButton.setOnClickListener(onCloseClick);
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionLabel.setText(info.versionName);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    private View.OnClickListener onCloseClick = new View.OnClickListener() {
        public void onClick(View v) {
            dismiss();
        }
    };
    
}
