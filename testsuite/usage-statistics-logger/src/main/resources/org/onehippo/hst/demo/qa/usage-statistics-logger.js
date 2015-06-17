/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

(function ($) {
  "use strict";

  var log;

  function createLog () {
    log = $('<div id=\"qa-usage-statistics-log\" class="hippo-global-hideme"></div>');
    log.appendTo(document.body);
  }

  function createCookie (name, value) {
    document.cookie = encodeURIComponent(name) + "=" + encodeURIComponent(value);
  }

  // mock the segment.com API
  window.analytics = {

    // mark the object as initialized so the real API won't be loaded
    initialize: true,

    ready: function (callback) {
      $(document).ready(function () {
        createLog();
        callback();
      })
    },

    page: function (pageName) {
      log.append('<div class="qa-usage-statistics-page">' + pageName + '</div>');
      console.log("TRACK PAGE: " + pageName);
    },

    track: function (name, params) {
      var paramsText = params ? JSON.stringify(params) : '';

      log.append(
        '<div class="qa-usage-statistics-event">' +
        '  <div class="qa-usage-statistics-event-name">' + name + '</div>' +
        '  <div class="qa-usage-statistics-event-params">' + paramsText + '</div>' +
        '</div>'
      );

      createCookie("qa-usage-statistics-event-name-last", name);
      createCookie("qa-usage-statistics-event-params-last", paramsText);

      console.log("TRACK EVENT: " + name + " " + paramsText);
    }

  }

}(jQuery));
