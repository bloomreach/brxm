/*
 * Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
 * Provides an interface for components to register on handling WicketAjax
 * component update
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hashmap
 * @module hippoajax
 * @beta
 */

YAHOO.namespace('hippo');

var HippoAjax = YAHOO.hippo.HippoAjax;

if (!YAHOO.hippo.HippoAjax) { // Ensure only one hippo ajax exists
  (function () {
    "use strict";

    var Dom = YAHOO.util.Dom,
      Lang = YAHOO.lang;

    YAHOO.hippo.HippoAjaxImpl = function () {
    };

    YAHOO.hippo.HippoAjaxImpl.prototype = {
      prefix: 'hippo-destroyable-',
      callbacks: new YAHOO.hippo.HashMap(),
      _scrollbarWidth: null,

      loadJavascript: function (url, callback, scope) {
        var evt = "onload",
          element = document.createElement("script");
        element.type = "text/javascript";
        element.src = url;
        if (callback) {
          element[evt] = function () {
            callback.call(scope);
            element[evt] = null;
          };
        }
        document.getElementsByTagName("head")[0].appendChild(element);
      },

      getScrollbarWidth: function () {
        if (this._scrollbarWidth === null) {
          this._scrollbarWidth = this._calculateScrollbarWidth();
        }
        return this._scrollbarWidth;
      },

      getScrollbarHeight: function () {
        return this.getScrollbarWidth();
      },

      _calculateScrollbarWidth: function() {
        var box, width;

        box = document.createElement('div');
        box.style.position = 'fixed';
        box.style.left = '0px';
        box.style.visibility = 'hidden';
        box.style.overflowY = 'scroll';
        document.body.appendChild(box);

        width = box.getBoundingClientRect().right;

        document.body.removeChild(box);

        return width;
      },

      registerDestroyFunction: function (el, func, context, args) {
        var id = this.prefix + Dom.generateId();
        el.HippoDestroyID = id;
        if (!Lang.isArray(args)) {
          args = [args];
        }
        this.callbacks.put(id, {func: func, context: context, args: args});
      },

      callDestroyFunction: function (id) {
        if (this.callbacks.containsKey(id)) {
          var callback = this.callbacks.remove(id);
          callback.func.apply(callback.context, callback.args);
        }
      },

      cleanupElement: function (el) {
        var els, i, len;

        els = Dom.getElementsBy(function (node) {
          return node.hasOwnProperty('HippoDestroyID');
        }, null, el);

        for (i = 0, len = els.length; i < len; i++) {
          this.callDestroyFunction(els[i].HippoDestroyID);
        }

        if (!Lang.isUndefined(el.HippoDestroyID)) {
          this.callDestroyFunction(el.HippoDestroyID);
        }
      }
    };

    YAHOO.hippo.HippoAjax = HippoAjax = new YAHOO.hippo.HippoAjaxImpl();

    Wicket.Event.subscribe(Wicket.Event.Topic.DOM_NODE_REMOVING,
      function (jqEvent, el, jqXHR, errorThrown, textStatus) {
        if (el !== null && el !== undefined) {
          HippoAjax.cleanupElement(el);
          YAHOO.util.Event.purgeElement(el, true);
        }
      }
    );
  }());

  YAHOO.register("hippoajax", YAHOO.hippo.HippoAjax, {
    version: "2.8.1", build: "19"
  });
}
