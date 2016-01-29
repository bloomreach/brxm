/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
  'use strict';

  function createMessageBus() {
    var subscriptions = {};

    function addCallback(list, callback, scope) {
      list.push({
        callback: callback,
        scope: scope || window,
      });
    }

    function call(entries, args) {
      var i;
      var len;
      var entry;

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

      publish: function (topic) {
        var argumentsWithoutTopic = Array.prototype.slice.call(arguments, 1);
        return call(subscriptions[topic], argumentsWithoutTopic);
      },

      subscribe: function (topic, callback, scope) {
        if (subscriptions[topic] === undefined) {
          subscriptions[topic] = [];
        }

        addCallback(subscriptions[topic], callback, scope);
      },

    };
  }

  function mockHost() {
    var parentIFramePanel;
    window.APP_CONFIG = {};
    window.APP_TO_CMS = createMessageBus();
    window.CMS_TO_APP = createMessageBus();

    parentIFramePanel = {
      initialConfig: {
        iframeConfig: window.APP_CONFIG,
      },
      iframeToHost: window.APP_TO_CMS,
      hostToIFrame: window.CMS_TO_APP,
    };

    window.history.replaceState({}, document.title, window.location.href + '?proCache4321&parentExtIFramePanelId=ext-42&antiCache=1234');

    window.parent = {
      Ext: {
        getCmp: function () {
          return parentIFramePanel;
        },
      },
    };

    spyOn(window.CMS_TO_APP, 'subscribe').and.callThrough();
    spyOn(window.APP_TO_CMS, 'publish').and.callThrough();
  }

  function mockFallbackTranslations() {
    module('hippo-cm', function ($provide, $translateProvider) {
      $translateProvider.translations('en', {});
    });
  }

  beforeEach(mockHost);
  beforeEach(mockFallbackTranslations);

}());
