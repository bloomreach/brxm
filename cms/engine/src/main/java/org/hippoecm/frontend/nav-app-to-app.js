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

(function() {
  'use strict';

  class IFrameConnections {
    constructor(navAppCommunication, cmsToNavApp, subAppToCms) {
      this.navAppCommunication = navAppCommunication;
      this.cmsToNavApp = cmsToNavApp;
      this.subAppToCms = subAppToCms;
      this.connections = new Map([]);
      this.parentConnection = {};
    }

    getConnections() {
      return this.connections;
    }

    getParentConnection() {
      return this.parentConnection;
    }

    navigate(iframeElement, location, flags) {
      let childConnectionPromise;

      if (!this.getConnections().has(iframeElement)) {
        childConnectionPromise = this.registerIframe(iframeElement);
      } else {
        childConnectionPromise = this.connections.get(iframeElement);
      }

      return childConnectionPromise
        .then((childApi) => childApi.navigate(location, flags))
        .catch((e) => console.error(e));
    }

    registerIframe(iframeElement) {
      const subAppConnectConfig = {
        iframe: iframeElement,
        methods: this.subAppToCms,
      };
      const connectionPromise = this.navAppCommunication.connectToChild(
        subAppConnectConfig,
      );

      connectionPromise
        .then(() => this.connections.set(iframeElement, connectionPromise))
        .catch((e) => console.error(e));

      return connectionPromise;
    }

    connectToParent(parentOrigin) {
      // Config object sent to parent when connecting.
      const parentConnectionConfig = {
        parentOrigin,
        methods: this.cmsToNavApp,
      };

      return Promise.all(this.getConnections().values()).then(
        () => {
          this.parentConnection = this.navAppCommunication.connectToParent(
            parentConnectionConfig,
          ).then((parentApi) => {
            Hippo.UserActivity.registerOnInactive(() => {
              parentApi.onSessionExpired();
            });
            Hippo.UserActivity.registerOnActive(() => {
              if (parentApi.onUserActivity) {
                parentApi.onUserActivity();
              }
            });
            return parentApi;
          });
          return this.parentConnection;
        },
        (e) => console.error(e),
      );
    }
  }

  // This method is called whenever a Perspective is activated (see ParentApiCaller)
  // The path is expected to be the appPath, which represents the identifier of a Perspective.
  Hippo.updateNavLocation = function(path) {
    if (
      !Hippo.currentNavLocation ||
      Hippo.currentNavLocation.path.indexOf(path) < 0
    ) {
      // Hippo.currentNavLocation is undefined initially (after login). When the CMS activates the first
      // perspective then it calls this method, so then we can set it with path and leave breadcrumb undefined.
      //
      // If currentNavLocation.path starts with path then a sub-app has already called changed the
      // currentNavLocation and called AppToNavApp.updateNavLocation with it. (see cms-subapp-iframe-communication.js)
      //
      // In all other cases it means that the perspective changed to some other perspective so then we should
      // change the currentLocation and inform the navapp about this
      Hippo.currentNavLocation = { path };
      Hippo.iframeConnections
        .getParentConnection()
        .then(cmsToNavApp =>
          cmsToNavApp.updateNavLocation(Hippo.currentNavLocation),
        );
    }
  };

  function getPerspective(identifier) {
    const perspectiveClassName = `.hippo-perspective-${identifier}perspective`;
    const perspective = document.querySelector(perspectiveClassName);

    return perspective;
  }

  function openChannelManagerOverview() {
    const rootPanel = Ext.getCmp('rootPanel');

    if (rootPanel) {
      rootPanel.fireEvent('navigate-to-channel-overview');
    }
  }

  Hippo.showMask = function() {
    return Hippo.iframeConnections
      .getParentConnection()
      .then(cmsToNavApp => cmsToNavApp.showMask());
  };

  Hippo.hideMask = function() {
    return Hippo.iframeConnections
      .getParentConnection()
      .then(cmsToNavApp => cmsToNavApp.hideMask());
  };

  // Receiver object for the implementation of the Parent API
  const subAppToCms = {
    updateNavLocation(location) {
      Hippo.currentNavLocation = {
        path: location.path,
        breadcrumbLabel: location.breadcrumbLabel,
      };

      return Hippo.iframeConnections
        .getParentConnection()
        .then(cmsToNavApp =>
          cmsToNavApp.updateNavLocation(Hippo.currentNavLocation),
        );
    },
    showMask() {
      return Hippo.showMask();
    } ,
    hideMask() {
      return Hippo.hideMask();
    } ,
  };

  const navAppToCms = {
    beforeNavigation() {
      return Promise.resolve(true);
    },
    navigate(location, flags) {
      const pathWithoutLeadingSlash = location.path.replace(/^\/+/, '');
      const pathElements = pathWithoutLeadingSlash.split('/');
      const perspectiveIdentifier = pathElements.shift();
      const perspective = getPerspective(perspectiveIdentifier);

      if (!perspective) {
        return Promise.reject(new Error(`${perspectiveIdentifier} not found`));
      }

      perspective.click();

      const connections = Hippo.iframeConnections.getConnections();
      const iframeClassName = Array.from(connections.keys()).map(
        (iframe) => iframe.className,
      );

      if (perspectiveIdentifier === 'browser' && pathElements.length > 1) {
        const location = document.location;
        const url = new URL(location.href);
        url.searchParams.append('uuid', pathElements[1]);
        location.assign(url.toString());
      }

      if (iframeClassName.includes(`${perspectiveIdentifier}-iframe`)) {
        if ( perspectiveIdentifier === 'projects' ) {
          pathElements.unshift(perspectiveIdentifier);
        }
        const subAppLocation
          = { path: pathElements.join('/') };

        if (flags && flags['forceRefresh']) {
          if (perspectiveIdentifier === 'channelmanager') {
            openChannelManagerOverview.call(this);
          }
          if (perspectiveIdentifier === 'projects') {
            subAppLocation
              .path = '/projects';
          }
        }

        const iframe = Array.from(connections.keys()).find(
          iframe => iframe.className === perspectiveIdentifier + '-iframe',
        );

        if (iframe) {
          return Hippo.iframeConnections.navigate(iframe, subAppLocation
            , flags);
        }
      }

      return Promise.resolve();
    },

    logout() {
      // The jqXHR objects returned by jQuery.ajax() as of jQuery 1.5 implements the Promise interface
      // See http://api.jquery.com/jquery.ajax/
      return jQuery.ajax('${logoutCallbackUrl}');
    },

    onUserActivity() {
      Hippo.UserActivity.report();
      return Promise.resolve();
    }

  };

  window.bloomreach = window.bloomreach || {};

  Hippo.IFrameConnections = IFrameConnections;
  Hippo.iframeConnections = new IFrameConnections(
    window.bloomreach['navapp-communication'],
    navAppToCms,
    subAppToCms,
  );

  Hippo.iframeConnections.connectToParent('${parentOrigin}');
})();

//# sourceURL=nav-app-to-app.js
