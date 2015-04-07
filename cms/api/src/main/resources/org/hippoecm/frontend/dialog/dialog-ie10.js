/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// IE10 needs a width in pixels on the .hippo-dialog.bottom-left panel in order to render
// items that have overflow:hidden correctly.
function fixDialogForIE10() {

    "use strict";

    var bottom = $('.hippo-dialog-bottom'),
        left = $('.hippo-dialog-bottom-left'),
        right = $('.hippo-dialog-bottom-right');

    console.log('test ' + left.length);

    if (left.length) {
        left.width(bottom.width() - right.width());
    }
}


