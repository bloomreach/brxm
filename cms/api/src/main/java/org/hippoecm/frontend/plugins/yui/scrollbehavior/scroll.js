/*
 *  Copyright 2008-2013 Hippo.
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

YAHOO.namespace("hippo");

if (!YAHOO.hippo.ScrollStateSaver) {
    (function() {
        var Dom = YAHOO.util.Dom;

        YAHOO.hippo.ScrollStateSaver = function(cookiePrefix) {
            this.scrollElementId = null;
            this.cookiePrefix = cookiePrefix;
            var _this = this;
            Wicket.Ajax.registerPreCallHandler(function() {
                _this.saveScrollPosition();
            });
            Wicket.Ajax.registerPostCallHandler(function() {
                _this.loadScrollPosition();
            });
        };

        YAHOO.hippo.ScrollStateSaver.prototype = {
            setScrollElementId : function(scrollElementId) {
                this.scrollElementId = scrollElementId;
            },

            getScrollElement : function() {
                var scrollElement, unit;

                scrollElement = Dom.get(this.scrollElementId);
                if (!scrollElement) {
                    return null;
                }
                unit = YAHOO.hippo.LayoutManager.findLayoutUnit(scrollElement);
                if (!unit) {
                    return null;
                }
                return unit.body;
            },

            saveScrollPosition : function() {
                var scrollElement, offsetX, offsetY;

                scrollElement = this.getScrollElement();
                if (scrollElement === null) {
                    return;
                }
                offsetX = scrollElement.pageXOffset || scrollElement.scrollLeft;
                YAHOO.util.Cookie.set(this.cookiePrefix+"ScrollOffsetX", offsetX);
                offsetY = scrollElement.pageYOffset || scrollElement.scrollTop;
                YAHOO.util.Cookie.set(this.cookiePrefix+"ScrollOffsetY", offsetY);
            },

            loadScrollPosition : function() {
                var scrollElement, offsetX, offsetY;

                scrollElement = this.getScrollElement();
                if (scrollElement === null || scrollElement === undefined) {
                    return;
                }
                offsetX = YAHOO.util.Cookie.get(this.cookiePrefix+"ScrollOffsetX", Number);
                if (offsetX) {
                    scrollElement.scrollLeft = offsetX;
                }
                offsetY = YAHOO.util.Cookie.get(this.cookiePrefix+"ScrollOffsetY", Number);
                if (offsetY) {
                    scrollElement.scrollTop = offsetY;
                }
                YAHOO.util.Cookie.remove(this.cookiePrefix+"ScrollOffsetX");
                YAHOO.util.Cookie.remove(this.cookiePrefix+"ScrollOffsetY");
            }
        };
    }());

    YAHOO.register("ScrollStateSaver", YAHOO.hippo.ScrollStateSaver, {
        version: "1.0.0", build: "1"
    });
}