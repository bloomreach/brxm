/**
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function ($) {
  "use strict";

  Ext.namespace('Hippo.ChannelManager');

  var KEEPALIVE_END_POINT = '/cafebabe-cafe-babe-cafe-babecafebabe./keepalive',
    MINIMUM_TIMEOUT_MILLIS = 20 * 1000;

  Hippo.ChannelManager.Pinger = function (config) {

    var ajaxSettings = {
        headers: {
          'CMS-User': config.cmsUser,
          'Force-Client-Host': 'true'
        }
      },
      timeoutMillis = {};

    function ping (contextPath) {
      var pingPath = contextPath + '/' + config.composerRestMountPath + KEEPALIVE_END_POINT;

      function updateTimeout (response) {
        var keptAliveSeconds = parseInt(response.data, 10) || 0;
        if (keptAliveSeconds > 0) {
          timeoutMillis[contextPath] = Math.max((keptAliveSeconds - 60) * 1000, MINIMUM_TIMEOUT_MILLIS);
        }
      }

      function scheduleNextPing () {
        var nextTimeoutMillis = timeoutMillis[contextPath] || MINIMUM_TIMEOUT_MILLIS;
        window.setTimeout(function () {
          ping(contextPath);
        }, nextTimeoutMillis);
      }

      $.ajax(pingPath, ajaxSettings)
        .done(updateTimeout)
        .always(scheduleNextPing);
    }

    function run () {
      if (config.contextPaths) {
        config.contextPaths.forEach(ping);
      }
    }

    return {
      run: run
    };
  };

}(jQuery));