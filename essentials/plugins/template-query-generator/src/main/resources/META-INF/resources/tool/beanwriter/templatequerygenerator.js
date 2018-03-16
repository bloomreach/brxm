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
    angular.module('hippo.essentials')
        .controller('beanWriterCtrl', function ($scope, $http, essentialsRestService) {
            $scope.endpoint = essentialsRestService.baseUrl + '/beanwriter';
            $scope.resultMessages = [];
            $scope.selectedImageSet = 'HippoGalleryImageSet';
            $scope.updateImageMethods = false;
            $scope.runBeanWriter = function () {
                var parameters = {
                    imageSet: $scope.selectedImageSet,
                    updateImageMethods: $scope.updateImageMethods
                };
                $http.post($scope.endpoint, parameters).success(function (data) { });
            };
            $scope.identity = angular.identity; // for sorting the imageSets in the UI.

            $http.get($scope.endpoint + "/imagesets").success(function (data) {
                $scope.imageSets = data;
                // check if we have custom image sets and preselect (first) one.
                if ($scope.imageSets) {
                    for (var i = 0; i < $scope.imageSets.length; i++) {
                        var value = $scope.imageSets[i];
                        if (value !== 'HippoGalleryImageSet') {
                            $scope.selectedImageSet = value;
                            break;
                        }
                    }
                }
            });
        })
})();