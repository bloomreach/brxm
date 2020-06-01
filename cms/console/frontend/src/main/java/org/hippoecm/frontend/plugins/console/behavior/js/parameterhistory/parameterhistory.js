/*
 * Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
(function ($, window, document, history) {
  "use strict";

  window.Hippo = window.Hippo || {};

  if (!Hippo.ParameterHistory) {

    var Lang = YAHOO.lang,
      knownParams = ['path=', 'uuid='],
      isSupported = history && Lang.isFunction(history.pushState),
      ParameterHistoryImpl,
      ParameterHistoryStub;

    ParameterHistoryImpl = function () {
      this.initialized = false;
      // Safari calls popstate on window.load, other browsers don't. To work around this we add a custom state
      // object when calling history.pushState and check for that object when a popstate event comes in. If the custom
      // state is not present the event is skipped.
      this.state = {HippoState: true};
    };

    ParameterHistoryImpl.prototype = {

      init: function (callback) {
        if (this.initialized) {
          return;
        }

        this.callback = callback;

        $(window).bind("popstate", $.proxy(function (e) {
          if (e.originalEvent && e.originalEvent.state !== null && e.originalEvent.state.HippoState) {
            this.onUrlChange();
          }
        }, this));

        this.onUrlChange();

        // Add custom state object to the page load history entry. This way navigating back using the 'back'
        // button will not cause a skip in the popstate handler (since the custom state object would not be found).
        // Note: This will not affect the state value for the first popstate callback on Safari, it is always null.
        history.replaceState(this.state, '', document.location.search);

        this.initialized = true;
      },

      // Change the current url into a 'path' variant. If replace is true, the current history entry is replaced with
      // the new url. This can be used to remove invalid entries from the history log, e.g. document no longer exists
      // or replace the UUID url with a corresponding path url.
      setPath: function (path, replace) {
        var url = this.getUrl('path', path);
        if (replace === true) {
          history.replaceState(this.state, '', url);
        } else {
          history.pushState(this.state, '', url);
        }
      },

      // Add or replace request parameter name=value. From the current query string, remove all known
      // parameters (reset), then add the new parameter.
      getUrl: function (name, value) {
        var url = document.location.search,
          urlParts = url.split('?'),
          params = urlParts.length < 2 ? [] : urlParts[1].split(/[&;]/g),
          namePart = encodeURIComponent(name) + '=',
          param = namePart + value,
          i;

        if (params.length === 0) {
          // no parameters found, construct a new query string
          return '?' + param;
        }
        else {
          // remove known parameters and add new one
          for (i = params.length; i-- > 0;) {
            if (this.isKnownParameter(params[i])) {
              params.splice(i, 1);
            }
          }
          params.push(param);
          return urlParts[0] + '?' + params.join('&');
        }
      },

      isKnownParameter: function (paramName) {
        var i;
        for (i = 0; i < knownParams.length; i++) {
          //string.startsWith
          if (paramName.lastIndexOf(knownParams[i], 0) !== -1) {
            return true;
          }
        }
        return false;
      },

      // Sync the current URL state with the server. If path or uuid are not in the URL, set request param path
      // with value '/'.
      onUrlChange: function () {
        var parameter;
        if (!Lang.isUndefined(parameter = this.getParameter('uuid'))) {
          this.callback(null, parameter);
        } else {
          if (Lang.isUndefined(parameter = this.getParameter('path'))) {
            parameter = '/';
            this.setPath(parameter, true);
          }
          this.callback(parameter);
        }
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

    // Fallback class for browsers that don't support the history API.
    ParameterHistoryStub = function () {
    };

    ParameterHistoryStub.prototype = {
      init: function () {
      },
      setPath: function () {
      }
    };

    Hippo.ParameterHistory = isSupported ? new ParameterHistoryImpl() : new ParameterHistoryStub();
  }

}(jQuery, window, document, history));
