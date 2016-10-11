import 'angular';
import 'angular-mocks';

function createMessageBus() {
  const subscriptions = {};

  function addCallback(list, callback, scope) {
    list.push({
      callback,
      scope: scope || window,
    });
  }

  function removeCallback(list, callback, scope) {
    if (list === undefined) {
      return false;
    }
    const scopeParameter = scope || window;
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
  window.APP_CONFIG = {
    antiCache: '123',
  };
  window.APP_TO_CMS = createMessageBus();
  window.CMS_TO_APP = createMessageBus();

  const parentIFramePanel = {
    initialConfig: {
      iframeConfig: window.APP_CONFIG,
    },
    iframeToHost: window.APP_TO_CMS,
    hostToIFrame: window.CMS_TO_APP,
  };

  window.history.replaceState(
    {},
    document.title,
    `${window.location.href}?proCache4321&parentExtIFramePanelId=ext-42&antiCache=1234`
  );

  window.parent = {
    Ext: {
      getCmp() {
        return parentIFramePanel;
      },
    },
  };
}

function mockFallbackTranslations() {
  angular.mock.module('hippo-cm', ($provide, $translateProvider) => {
    $translateProvider.translations('en', {});
  });
}

function mockMdIcon() {
  angular.mock.module('hippo-cm', ($provide) => {
    // mock md-icon directive to ignore GET requests fetching SVG files
    $provide.factory('mdIconDirective', () => angular.noop);
  });
}

beforeEach(mockHost);
beforeEach(mockFallbackTranslations);
beforeEach(mockMdIcon);

const context = require.context('./angularjs', true, /\.js$/);
context.keys().forEach(context);
