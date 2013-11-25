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
(function () {
    "use strict";

    window.Hippo = window.Hippo || {};

    if (!Hippo.PathHistory) {

        var PathHistoryImpl = function () {
            this.initialized = false;
        };

        PathHistoryImpl.prototype = {

            init: function (callback) {
                if (this.initialized) {
                    return;
                }

                var handleUrlChange = jQuery.proxy(function () {
                    this.onUrlChange();
                }, this);
                window.addEventListener('popstate', handleUrlChange);

                this.callback = callback;

                this.initialized = true;
            },

            setPath: function (path) {
                var url = '?path=' + path;
                history.pushState(null, null, url);
            },

            onUrlChange: function () {
                var path;
                path = this.getParameter('path');
                if (path === undefined) {
                    path = "/";
                }
                this.callback(path);
            },

            getParameter: function (name) {
                name = (new RegExp('[?&]' + encodeURIComponent(name) + '=([^&]*)')).exec(location.search);
                if (name) {
                    return decodeURIComponent(name[1]);
                } else {
                    return undefined;
                }
            }
        };

        Hippo.PathHistory = new PathHistoryImpl();
    }

}());
