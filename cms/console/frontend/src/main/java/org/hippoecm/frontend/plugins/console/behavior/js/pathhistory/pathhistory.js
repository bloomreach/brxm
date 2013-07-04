/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
 * @description
 * <p>
 * Todo
 * </p>
 * @namespace YAHOO.hippo
 * @requires hippoajax, hashmap, hippodom
 * @module pathhistory
 */
(function() {
    "use strict";

    YAHOO.namespace('hippo');

    if (!YAHOO.hippo.PathHistory) {

        var Lang = YAHOO.lang;

        YAHOO.hippo.PathHistoryImpl = function() {
            this.initialized = false;
        };

        YAHOO.hippo.PathHistoryImpl.prototype = {

            init : function(url, callback) {
                if (this.initialized) {
                    return;
                }

                var handleUrlChange = jQuery.proxy(function() {
                    this.onUrlChange();
                }, this);
                window.addEventListener('popstate', handleUrlChange);

                this.url = url;
                this.callback = callback;

                this.initialized = true;
            },

            setPath : function(path) {
                var url = '?path=' + path;
                history.pushState(null, null, url);
            },

            onUrlChange : function() {
                var path, url;
                path = this.getParameter('path');
                if (Lang.isUndefined(path)) {
                    path = "/";
                }
                url = this.url + "&path=" + path;
                this.callback(url);
            },

            getParameter : function(name) {
                name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search);
                if (name) {
                    return decodeURIComponent(name[1]);
                }
            }
        };

        YAHOO.hippo.PathHistory = new YAHOO.hippo.PathHistoryImpl();
        YAHOO.register("pathhistory", YAHOO.hippo.PathHistory, {
            version : "2.7.0", build : "1799"
        });
    }

}());
