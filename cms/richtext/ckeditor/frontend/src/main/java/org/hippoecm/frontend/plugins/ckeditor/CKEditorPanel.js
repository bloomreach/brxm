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
    var DOM_MIN_TIMEOUT_MS = 4,
        CKEDITOR_READY = new Hippo.Future(function(success) {
            var pollTimeoutMillis = DOM_MIN_TIMEOUT_MS;

            (function succeedWhenCKEditorIsLoaded() {
                if (typeof CKEDITOR.on === 'function') {
                    if (CKEDITOR.status === 'loaded') {
                        success();
                    } else {
                        CKEDITOR.on('loaded', success);
                    }
                } else {
                    // try again using exponential backoff
                    pollTimeoutMillis *= 2;
                    console.log("Waiting " + pollTimeoutMillis + " ms for CKEditor's event mechanism to load...");
                    window.setTimeout(succeedWhenCKEditorIsLoaded, pollTimeoutMillis);
                }
            }());
        });

    function isStyleSheetPartOfHead(cssPath) {
        var ss = document.styleSheets,
            i, length;
        for (i = 0, length = ss.length; i < length; i++) {
            if (ss[i].href === cssPath) {
                return true;
            }
        }
        return false;
    }

    function addCssToHead(cssPath) {
        $('head').append('<link rel="stylesheet" href="' + cssPath + '" type="text/css"/>');
    }

    function addCssToHeadOnce(cssPath) {
        if (!isStyleSheetPartOfHead(cssPath)) {
            addCssToHead(cssPath);
        }
    }

    function ensureDivAreaCompatibility(editor) {
        if (!editor.addContentsCss) {
            editor.addContentsCss = addCssToHeadOnce;
        }
    }

    function callFunctions(functionMap) {
        var keys = CKEDITOR.tools.objectKeys(functionMap),
            i, length;
        for (i = 0, length = keys.length; i < length; i++) {
            CKEDITOR.tools.callFunction(functionMap[keys[i]]);
        }
    }

    function callFunctionsOnFormSubmit(form, functionMap) {
        form.$.submit = CKEDITOR.tools.override(form.$.submit, function(originalSubmit) {
            return function() {
                callFunctions(functionMap);

                // For IE, the DOM submit function is not a function
                if (originalSubmit.apply) {
                    originalSubmit.apply(this);
                } else {
                    originalSubmit();
                }
            };
        });
    }

    function getFormSubmitListeners(editor) {
        var form = new CKEDITOR.dom.element(editor.element.$.form),
            submitListeners = form.getCustomData('submitListeners');

        if (submitListeners === null) {
            submitListeners = {};
            form.setCustomData('submitListeners', submitListeners);
            callFunctionsOnFormSubmit(form, submitListeners);
        }

        return submitListeners;
    }

    function setFormSubmitListener(editor, fn) {
        var submitListeners = getFormSubmitListeners(editor);
        submitListeners[editor.id] = CKEDITOR.tools.addFunction(fn);
    }

    function deleteFormSubmitListener(editor) {
        var form = new CKEDITOR.dom.element(editor.element.$.form),
            submitListeners = form.getCustomData('submitListeners');

        delete submitListeners[editor.id];
    }

    function updateEditorElementWhenFormIsSubmitted(editor) {
        setFormSubmitListener(editor, function() {
            if (editor.checkDirty()) {
                editor.updateElement();
            }
        });
    }

    function destroyEditorWhenElementIsDestroyed(editor) {
        HippoAjax.registerDestroyFunction(editor.element.$, function() {
            deleteFormSubmitListener(editor);
            editor.destroy();
        }, editor);
    }

    function removeHippoEditorFieldBorder(editor) {
        $(editor.element.$).closest('.hippo-editor-field-subfield').css('border', '0');
    }

    if (Hippo === undefined) {
        Hippo = {};
    }

    Hippo.createCKEditor = function(elementId, config) {
        CKEDITOR_READY.when(function() {
            // queue editor creation
            window.setTimeout(function() {
                try {
                    var editor = CKEDITOR.replace(elementId, config);
                    if (editor !== null) {
                        ensureDivAreaCompatibility(editor);
                        updateEditorElementWhenFormIsSubmitted(editor);
                        destroyEditorWhenElementIsDestroyed(editor);
                        removeHippoEditorFieldBorder(editor);
                    } else {
                        console.error("CKEditor instance with id '" + elementId + "' was not created");
                    }
                } catch (exception) {
                    console.error("Cannot create CKEditor instance with id '" + elementId + "'", exception);
                }
            }, DOM_MIN_TIMEOUT_MS);
        });
    };

    if (Wicket.Browser.isIE()) {
        CKEDITOR_READY.when(function() {
            /*
              Replace CKEditor's 'appendStyleText' method. IE chokes on the original because it calls createStyleSheet()
              with an empty string as argument. That throws an Error when the page is served by an HTTP server.
             */
            CKEDITOR.dom.document.prototype.appendStyleText = function(cssStyleText) {
                var style = this.$.createStyleSheet();
                style.cssText = cssStyleText;
                return style;
            };
        });
    }

}(jQuery));
