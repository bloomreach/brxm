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

  var EXTERNAL_SCRIPT_URL = window.location.protocol + '//' + '${externalScriptUrl}',
    EXTERNAL_LOAD_TIMEOUT_MS = 10000,
    eventQueue = [];

  function queueEvent (topic, params) {
    eventQueue.push({
      name: topic,
      params: params
    });
  }

  function loadExternalScript(done) {
    $.ajax(EXTERNAL_SCRIPT_URL, {
      cache: true,
      timeout: EXTERNAL_LOAD_TIMEOUT_MS,
      dataType: 'script',
      complete: done
    });
  }

  Hippo.Events.monitor(queueEvent);

  loadExternalScript(function() {
    Hippo.Events.unmonitor(queueEvent);
    Hippo.Events.publish('start-usage-statistics', eventQueue);
    eventQueue = null;
  });

}(jQuery));
