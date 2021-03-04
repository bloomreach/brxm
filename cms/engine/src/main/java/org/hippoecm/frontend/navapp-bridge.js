/*
 * Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
 * This file contains code for establishing a connection between the
 * the Navigation Application, the CMS main window, and the CMS sub app windows.
 * It provides an implementation of the Child API.
 * It will register the CMS as a child of the Navigation App and, upon
 * success stores the Parent API methods in a global object so that
 * these methods will be available in the CMS iframe.
 * (see Navigation Communication Library typedocs for for information on
 * Child API and Parent API)
 */

(async function SetupNavappConnections() {
  'use strict';

  const navCom = window.bloomreach && window.bloomreach['navapp-communication'];

  if (!navCom) {
    throw Error('Navapp Communication Library not found but it is required');
  }

  let navappConnection;
  let isInitialNavigation = true;
  const subappConnectionMap = new Map();
  const locationsMap = new Map();
  const contentPerspectiveLoaded = new Promise((resolve, reject) => {
    const maxTotalDelayInMs = 3000;
    const delayInMs = 100;
    let totalDelayInMs = 0;

    const timer = setInterval(() => {
      if (Hippo.openById) {
        resolve();
      }

      totalDelayInMs += delayInMs;

      if (totalDelayInMs > maxTotalDelayInMs) {
        clearInterval(timer);
        reject();
      }
    }, delayInMs);
  });

  async function registerIframe(iframeElement) {
    const navappAPI = await navappConnection;
    const subappConnectConfig = {
      iframe: iframeElement,
      // Java Templating in js file
      // eslint-disable-next-line no-template-curly-in-string
      connectionTimeout: '${iframesConnectionTimeout}',
      methods: {
        ...navappAPI,
        updateNavLocation: async (location) => {
          locationsMap.set(iframeElement, location);

          return Hippo.navapp.updateNavLocation(location);
        },
      },
    };

    const subappConnection = navCom
      .connectToChild(subappConnectConfig)
      .catch(() => {
        subappConnectionMap.delete(iframeElement);
        navappAPI.onError({
          erroCode: 500,
          errorType: 'lenient',
          // Java Templating in js file
          // eslint-disable-next-line no-template-curly-in-string
          message: '${subAppConnectionTimeoutMessage}',
        });
      });

    subappConnectionMap.set(iframeElement, subappConnection);
    return subappConnection;
  }

  function getIFrameByPerspectiveId(perspectiveId) {
    const key = `${perspectiveId}-iframe`;
    return Array.from(subappConnectionMap.keys()).find((iframe) => iframe.className === key);
  }

  function getSubappConnection(iframeElement) {
    return subappConnectionMap.get(iframeElement) || registerIframe(iframeElement);
  }

  async function navigateIframe(iframe, path, triggeredBy) {
    const location = locationsMap.get(iframe);

    if (location && triggeredBy === 'Menu') {
      return Hippo.navapp.updateNavLocation(location);
    }

    const subappAPI = await getSubappConnection(iframe);

    if (subappAPI.navigate) {
      return subappAPI.navigate({ path }, triggeredBy);
    }
  }

  async function navigateToPerspective(appPath, pathElements, triggeredBy) {
    const iframe = getIFrameByPerspectiveId(appPath);

    if (appPath === 'projects') {
      if (!iframe) {
        throw Error('project\'s iframe is not found');
      }

      pathElements.unshift('projects');

      return navigateIframe(iframe, pathElements.join('/'), triggeredBy);
    }

    if (appPath === 'experience-manager') {
      // if iframe is undefined most probably it means it's not initialized yet
      // we can't distinguish between not initialized yet state and not defined at al
      if (!iframe) {
        const message = '"experience-manager" iframe is not registered. Internal iframe navigation step is skipped.';
        console.warn(message);
        return Promise.resolve();
      }

      return navigateIframe(iframe, pathElements.join('/'), triggeredBy);
    }

    if (appPath === 'content') {
      if (pathElements.length === 0) {
        if (triggeredBy === 'Breadcrumbs') {
          await contentPerspectiveLoaded;
          Hippo.openRootFolder();

          return;
        }

        return Promise.resolve();
      }

      if (pathElements[0] === 'path') {
        pathElements.shift();
        const documentPath = `/${pathElements.join('/')}`;
        await contentPerspectiveLoaded;
        Hippo.openByPath(documentPath, 'view');
      } else {
        const documentId = pathElements[1];
        await contentPerspectiveLoaded;
        Hippo.openById(documentId, 'view');
      }
    }

    return Promise.resolve();
  }

  const xmAPI = {
    getConfig: async () => {
      const apiVersion = navCom.getVersion();
      return { apiVersion };
    },

    beforeLogout: async () => {
      // Skip beforeLogout during initalNavigation to speed up
      // perceived loading time as during initialNavigation there will be
      // no app that could want to cancel navigation
      if (isInitialNavigation) {
        isInitialNavigation = false;
        return true;
      }

      const beforeLogoutPromises = Array
        .from(subappConnectionMap.values())
        .map(async (subappConnection) => {
          const subappAPI = await subappConnection;
          return subappAPI.beforeLogout ? subappAPI.beforeLogout() : Promise.resolve(true);
        });

      return Promise.all(beforeLogoutPromises);
    },

    beforeNavigation: async () => {
      // Skip beforeNavigation during initalNavigation to speed up
      // perceived loading time as during initialNavigation there will be
      // no app that could want to cancel navigation
      if (isInitialNavigation) {
        isInitialNavigation = false;
        return true;
      }

      const beforeNavigationPromises = Array
        .from(subappConnectionMap.values())
        .map(async (subappConnection) => {
          const subappAPI = await subappConnection;
          return subappAPI.beforeNavigation ? subappAPI.beforeNavigation() : Promise.resolve(true);
        });

      const responses = await Promise.all(beforeNavigationPromises);
      return responses.every((canNavigate) => canNavigate);
    },

    navigate: async (location, triggeredBy) => {
      const pathWithoutLeadingSlash = location.path.replace(/^\/+/, '');
      const pathElements = pathWithoutLeadingSlash.split('/');
      const appPath = pathElements.shift();
      const appLinkElement = document.querySelector(`.${appPath}`);

      if (!appLinkElement) {
        throw Error(`${appPath} not found`);
      }

      await navigateToPerspective(appPath, pathElements, triggeredBy);
      appLinkElement.click();
    },

    logout: async () => {
      Hippo.Events.publish('CMSLogout');
      // Java Templating in js file
      // eslint-disable-next-line no-template-curly-in-string
      document.location.href = '${logoutCallbackUrl}';
    },

    onUserActivity: async () => {
      Hippo.UserActivity.report();
    },
  };

  // Proxy the navapp api for access through window global scope
  // Used by Wicket and ExtJS
  const navapp = {
    getConfig: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.getConfig();
    },

    updateNavLocation: async (location) => {
      const navappAPI = await navappConnection;
      return navappAPI.updateNavLocation(location);
    },

    navigate: async (location) => {
      const navappAPI = await navappConnection;
      return navappAPI.navigate(location);
    },

    showMask: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.showMask();
    },

    hideMask: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.hideMask();
    },

    showBusyIndicator: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.showBusyIndicator();
    },

    hideBusyIndicator: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.hideBusyIndicator();
    },

    onUserActivity: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.onUserActivity();
    },

    onSessionExpired: async () => {
      const navappAPI = await navappConnection;
      return navappAPI.onSessionExpired();
    },

    onError: async (clientError) => {
      const navappAPI = await navappConnection;
      return navappAPI.onError(clientError);
    },
  };

  Object.assign(Hippo, {
    navapp,
    iframeConnections: {
      registerIframe,
    },
  });

  navappConnection = navCom
    .connectToParent({
      // Java Templating in js file
      // eslint-disable-next-line no-template-curly-in-string
      parentOrigin: '${parentOrigin}',
      methods: xmAPI,
    });

  const navappAPI = await navappConnection;
  Hippo.UserActivity.registerOnInactive(() => navappAPI.onSessionExpired());
  Hippo.UserActivity.registerOnActive(() => navappAPI.onUserActivity());
}());
