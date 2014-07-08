/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    "use strict";

    var DOM_MIN_TIMEOUT_MILLIS = 4,     // minimum delay for setTimeout() calls as defined in HTML5 spec
            DEFAULT_THROTTLE_MILLIS = 2000,
            timer = null;

    function save(data, callbackUrl, componentId) {
        Wicket.Ajax.post({
            u: callbackUrl,
            c: componentId,
            ep: {
                data: data
            }
        });
    }

    /**
     * Save the given data after <millis> milliseconds. A previous delayed save will be cancelled.
     */
    function delaySave(millis, data, callbackUrl, componentId) {
        clearTimeout(timer);
        timer = setTimeout(function () {
            save(data, callbackUrl, componentId);
        }, millis);
    }

    CKEDITOR.plugins.add('hippoautosave', {

        init: function (editor) {
            var callbackUrl = editor.config.hippoautosave_callbackUrl,
                    throttleMillis = editor.config.hippoautosave_throttleMillis || DEFAULT_THROTTLE_MILLIS,
                    editorData = editor.getData();

            function scheduleSave(newData) {
                var id = editor.element.getId();
                // only save data over <throttleMillis> milliseconds when it really changed
                if (newData !== editorData) {
                    editorData = newData;
                    delaySave(throttleMillis, editorData, callbackUrl, id);
                }
            }

            editor.on('change', function() {
                scheduleSave(editor.getData());
            });

            // Detect changes in source mode.
            editor.on('mode', function () {
                if (editor.mode !== 'source') {
                    return;
                }
                var codeMirror = window["codemirror_" + editor.id];
                if (codeMirror) {
                    codeMirror.on('change', function() {
                        scheduleSave(codeMirror.getValue());
                    });
                }
            });

            editor.on('blur', function () {
                // save directly; the data is probably updated already via the 'change'
                // event, but check again to be sure
                var newData = editor.getData(),
                    id = editor.element.getId();

                if (newData !== editorData) {
                    editorData = newData;
                }
                delaySave(DOM_MIN_TIMEOUT_MILLIS, editorData, callbackUrl, id);
            });
        }

    });
}());
