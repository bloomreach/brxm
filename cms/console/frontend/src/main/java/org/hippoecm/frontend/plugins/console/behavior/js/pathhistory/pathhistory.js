"use strict";

/**
 * @description
 * <p>
 * Todo
 * </p>
 * @namespace YAHOO.hippo
 * @requires hippoajax, hashmap, hippodom
 * @module pathhistory
 */


YAHOO.namespace('hippo');

if (!YAHOO.hippo.PathHistory) {

    var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, HippoAjax = YAHOO.hippo.HippoAjax;

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
            var path = this.getParameter('path');
            if (Lang.isUndefined(path)) {
                path = "/";
            }
            var url = this.url + "&path=" + path;
            this.callback(url);
        },

        getParameter : function(name){
            if(name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search))
                return decodeURIComponent(name[1]);
        }
    };

    YAHOO.hippo.PathHistory = new YAHOO.hippo.PathHistoryImpl();
    YAHOO.register("pathhistory", YAHOO.hippo.PathHistory, {
        version : "2.7.0", build : "1799"
    });
}

