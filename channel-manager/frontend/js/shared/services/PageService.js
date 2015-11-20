/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

  angular.module('hippo.channel')

    .service('hippo.channel.PageService', [
      '$http',
      '$q',
      'hippo.channel.ConfigService',
      function ($http, $q, ConfigService) {
        var pageService = {};

        function pageServiceUrl (suffix) {
          var url = ConfigService.apiUrlPrefix;
          if (angular.isString(suffix)) {
            url += suffix;
          }
          return url;
        }

        pageService.getPages = function () {
          var deferred = $q.defer();

          $http.get(pageServiceUrl('/' + ConfigService.sitemapId + './pages'))
            .success(function (response) {
              deferred.resolve(response.data.pages);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        pageService.getCurrentPage = function () {
          var deferred = $q.defer();

          $http.get(pageServiceUrl('/' + ConfigService.sitemapId + './item/' + ConfigService.sitemapItemId))
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        pageService.getMountInfo = function () {
          var deferred = $q.defer();

          $http.get(pageServiceUrl('/' + ConfigService.sitemapId + './mount'))
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        pageService.createPage = function (page, location) {
          var deferred = $q.defer(),
            url = '/' + ConfigService.sitemapId + './create';

          if (location && location.id !== null) {
            url += '/' + location.id;
          }

          $http.post(pageServiceUrl(url), page)
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });
          return deferred.promise;
        };

        pageService.copyPage = function (mountId, siteMapItemUUId, targetName, targetSiteMapItemUUID) {

          var deferred = $q.defer(),
              url = '/' + ConfigService.sitemapId + './copy';

          console.log("targetSiteMapItemUUID", targetSiteMapItemUUID);
          var req = {
            method :  'POST',
            url : pageServiceUrl(url),
            headers: {
              'siteMapItemUUId': siteMapItemUUId,
              'targetName': targetName
            }
          };
          if(mountId) {
            req.headers.mountId = mountId;
          }
          if(targetSiteMapItemUUID) {
            req.headers.targetSiteMapItemUUID = targetSiteMapItemUUID;
          }
          $http(req)
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });
          return deferred.promise;
        };

        pageService.updatePage = function (page) {
          var deferred = $q.defer();

          $http.post(pageServiceUrl('/' + ConfigService.sitemapId + './update'), page)
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        pageService.deletePage = function (pageId) {
          var deferred = $q.defer();

          $http.post(pageServiceUrl('/' + ConfigService.sitemapId + './delete/' + pageId))
            .success(function (response) {
              deferred.resolve(response);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        return pageService;
      }
    ]);
}());
