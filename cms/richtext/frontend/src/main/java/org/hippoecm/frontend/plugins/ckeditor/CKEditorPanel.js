/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
(function($) {
    "use strict";

    // minimum delay for setTimeout() calls as defined in HTML5 spec
    var DOM_MIN_TIMEOUT_MS = 4;

    function updateEditorElementWhenDataChanged(editor) {
        var data = editor.getData();

        editor.on('change', function() {
            // only update the element when data really changed
            var newData = editor.getData();
            if (newData !== data) {
                data = newData;
                editor.updateElement();
            }
        });
    }

    function destroyEditorWhenElementIsDestroyed(editor, elementId) {
        var element = $('#' + elementId)[0];
        HippoAjax.registerDestroyFunction(element, editor.destroy, editor);
    }

    if (Hippo === undefined) {
        Hippo = {};
    }

    Hippo.createCKEditor = function(elementId, config) {
        // queue editor creation
        window.setTimeout(function() {
            var editor = CKEDITOR.replace(elementId, config);
            updateEditorElementWhenDataChanged(editor);
            destroyEditorWhenElementIsDestroyed(editor, elementId);
        }, DOM_MIN_TIMEOUT_MS);
    };

}(jQuery));
