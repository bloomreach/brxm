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

/**
 * IE-specific fixes.
 */
(function() {
    "use strict";

    // IE9 and earlier cannot handle more than 31 stylesheets, as described at http://support.microsoft.com/kb/262161
    var MAX_IE_STYLESHEETS = 31;

    /**
     * Adds a stylesheet URL as an @import statement to a 'style' tag in the head. The amount of @import statements
     * per 'style' tag is limited to MAX_IE_STYLESHEETS.
     *
     * @param styleCount the internal zero-based counter of 'style' tags that have already been created
     * @param cssFileUrl the stylesheet URL to add
     * @returns {*}
     */
    function addCssImport(styleCount, cssFileUrl) {
        var styleId, style, sheet, imports, importsLength, urlWithoutDotSlash, i;

        styleId = "wicketimportstyle" + styleCount;
        style = window.document.getElementById(styleId);

        if (style) {
            sheet = style.styleSheet || style.sheet;
            imports = sheet.imports;
            importsLength = imports.length;

            // Check if href already imported by this stylesheet. Compare the substring after the "./" because the URL
            // of the added import will be fully qualified so the exact href value won't match
            urlWithoutDotSlash = cssFileUrl.indexOf('./') === 0 ? cssFileUrl.substring(2) : cssFileUrl;
            for (i = 0; i < importsLength; i++) {
                if (imports[i].href.indexOf(urlWithoutDotSlash) !== -1) {
                    return false;
                }
            }

            // Stylesheets in IE can have no more than 31 imports
            if (importsLength >= MAX_IE_STYLESHEETS) {
                return addCssImport(styleCount + 1, cssFileUrl);
            }

            sheet.addImport(cssFileUrl);
        } else {
            style = window.document.createStyleSheet();
            style.owningElement.id = styleId;
            style.owningElement.type = 'text/css';
            style.addImport(cssFileUrl);
        }
        return true;
    }

    window.Hippo = window.Hippo || {};

    window.Hippo.IE = {

        /**
         * Adds a CSS style sheet to the head of the document.
         *
         * @param cssFileUrl the URL of the CSS file
         */
        addStyleSheet: function(cssFileUrl) {
            return addCssImport(0, cssFileUrl);
        }

    };

    /**
     * Patch Wicket: external style sheets added in Ajax calls should be added in the Hippo-specific way
     * to prevent that the number of stylesheets in the head grows larger than MAX_IE_STYLESHEETS.
     */
    jQuery.extend(true, Wicket.Head.Contributor, {

        processLink: function(context, node) {
            context.steps.push(function(notify) {
                var href = node.getAttribute("href"),
                    added, css, img, notifyCalled;

                // if the element is already in head, skip it
                if (Wicket.Head.containsElement(node, "href")) {
                    notify();
                    return;
                }

                // add the link href in an IE-specific way
                added = window.Hippo.IE.addStyleSheet(href);
                if (!added) {
                    notify();
                    return;
                }

                // cross browser way to check when the css is loaded
                // taked from http://www.backalleycoder.com/2011/03/20/link-tag-css-stylesheet-load-event/
                // this makes a second GET request to the css but it gets it either from the cache or
                // downloads just the first several bytes and realizes that the MIME is wrong and ignores the rest
                img = window.document.createElement('img');
                notifyCalled = false;
                img.onerror = function() {
                    if (!notifyCalled) {
                        notifyCalled = true;
                        notify();
                    }
                };
                img.src = href;
                if (img.complete) {
                    if (!notifyCalled) {
                        notifyCalled = true;
                        notify();
                    }
                }
            });
        }
    });

}());
