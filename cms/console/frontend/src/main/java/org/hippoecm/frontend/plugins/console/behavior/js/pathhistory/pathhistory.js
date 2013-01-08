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

                var handleUrlChange = function() {
                    this.onUrlChange();
                }.bind(this);
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
