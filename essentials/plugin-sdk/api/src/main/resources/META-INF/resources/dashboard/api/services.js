/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
  "use strict";

  var dashboardRoot = window.SERVER_URL + '/essentials/rest';
  var projectSettings = dashboardRoot + '/project/settings';
  var contentTypes = dashboardRoot + '/documents';
  var templateQueries = contentTypes + '/templatequeries';

  function pluginById(id) {
    return dashboardRoot + '/plugins/' + id;
  }
  function instancesForContentType(jcrType) {
    return contentTypes + '/' + jcrType;
  }

  angular.module('hippo.essentials')
    .service('essentialsRestService', function() {

      /**
       * Base URL for all dynamically registered REST resources.
       */
      this.baseUrl = dashboardRoot + '/dynamic';
    })

    .service('essentialsPluginService', function($http) {

      /**
       * Retrieve the parameters for the plugin with the specified ID.
       *
       * Returns an AngularJs 'HttpPromise', which, upon success, resolves into a deserialized PluginDescriptor object.
       */
      this.getPluginById = function(id) {
        return $http.get(pluginById(id));
      };
    })

    .service('essentialsProjectService', function($http) {

      /**
       * Retrieve the project's current settings.
       *
       * Returns an AngularJs 'HttpPromise', which, upon success, resolves into a deserialized ProjectSettings object
       */
      this.getProjectSettings = function() {
        return $http.get(projectSettings);
      };
    })

    .service('essentialsContentTypeService', function($http) {

      /**
       * Retrieve a list of content types in the project's namespace.
       *
       * Returns an AngularJs 'HttpPromise', which, upon success, resolves into a list of deserialized ContentType
       * objects.
       */
      this.getContentTypes = function() {
        return $http.get(contentTypes);
      };

      /**
       * Retrieve a list of instances of the specified (JCR) content type, e.g. 'myhippoproject:newsdocument'.
       *
       * Returns an AngularJs 'HttpPromise', which, upon success, resolves into a list of deserialized
       * ContentTypeInstance objects.
       */
      this.getContentTypeInstances = function(jcrType) {
        return $http.get(instancesForContentType(jcrType));
      };

      /**
       * Retrieve a list of 'template queries'.
       *
       * Returns an AngularJs 'HttpPromise', which, upon success, resolves into a list of deserialized
       * TemplateQuery objects.
       */
      this.getTemplateQueries = function() {
        return $http.get(templateQueries);
      };
    });
})();