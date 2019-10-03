/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

/**
 * This file contains functions for establishing a connection between the
 * CMS (the child) and the Navigation Application (the parent).
 * It provides an implementation of the Child API.
 * It will register the CMS as a child of the Navigation App and, upon
 * success stores the Parent API methods in a global object so that
 * these methods will be available in the CMS iframe.
 */

(function () {
  'use strict';

  class IFrameConnections {
    constructor(parentApiPromise) {
      this.parentApiPromise = parentApiPromise;
      this.childApiPromiseMap = new Map([]);
    }

    getIFrameByPerspectiveId (perspectiveId) {
      const key = `${perspectiveId}-iframe`;
      return Array.from(this.childApiPromiseMap.keys()).find(iframe => iframe.className === key);
    }

    getChildApiPromises () {
      return Array.from(this.childApiPromiseMap.values());
    }

    getChildApiPromise (iframeElement) {
      if (this.childApiPromiseMap.has(iframeElement)) {
        return this.childApiPromiseMap.get(iframeElement);
      }
      return this.registerIframe(iframeElement);
    }

    registerIframe (iframeElement) {
      return this.parentApiPromise
        .then(parentApi => {
          const subAppConnectConfig = {
            iframe: iframeElement,
            methods: parentApi,
          };
          const childApiPromise = window.bloomreach['navapp-communication'].connectToChild(subAppConnectConfig);
          this.childApiPromiseMap.set(iframeElement, childApiPromise);
          return childApiPromise;
        });
    }

    showMask () {
      this.parentApiPromise.then(parentApi => parentApi.showMask());
    }

    hideMask () {
      this.parentApiPromise.then(parentApi => parentApi.hideMask());
    }

    updateNavLocation (location) {
      this.parentApiPromise.then(parentApi => parentApi.updateNavLocation(location));
    }

  }
  Hippo.IFrameConnections = IFrameConnections;

  const cmsChildApi = {

    beforeNavigation () {
      const beforeNavigationPromises = Hippo.iframeConnections.getChildApiPromises()
        .map(childApiPromise =>
          childApiPromise.then(
            childApi => childApi.beforeNavigation && childApi.beforeNavigation() || Promise.resolve(true)
          )
        );
      return Promise.all(beforeNavigationPromises);
    },

    navigate (location, flags) {
      const pathWithoutLeadingSlash = location.path.replace(/^\/+/, '');
      const pathElements = pathWithoutLeadingSlash.split('/');
      const perspectiveId = pathElements.shift();
      const perspective = document.querySelector(`.hippo-perspective-${perspectiveId}perspective`);

      if (!perspective) {
        return Promise.reject(new Error(`${perspectiveId} not found`));
      }

      const iframe = Hippo.iframeConnections.getIFrameByPerspectiveId(perspectiveId);
      if (iframe) {
        const forceRefresh = flags && flags.forceRefresh;
        const subAppLocation = {path: pathElements.join('/')};
        switch (perspectiveId) {

          case 'projects':
            pathElements.unshift(perspectiveId);
            if (forceRefresh) {
              subAppLocation.path = 'projects';
            }
            break;

          case 'channelmanager':
            if (forceRefresh) {
              const rootPanel = Ext.getCmp('rootPanel');
              if (rootPanel) {
                rootPanel.fireEvent('navigate-to-channel-overview');
              }
            }
            break;
        }
        return Hippo.iframeConnections.getChildApiPromise(iframe)
          .then(childApi => childApi.navigate(subAppLocation, flags))
          .then(() => perspective.click());
      }

      if (perspectiveId === 'browser' && pathElements.length > 1) {
        const docLocation = document.location;
        const url = new URL(docLocation.href);
        url.searchParams.append('uuid', pathElements[1]);
        docLocation.assign(url.toString())
      }
      perspective.click();
      return Promise.resolve();
    },

    logout () {
      // The jqXHR objects returned by jQuery.ajax() as of jQuery 1.5 implements the Promise interface
      // See http://api.jquery.com/jquery.ajax/
      return jQuery.ajax('${logoutCallbackUrl}');
    },

    onUserActivity () {
      Hippo.UserActivity.report();
      return Promise.resolve();
    }

  };

  const parentApiPromise = window.bloomreach && window.bloomreach['navapp-communication']
    .connectToParent({parentOrigin: '${parentOrigin}', methods: cmsChildApi})
    .then(parentApi => {
      Hippo.UserActivity.registerOnInactive(() => parentApi.onSessionExpired());
      Hippo.UserActivity.registerOnActive(() => parentApi.onUserActivity());
      return parentApi;
    });

  Hippo.iframeConnections = new IFrameConnections(
    parentApiPromise
  );

  Hippo.updateNavLocation = function(location) {
    Hippo.iframeConnections.updateNavLocation(location);
  }
  Hippo.showMask = function() {
    Hippo.iframeConnections.showMask();
  }
  Hippo.hideMask = function() {
    Hippo.iframeConnections.hideMask();
  }
})();

//# sourceURL=nav-app-to-app.js
