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

  if (window.parent === window) {
    // not in an iframe, so there will be no parent to connect to
    return;
  }

  // This method is called whenever a Perspective is activated (see ParentApiCaller)
  // The path is expected to be the appPath, which represents the identifier of a Perspective.
  Hippo.updateNavLocation = function (path) {
    if (!Hippo.currentNavLocation || Hippo.currentNavLocation.path.indexOf(path) < 0) {
      // Hippo.currentNavLocation is undefined initially (after login). When the CMS activates the first
      // perspective then it calls this method, so then we can set it with path and leave breadcrumb undefined.
      //
      // If currentNavLocation.path starts with path then a sub-app has already called changed the
      // currentNavLocation and called AppToNavApp.updateNavLocation with it. (see cms-subapp-iframe-communication.js)
      //
      // In all other cases it means that the perspective changed to some other perspective so then we should
      // change the currentLocation and inform the navapp about this
      Hippo.currentNavLocation = {path};
      Hippo.AppToNavApp.updateNavLocation(Hippo.currentNavLocation);
    }
  };

  let openChannelManagerOverview = function () {
    this.rootPanel = Ext.getCmp('rootPanel');
    if (this.rootPanel) {
      this.rootPanel.fireEvent('navigate-to-channel-overview');
    }
  };
// Implementation of the Child API
  Hippo.NavAppToApp = {

    beforeNavigation: function () {
      return Promise.resolve(true);
    },

    navigate: function (location, flags) {
      const pathElements = location.path.split('/');
      const perspectiveIdentifier = pathElements.shift();
      const perspectiveClassName = `.hippo-perspective-${perspectiveIdentifier}perspective`;

      if (!perspectiveIdentifier) {
        return Promise.reject(new Error(`${location.path} is invalid`));
      }

      const perspective = document.querySelector(perspectiveClassName);
      if (!perspective) {
        return Promise.reject(new Error(`${perspectiveClassName} not found`));
      }

      perspective.click();

      if (['channelmanager', 'projects'].includes(perspectiveIdentifier)) {
        var location = {path: pathElements.join('/')}
        if (flags && flags['forceRefresh']) {
          if (perspectiveIdentifier === 'channelmanager') {
            openChannelManagerOverview.call(this);
          }
          if (perspectiveIdentifier === 'projects') {
            location.path = '/projects';
          }
        }
        let childApi = Hippo.SubApp[perspectiveIdentifier + '-iframe'];
        if (childApi) {
          childApi.navigate(location, flags);
        }
      }
      return Promise.resolve();
    },

    logout: function () {
      // The jqXHR objects returned by jQuery.ajax() as of jQuery 1.5 implements the Promise interface
      // See http://api.jquery.com/jquery.ajax/
      return jQuery.ajax('${logoutCallbackUrl}');
    }
  };

  // Receiver object for the implementation of the Parent API
  Hippo.AppToNavApp = {};

  // Config object sent to parent when connecting.
  const parentConnectionConfig = {
    parentOrigin: '${parentOrigin}',
    methods: Hippo.NavAppToApp
  };

  // Establish the connection
  window.bloomreach['navapp-communication']
    .connectToParent(parentConnectionConfig)
    .then(parentApi => {
      Object.assign(Hippo.AppToNavApp, parentApi);
      Hippo.UserActivity.onInactive(() => {
        parentApi.onSessionExpired();
     })
    })
    .catch(error => console.error(error));

}());
//# sourceURL=nav-app-to-app.js
