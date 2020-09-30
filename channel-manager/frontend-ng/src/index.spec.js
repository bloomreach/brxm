/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import 'angular';
import 'angular-mocks';

function createMessageBus($window) {
  const subscriptions = {};

  function addCallback(list, callback, scope) {
    list.push({
      callback,
      scope: scope || $window,
    });
  }

  function removeCallback(list, callback, scope) {
    if (list === undefined) {
      return false;
    }
    const scopeParameter = scope || $window;
    for (let i = 0; i < list.length; i += 1) {
      const entry = list[i];
      if (entry.callback === callback && entry.scope === scopeParameter) {
        list.splice(i, 1);
        return true;
      }
    }
    return false;
  }

  function call(entries, args) {
    if (entries === undefined) {
      return true;
    }

    const len = entries.length;
    for (let i = 0; i < len; i += 1) {
      const entry = entries[i];
      if (entry.callback.apply(entry.scope, args) === false) {
        return false;
      }
    }

    return true;
  }

  return {
    publish(topic, ...args) {
      return call(subscriptions[topic], args);
    },

    subscribe(topic, callback, scope) {
      if (subscriptions[topic] === undefined) {
        subscriptions[topic] = [];
      }
      addCallback(subscriptions[topic], callback, scope);
    },

    subscribeOnce(topic, callback, scope) {
      const interceptedCallback = (...args) => {
        const result = callback.apply(scope, args);
        this.unsubscribe(topic, interceptedCallback, scope);
        return result;
      };
      this.subscribe(topic, interceptedCallback, scope);
    },

    unsubscribe(topic, callback, scope) {
      return removeCallback(subscriptions[topic], callback, scope);
    },
  };
}

function mockHost() {
  angular.mock.module(($provide) => {
    const $window = {
      addEventListener: window.addEventListener.bind(window),
      document: window.document,
      confirm: window.confirm.bind(window),
      getComputedStyle: window.getComputedStyle.bind(window),
      navigator: window.navigator,
      dispatchEvent: window.dispatchEvent.bind(window),
      postMessage: window.postMessage.bind(window),
      removeEventListener: window.removeEventListener.bind(window),
      scrollX: window.scrollX,
      scrollY: window.scrollY,
    };

    $window.APP_CONFIG = {
      antiCache: '123',
    };
    $window.APP_TO_CMS = createMessageBus($window);
    $window.CMS_TO_APP = createMessageBus($window);

    const parentIFramePanel = {
      initialConfig: {
        iframeConfig: $window.APP_CONFIG,
      },
      iframeToHost: $window.APP_TO_CMS,
      hostToIFrame: $window.CMS_TO_APP,
    };

    $window.location = {
      origin: 'http://localhost:8080',
      search: '?proCache4321&parentExtIFramePanelId=ext-42&antiCache=1234',
    };

    $window.top = $window;
    $window.parent = {
      document: {
        documentElement: {
          hasClass() {
            return false;
          },
        },
      },
      location: {
        origin: 'http://localhost:8080',
        pathname: '/test/',
      },
      Ext: {
        getCmp() {
          return parentIFramePanel;
        },
      },
      Hippo: {
        Events: createMessageBus($window),
        Targeting: {
          HttpProxy: {},
          PropertiesEditor: {},
        },
        Workflow: {
          invoke: () => Promise.resolve(),
        },
      },
    };

    $provide.value('$window', $window);
  });
}

function mockFallbackTranslations() {
  angular.mock.module('hippo-cm', ($translateProvider) => {
    $translateProvider.translations('en', {});
  });
}

function mockMdIcon() {
  angular.mock.module('hippo-cm', ($provide) => {
    // mock md-icon directive to ignore GET requests fetching SVG files
    $provide.factory('mdIconDirective', () => angular.noop);
  });
}

function disableStateTransitionErrors() {
  angular.mock.module('hippo-cm', ($uiRouterProvider) => {
    $uiRouterProvider.stateService.defaultErrorHandler(angular.noop);
  });
}

function mockNavappConnection() {
  angular.mock.module('hippo-cm', ($provide) => {
    const parentApiMock = {
      updateNavLocation: () => Promise.resolve(),
    };

    $provide.constant('NavappCommunication', {
      connectToParent: () => Promise.resolve(parentApiMock),
    });
  });
}

beforeEach(mockHost);
beforeEach(mockFallbackTranslations);
beforeEach(mockMdIcon);
beforeEach(disableStateTransitionErrors);
beforeEach(mockNavappConnection);

const context = require.context('./app', true, /\.js$/);
context.keys().forEach(context);
