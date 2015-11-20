/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

    .service('hippo.channel.ChannelService', [
      'hippo.channel.ConfigService',
      '$http',
      '$q',
      function (ConfigService, $http, $q) {

        var channelService = {};

        channelService.getPreviewChannels = function () {
          var deferred = $q.defer();

          $http.get(ConfigService.apiUrlPrefix +
            '/cafebabe-cafe-babe-cafe-babecafebabe./channels?preview=false&workspaceRequired=true')
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        channelService.getPageLocations = function (mountId) {
          var deferred = $q.defer();

          $http.get(ConfigService.apiUrlPrefix + '/' + mountId + './pagelocations/' + mountId)
            .success(function (response) {
              deferred.resolve(response.data);
            })
            .error(function (error) {
              deferred.reject(error);
            });

          return deferred.promise;
        };

        return channelService;
      }
    ]);
}());
