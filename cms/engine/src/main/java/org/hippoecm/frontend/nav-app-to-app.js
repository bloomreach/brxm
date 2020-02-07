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

  const NAVAPP_COMMUNICATION_LIBRARY = window.bloomreach && window.bloomreach['navapp-communication'];

  class IFrameConnections {
    constructor(parentApiPromise) {
      this.parentApiPromise = parentApiPromise;
      this.childApiPromiseMap = new Map([]);
      this.locationsMap = new Map();
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
            methods: {
              ...parentApi,
              updateNavLocation: (location) => {
                this.locationsMap.set(iframeElement, location);

                return this.updateNavLocation(location);
              }
             },
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

    showBusyIndicator() {
      this.parentApiPromise.then(parentApi => parentApi.showBusyIndicator());
    }

    hideBusyIndicator() {
      this.parentApiPromise.then(parentApi => parentApi.hideBusyIndicator());
    }

    updateNavLocation (location) {
      return this.parentApiPromise.then(parentApi => parentApi.updateNavLocation(location));
    }

    onError(clientError) {
      return this.parentApiPromise.then(parentApi => parentApi.onError(clientError));
    }
  }

  Hippo.IFrameConnections = IFrameConnections;

  const navigateIframe = (iframe, path, triggeredBy) => {
    const location = Hippo.iframeConnections.locationsMap.get(iframe);

    if (location && triggeredBy === 'Menu') {
      return Hippo.updateNavLocation(location);
    }

    return Hippo.iframeConnections.getChildApiPromise(iframe)
      .then(childApi => childApi.navigate({ path }, triggeredBy));
  };

  const cmsChildApi = {

    getConfig() {
      const apiVersion = (NAVAPP_COMMUNICATION_LIBRARY && NAVAPP_COMMUNICATION_LIBRARY.getVersion()) || 'unknown';
      return { apiVersion };
    },

    beforeNavigation () {
      const beforeNavigationPromises = Hippo.iframeConnections.getChildApiPromises()
        .map(childApiPromise =>
          childApiPromise.then(
            childApi => childApi.beforeNavigation ? childApi.beforeNavigation() : Promise.resolve(true)
          )
      );

      return Promise
        .all(beforeNavigationPromises)
        .then(
          childResponses => childResponses.every(canNavigate => canNavigate),
          error => new Error(error),
        );
    },

    navigate (location, triggeredBy) {
      const pathWithoutLeadingSlash = location.path.replace(/^\/+/, '');
      const pathElements = pathWithoutLeadingSlash.split('/');
      const appPath = pathElements.shift();
      const appLinkElement = document.querySelector(`.${appPath}`);

      if (!appLinkElement) {
        return Promise.reject(new Error(`${appPath} not found`));
      }

      const iframe = Hippo.iframeConnections.getIFrameByPerspectiveId(appPath);

      const navigateToPerspective = () => {
        switch (appPath) {
          case 'projects':
            if (!iframe) {
              return Promise.reject(new Error('project\'s iframe is not found'));
            }

            pathElements.unshift('projects');

            return navigateIframe(iframe, pathElements.join('/'), triggeredBy);

          case 'experience-manager':
            // if iframe is undefined most probably it means it's not initialized yet
            // we can't distinguish between not initialized yet state and not defined at al
            if (!iframe) {
              const message = '"experience-manager" iframe is not registered. Internal iframe navigation step is skipped.';
              console.warn(message);

              return Promise.resolve();
            }

            return navigateIframe(iframe, pathElements.join('/'), triggeredBy);

          case 'content':
            if (pathElements.length === 0) {
              return Promise.resolve();
            }

            if (pathElements[0] === 'path') {
              pathElements.shift();
              let documentPath = `/${pathElements.join('/')}`;
              contentPerspectiveLoaded.then(() => {
                  Hippo.openDocumentByPath(documentPath, "view");
                }
              );

            } else {
              let documentId = pathElements[1];
              contentPerspectiveLoaded.then(() => {
                Hippo.openDocumentById(documentId, "view");
              });
            }

          default:
              return Promise.resolve();
        }
      };

      return navigateToPerspective().then(() => appLinkElement.click());
    },

    logout () {
      Hippo.Events.publish('CMSLogout');
      return new Promise((res, _) => {
        res();
        document.location.href = '${logoutCallbackUrl}';
      });
    },

    onUserActivity () {
      Hippo.UserActivity.report();
      return Promise.resolve();
    }

  };

  const contentPerspectiveLoaded = new Promise((resolve, reject) => {

    const maxTotalDelayInMs = 3000;
    const delayInMs = 100;
    let totalDelayInMs = 0;

    (function waitForContentPerspective() {

      if (Hippo.openDocumentById) {
        return resolve();
      }

      totalDelayInMs += delayInMs;

      if (totalDelayInMs > maxTotalDelayInMs) {
        return reject();
      }

      window.setTimeout(waitForContentPerspective, delayInMs);
    })();
  });


  const parentApiPromise = (NAVAPP_COMMUNICATION_LIBRARY && NAVAPP_COMMUNICATION_LIBRARY.connectToParent({parentOrigin: '${parentOrigin}', methods: cmsChildApi})
    .then(parentApi => {
      Hippo.UserActivity.registerOnInactive(() => parentApi.onSessionExpired());
      Hippo.UserActivity.registerOnActive(() => parentApi.onUserActivity());
      parentApi.getConfig().then(config => {
        console.info(`Connected to parent, parent api version = ${config.apiVersion}`);
      });
      return parentApi;
    })) || Promise.reject(new Error('navapp-communication library is required'));

  Hippo.iframeConnections = new IFrameConnections(
    parentApiPromise
  );

  Hippo.updateNavLocation = function(location) {
    return Hippo.iframeConnections.updateNavLocation(location);
  };
  Hippo.showMask = function() {
    Hippo.iframeConnections.showMask();
  };
  Hippo.hideMask = function() {
    Hippo.iframeConnections.hideMask();
  };
  Hippo.showBusyIndicator = function() {
    Hippo.iframeConnections.showBusyIndicator();
  };
  Hippo.hideBusyIndicator = function() {
    Hippo.iframeConnections.hideBusyIndicator();
  };
  Hippo.onError = function(clientError) {
    Hippo.iframeConnections.onError(clientError);
  }
})();

//# sourceURL=nav-app-to-app.js
