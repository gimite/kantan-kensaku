# coding: utf-8

# Copyright 2011 Google Inc. All Rights Reserved.
# Author: Hiroshi Ichikawa
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys

def Generate(type, size):
  
  f = open("res/xml/%s_keyboard_%s.xml" % (type, size), "w")

  if size == "small":
    kana_x_metrics = [(7, 56), (10, 56), (10, 56), (36, 67), (13, 67), (14, 67), (13, 67), (13, 67), (13, 67), (14, 67), (13, 67), (13, 67), (12, 67),]
    ascii_x_metrics = [(35, 67), (13, 67), (13, 67), (14, 67), (13, 67), (13, 67), (12, 67), ]
    key_height = 62
    vertical_gap = 8
  else:
    kana_x_metrics = [(8, 71), (12, 71), (12, 70), (45, 84), (16, 84), (18, 83), (17, 83), (17, 83), (17, 83), (18, 83), (17, 83), (17, 83), (16, 83), ]
    ascii_x_metrics = [(119, 84), (16, 84), (16, 84), (18, 83), (17, 83), (17, 83), (15, 84), ]
    key_height = 80
    vertical_gap = 11
  
  if type == "gojuon":
    key_labels = [
        u"123わらやまはなたさかあ",
        u"456　り　みひにちしきい",
        u"789をるゆむふぬつすくう",
        u" 0 　れ　めへねてせけえ",
        u"   んろよもほのとそこお",
    ]
    x_metrics = kana_x_metrics
  
  else:
    key_labels = [
        u"123ABCDEFG",
        u"456HIJKLMN",
        u"789OPQRSTU",
        u" 0 VWXYZ  ",
        u"          ",
    ]
    num_width = 0
    for i in xrange(0, 3):
      num_width += kana_x_metrics[i][0] + kana_x_metrics[i][1]
    x_metrics = (kana_x_metrics[0:3] +
                 [((300 - num_width) + ascii_x_metrics[0][0], ascii_x_metrics[0][1])] +
                 ascii_x_metrics[1:])
  
  print >>f, '<!--'
  print >>f, '  Copyright 2011 Google Inc. All Rights Reserved.'
  print >>f, '  Author: Hiroshi Ichikawa'
  print >>f, ''
  print >>f, '  Licensed under the Apache License, Version 2.0 (the "License");'
  print >>f, '  you may not use this file except in compliance with the License.'
  print >>f, '  You may obtain a copy of the License at'
  print >>f, ''
  print >>f, '      http://www.apache.org/licenses/LICENSE-2.0'
  print >>f, ''
  print >>f, '  Unless required by applicable law or agreed to in writing, software'
  print >>f, '  distributed under the License is distributed on an "AS IS" BASIS,'
  print >>f, '  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.'
  print >>f, '  See the License for the specific language governing permissions and'
  print >>f, '  limitations under the License.'
  print >>f, '-->'
  print >>f, '<Keyboard xmlns:android="http://schemas.android.com/apk/res/android"'
  print >>f, '    android:horizontalGap="7px"'
  print >>f, '    android:verticalGap="%dpx"' % vertical_gap
  print >>f, '    android:keyWidth="67px"'
  print >>f, '    android:keyHeight="%dpx"' % key_height
  print >>f, '    >'
  
  for y in xrange(len(key_labels)):
    print >>f, '    <Row>'
    for x in xrange(len(key_labels[y])):
      ch = key_labels[y][x]
      if ch in (u" ", u"　"):
        code = 0
      else:
        code = ord(ch)
      (gap, width) = x_metrics[x]
      if y == len(key_labels) - 1:
        if x == 0:
          width = x_metrics[0][1] + x_metrics[1][0] + x_metrics[1][1] + x_metrics[2][0] + x_metrics[2][1]
          code = ord(u" ")
        elif x in [1, 2]:
          continue
      if type == 'gojuon' or x < 3:
        icon = '@drawable/button_%s_kana_%d_%d' % (size, x, y)
      elif y < 4:
        icon = '@drawable/button_%s_ascii_%d_%d' % (size, x - 3, y)
      else:
        icon = '@drawable/button_%s_ascii_6_3' % size  # empty
      if code == ord(u" "):
        popup_id = "@drawable/popup_space"
      else:
        popup_id = "@drawable/popup_char_%04x" % code
      print >>f, ('        <Key android:codes="%d" android:keyIcon="%s" '
             'android:keyWidth="%dpx" android:horizontalGap="%dpx" '
             'android:iconPreview="%s"/>'
             % (code, icon, width, gap, popup_id))
    print >>f, '    </Row>'
  
  if size == "small":
    bottom_x_metrics =[(7, 188), (38, 143), (5, 143), (5, 143), (5, 143), (5, 194), ]
  else:
    bottom_x_metrics = [(8, 236), (47, 179), (6, 179), (6, 179), (6, 179), (6, 243), ]
  
  if type == "gojuon":
    specials = [
        [-230, "ascii"],  # ASCII mode
        [ord(u"ー"), "dash"],
        [-400, "dakuon"],
        [-401, "handakuon"],
        [-402, "youon"],
        [-100, "backspace"],
    ]
  else:
    specials = [
        [-230, "kana"],
        [0, None],
        [0, None],
        [0, None],
        [0, None],
        [-100, "backspace"],
    ]
   
  type2 = 'kana' if type == 'gojuon' else 'ascii'
  print >>f, '    <Row>'
  for i in xrange(len(specials)):
    (code, name) = specials[i]
    if not name:
      name = 'char_0000'
    print >>f, ('        <Key android:codes="%d" android:keyIcon="@drawable/button_%s_%s_bottom_%d" '
           'android:keyWidth="%dpx" android:horizontalGap="%dpx" android:iconPreview="@drawable/popup_%s"/>'
           % (code, size, type2, i, bottom_x_metrics[i][1], bottom_x_metrics[i][0], name))
  print >>f, '    </Row>'
  print >>f, '</Keyboard>'
  
  f.close()


for type in ["gojuon", "ascii"]:
  for size in ["small", "big"]:
    Generate(type, size)
