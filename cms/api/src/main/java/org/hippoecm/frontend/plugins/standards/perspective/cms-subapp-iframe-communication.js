/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
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

  // Implementation of the Child API
  Hippo.subApp = {};

  // Receiver object for the implementation of the Parent API
  Hippo.Cms = {
    showMask () {
      Hippo.AppToNavApp.showMask();
    },
    hideMask () {
      Hippo.AppToNavApp.hideMask();
    }
  };

  if (window.parent === window) { // cms is top window
    return;
  }

  const iFrameElement = window.document.getElementById('${iFrameElementId}'); // get iframe element of perspective
  // Config object sent to subapp when connecting.
  const subAppConnectConfig = {
    iframe: iFrameElement,
    methods: Hippo.Cms,
  };
  window.bloomreach['navapp-communication'].connectToChild(subAppConnectConfig)
    .then(childApi => Object.assign(Hippo.subApp, childApi))
    .catch(error => console.error(error));

}());
//# sourceURL=cms-subapp-iframe-communication.js
