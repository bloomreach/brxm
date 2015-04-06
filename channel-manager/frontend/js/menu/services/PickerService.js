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

  angular.module('hippo.channel.menu')

    .service('hippo.channel.menu.PickerService', [
      'hippo.channel.ConfigService',
      '$http',
      function (ConfigService, $http) {
        var pickerData = {
          items: []
        }, callObj = {
          method: 'GET'
        };

        function getInitialData (id, link) {
          callObj.url = ConfigService.apiUrlPrefix + '/' + id + './picker/';
          if (link) {
            callObj.url += link;
          }
          return $http(callObj).success(function (returnedData) {
            return angular.copy([returnedData.data], pickerData.items);
          });
        }

        function getData (item) {
          callObj.url = ConfigService.apiUrlPrefix + '/' + item.id + './picker/';
          return $http(callObj).success(function (returnedData) {
            item.items = returnedData.data.items;
          });
        }

        return {
          getTree: function () {
            return pickerData.items;
          },
          getInitialData: function (id, link) {
            return getInitialData(id, link);
          },
          getData: function (item) {
            return getData(item);
          }
        };
      }
    ]);
}());
