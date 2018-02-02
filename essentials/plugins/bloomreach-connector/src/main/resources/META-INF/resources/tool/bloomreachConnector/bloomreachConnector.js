/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
    angular.module('hippo.essentials')
        .controller('bloomreachConnectorCtrl', function ($scope, $http, essentialsRestService) {
            
            $scope.endpoint = essentialsRestService.baseUrl + '/bloomreachConnector';
            $scope.data = {};
            $scope.crispExists = false;
            $scope.crispDependencyExists = false;


            $scope.run = function () {
                var payload = $scope.data;
                $http.post($scope.endpoint, payload).success(function (data) {

                });
            };
            $scope.install = function () {

                $http.post($scope.endpoint +"/install", {}).success(function (data) {
                    // do nothing
                    $scope.crispDependencyExists = true;
                });
            };
            $http.get($scope.endpoint).success(function (data) {
                console.log(data);
                data.resourceName = "productsResource";
                data.realm = "prod";
                data.refUrl = "refUrl";
                data.fl = "pid,title,brand,price,sale_price,promotions,thumb_image,sku_thumb_images,sku_swatch_images,sku_color_group,url,price_range,sale_price_range,description,is_live,score";
                $scope.data = data;
                $scope.crispExists = data.crispExists;
                $scope.crispDependencyExists = data.crispDependencyExists;
            });

        });
})();