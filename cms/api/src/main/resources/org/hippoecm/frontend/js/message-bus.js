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
(function () {
  "use strict";

  window.Hippo = window.Hippo || {};

  Hippo.createMessageBus = function () {
    var subscriptions = {},
      monitors = [];

    function addCallback (list, callback, scope) {
      list.push({
        callback: callback,
        scope: scope || window
      });
    }

    function removeCallback (list, callback, scope) {
      var scopeParameter, i, len, entry;
      if (list === undefined) {
        return false;
      }
      scopeParameter = scope || window;
      for (i = 0, len = list.length; i < len; i++) {
        entry = list[i];
        if (entry.callback === callback && entry.scope === scopeParameter) {
          list.splice(i, 1);
          return true;
        }
      }
      return false;
    }

    function call (entries, args) {
      var i, len, entry;
      if (entries === undefined) {
        return true;
      }
      len = entries.length;
      for (i = 0; i < len; i++) {
        entry = entries[i];
        if (entry.callback.apply(entry.scope, args) === false) {
          return false;
        }
      }
      return true;
    }

    return {

      exception: function (msg, e) {
        this.publish('exception', msg, e);
      },

      publish: function (topic) {
        var argumentsWithoutTopic = Array.prototype.slice.call(arguments, 1);
        return call(subscriptions[topic], argumentsWithoutTopic) && call(monitors, arguments);
      },

      subscribe: function (topic, callback, scope) {
        if (subscriptions[topic] === undefined) {
          subscriptions[topic] = [];
        }
        addCallback(subscriptions[topic], callback, scope);
      },

      subscribeOnce: function (topic, callback, scope) {
        var self, interceptedCallback;

        self = this;

        interceptedCallback = function () {
          var result = callback.apply(scope, arguments);
          self.unsubscribe.call(self, topic, interceptedCallback, scope);
          return result;
        };

        this.subscribe(topic, interceptedCallback, scope);
      },

      unsubscribe: function (topic, callback, scope) {
        return removeCallback(subscriptions[topic], callback, scope);
      },

      unsubscribeAll: function () {
        subscriptions = {};
      },

      monitor: function (callback, scope) {
        return addCallback(monitors, callback, scope);
      },

      unmonitor: function (callback, scope) {
        return removeCallback(monitors, callback, scope);
      },

      clear: function () {
        this.unsubscribeAll();
        monitors = [];
      }

    };
  };

  Hippo.Events = Hippo.createMessageBus();

}());
