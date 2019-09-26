/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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

  var MAX_INACTIVE_INTERVAL_MS = parseInt("${maxInactiveIntervalMinutes}", 10) * 60 * 1000,
    AJAX_ATTR_SYSTEM_ACTIVITY = "${ajaxAttrSystemActivity}",
    lastActive = Date.now(),
    onInactiveCallbacks = [],
    onActiveCallbacks = [];

  Hippo.UserActivity = {

    isAjaxUserActivity: function (ajaxOptions) {
     return ajaxOptions[AJAX_ATTR_SYSTEM_ACTIVITY] !== true;
    },

    /**
     * Reports user activity and invokes all onActive callbacks
     */
    report: function () {
      lastActive = Date.now();
      invokeCallbacks(onActiveCallbacks, "Error while executing user activity callback");
    },

    /**
     * Registers the given onInactive callback. The callback will be invoked when no activity has been reported
     * during the last "${maxInactiveIntervalMinutes}" minutes.
     * @param callback
     */
    registerOnInactive: function(callback) {
      onInactiveCallbacks.push(callback);
    },

    /**
     * Registers the given onActive callback. The callback will be invoked when user activity is reported.
     * @param callback
     */
    registerOnActive: function(callback) {
      onActiveCallbacks.push(callback);
    },

    unInactive: function(callback) {
      var index = onInactiveCallbacks.indexOf(callback);
      if (index >= 0) {
        onInactiveCallbacks.splice(index, 1);
      }
    }

  };

  function monitorWicketAjaxUserCalls () {
    Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_BEFORE_SEND, function (attrs, jqXHR) {
      if (Hippo.UserActivity.isAjaxUserActivity(jqXHR)) {
        Hippo.UserActivity.report();
      }
    });
  }

  function invokeCallbacks(callbacks, errorMessage) {
    callbacks.forEach(function(callback) {
      try {
        callback();
      } catch (e) {
        console.warn(errorMessage, e, callback);
      }
    })
  }

  function observeInactivity() {
    var now = Date.now(),
      msSinceLastActivity = now - lastActive,
      msToNextCheck;

    if (msSinceLastActivity > MAX_INACTIVE_INTERVAL_MS) {
      invokeCallbacks(onInactiveCallbacks, "Error while executing user inactivity callback");
    } else {
      msToNextCheck = MAX_INACTIVE_INTERVAL_MS - msSinceLastActivity;
      window.setTimeout(observeInactivity, msToNextCheck);
    }
  }

  monitorWicketAjaxUserCalls();
  observeInactivity();

}());
