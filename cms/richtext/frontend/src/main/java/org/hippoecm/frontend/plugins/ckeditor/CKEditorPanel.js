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

    function removeHippoEditorFieldBorder(elementId) {
        $('#' + elementId).closest('.hippo-editor-field-subfield').css('border', '0');
    }

    function getWicketImportStyleElement(doc, styleCount) {
        var element = doc.getElementById('wicketimportstyle' + styleCount);
        if (element === undefined) {
            element = doc.createStyleSheet();
            element.owningElement.id = 'wicketimportstyle' + styleCount;
            element.owningElement.type = 'text/css';
        }
        return element;
    }

    function addCssImport(doc, styleCount, href) {
        var style = getWicketImportStyleElement(doc, styleCount),
            imports = style.styleSheet.imports,
            i, len;
        // check if href already imported by this stylesheet
        for (i = 0, len = imports.length; i < len; i++) {
            if (imports[i].href.indexOf(href) !== -1) {
                return false;
            }
        }
        // stylesheets in IE8 can have no more than 31 imports
        if (imports.length > 30) {
            return addCssImport(styleCount + 1, href);
        } else {
            style.styleSheet.addImport(href);
            return true;
        }
    }

    function addCssText(doc, cssStyleText) {
        var style = doc.createStyleSheet();
        style.cssText = cssStyleText;
        return style;
    }

    if (Hippo === undefined) {
        Hippo = {};
    }

    Hippo.createCKEditor = function(elementId, config) {
        // queue editor creation
        window.setTimeout(function() {
            try {
                var editor = CKEDITOR.replace(elementId, config);
                if (editor !== null) {
                    updateEditorElementWhenDataChanged(editor);
                    destroyEditorWhenElementIsDestroyed(editor, elementId);
                    removeHippoEditorFieldBorder(elementId);
                } else {
                    console.error("CKEditor instance with id '" + elementId + "' was not created");
                }
            } catch (exception) {
                console.error("Cannot create CKEditor instance with id '" + elementId + "'", exception);
            }
        }, DOM_MIN_TIMEOUT_MS);
    };

    if (Wicket.Browser.isIELessThan9()) {
        /*
          IE8 can only have up to 31 CSS style imports. Reuse the CMS trick to add more imports via
          @import statements in style tags (see org.hippoecm.frontend.CssImportingHeaderResponse).
         */
        CKEDITOR.dom.document.prototype.appendStyleSheet = function(cssFileUrl) {
            addCssImport(this.$, 0, cssFileUrl);
        };
        /*
          Fix broken appendStyleText implementation (the editor does not to load in IE8 because appendStyleText
          calls createStyleSheet with an empty string as argument, which throws an Error)
          TODO: check whether this workaround is still needed when CKEditor is upgraded to a version newer than 4.2
         */
        CKEDITOR.dom.document.prototype.appendStyleText = function(cssStyleText) {
            return addCssText(this.$, cssStyleText);
        };
    }

}(jQuery));
