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

  function connectToParent (cfg) {
    // TODO (meggermont): replace this stub with implementation from navigation client npm package
    console.log('Stubbed connection to parent:', cfg);
    return new Promise(function (resolve, ignore) {
      resolve({updateLocation: 'NOT IMPLEMENTED'});
    });
  }

  // Implementation of the Child API
  Hippo.NavAppToApp = {

    navigate: function (location) {
      const perspectiveClassName = location.path;
      const perspectives = document.getElementsByClassName(perspectiveClassName);
      if (perspectives && perspectives[0]) {
        perspectives[0].click();
        return Promise.resolve();
      }
      return Promise.reject(new Error(`${perspectiveClassName} not found`));
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
  connectToParent(parentConnectionConfig)
    .then(parentApi =>
      Object.assign(Hippo.AppToNavApp, parentApi))
    .catch(error =>
      console.error(error));

}());