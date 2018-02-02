/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

    function dimension(px) {
        if (px === 0) {
            return "unconstrained";
        } else {
            return px + "px";
        }
    }

    // load plugin-specific CSS
    $('head').append('<link rel="stylesheet" href="tool/galleryPlugin/galleryPlugin.css"/>');

    angular.module('hippo.essentials')
        .directive('autofocus', function () {
            return {
                restrict: 'A',
                link: function (scope, element) {
                    element[0].focus();
                }
            };
        })

        .directive('variantName', function () {
            return {
                require: 'ngModel',
                restrict: 'A',
                scope: {
                    imageSet: '='
                },
                link: function (scope, element, attrs, ctrl) {
                    ctrl.$validators.variantName = function (modelValue) {
                        return !scope.imageSet.models.some(function (variant) {
                            return variant.name === modelValue;
                        });
                    }
                }
            }
        })

        .controller('GalleryManagerImageSetAddModalCtrl', [ '$scope', '$http', '$uibModalInstance', 'resource', 'prefix',
            function ($scope, $http, $uibModalInstance, resource, prefix) {

            $scope.updateExisting = true;
            $scope.updateHint = "Selecting this checkbox has the effect that all images existing in the CMS "
                              + "will be converted to this Image Set Definition. Image (link) fields of document beans will "
                              + "be updated to this image set, too.";

            $scope.create = function() {
                var parameters = {
                    imageSetPrefix: prefix,
                    imageSetName: $scope.name,
                    updateExisting: $scope.updateExisting
                };

                $http.post(resource, parameters).success(function () {
                    $uibModalInstance.close(prefix + ":" + $scope.name);
                });
            };

            $scope.cancel = function() {
                $uibModalInstance.dismiss();
            };
        }])

        .controller('GalleryManagerVariantAddModalCtrl', ['$scope', '$http', '$uibModalInstance', 'imageSet', 'resource',
            function($scope, $http, $uibModalInstance, imageSet, resource) {

            $scope.imageSet = imageSet;
            $scope.create = function() {
                var parameters = {
                    imageVariantName: $scope.name,
                    selectedImageSet: $scope.imageSet.name
                };
                $http.post(resource, parameters).success(function () {
                    $uibModalInstance.close($scope.name);
                });
            };
            $scope.cancel = function() {
                $uibModalInstance.dismiss();
            };
        }])

        .controller('GalleryManagerVariantViewModalCtrl', ['$scope', '$uibModalInstance', 'variant',
            function($scope, $uibModalInstance, variant) {

            $scope.variant = variant;
            $scope.dimension = function(px) {
                return dimension(px);
            };
            $scope.upscaling = function() {
                var map = {
                    true: 'on',
                    false: 'off'
                };
                return map[$scope.variant.upscaling];
            };
            $scope.optimization = function() {
                var map = {
                    'quality': 'quality',
                    'speed': 'speed',
                    'speed.and.quality': 'speed and quality',
                    'best.quality': 'best quality',
                    'auto': 'auto'
                };
                return map[$scope.variant.optimize];
            };
            $scope.compression = function() {
                var map = {
                    '1': 'uncompressed',
                    '0.95': 'best',
                    '0.9': 'very good',
                    '0.8': 'good',
                    '0.7': 'medium',
                    '0.5': 'low'
                };
                return map[$scope.variant.compression];
            };
            $scope.dismiss = function() {
                $uibModalInstance.dismiss();
            };
        }])

        .controller('GalleryManagerVariantEditModalCtrl', ['$scope', '$http', '$uibModalInstance', 'variant', 'resource',
            function($scope, $http, $uibModalInstance, variant, resource) {

            $scope.variant = angular.copy(variant);
            $scope.optimizeValues = [
                { value: "quality", description: "quality" },
                { value: "speed", description: "speed" },
                { value: "speed.and.quality", description: "speed and quality" },
                { value: "best.quality", description: "best quality" },
                { value: "auto", description: "auto" }
            ];
            $scope.compressionValues = [
                { value: 1, description: "uncompressed" },
                { value: 0.95, description: "best" },
                { value: 0.9, description: "very good" },
                { value: 0.8, description: "good" },
                { value: 0.7, description: "medium" },
                { value: 0.5, description: "low" }
            ];
            $scope.addTranslation = function() {
                $scope.variant.translations.push({
                    language: "",
                    message: ""
                });
            };
            $scope.deleteTranslation = function(translation) {
                $scope.variant.translations.splice($scope.variant.translations.indexOf(translation), 1);
            };
            $scope.save = function() {
                $http.post(resource, $scope.variant).success(function () {
                    $uibModalInstance.close();
                });
            };
            $scope.cancel = function() {
                $uibModalInstance.dismiss();
            };
        }])

        .controller('GalleryManagerVariantDeleteModalCtrl', ['$scope', '$http', '$uibModalInstance', 'variant', 'resource',
            function($scope, $http, $uibModalInstance, variant, resource) {

            $scope.variant = variant;
            $scope.ok = function() {
                $http.post(resource, variant).success(function () {
                    $uibModalInstance.close();
                });
            };
            $scope.cancel = function() {
                $uibModalInstance.dismiss();
            }
        }])

        .controller('GalleryManagerMainCtrl', ['$scope', '$rootScope', '$http', '$uibModal', 'essentialsRestService',
            function ($scope, $rootScope, $http, $uibModal, essentialsRestService) {

            var endpoint = essentialsRestService.baseUrl + "/galleryplugin";
            $scope.imageSets = [];
            $scope.selectedImageSet = null;
            $scope.feedback = "You have changed Image Sets and may want to update your Hippo Image Set beans now. "
                + "You can use the BeanWriter tool for this. Also, this changed the CMS configuration. "
                + "If you are logged in to the CMS, please log in again before uploading new images.";

            $scope.selectImageSet = function(imageSet) {
                $scope.selectedImageSet = imageSet;
            };

            $scope.isSelected = function(imageSet) {
                return $scope.selectedImageSet === imageSet;
            };

            $scope.dimension = function (px) {
                return dimension(px);
            };

            $scope.addImageSet = function () {
                $uibModal.open({
                    animation: true,
                    templateUrl: 'imageSetAddModal.html',
                    controller: 'GalleryManagerImageSetAddModalCtrl',
                    resolve: {
                        prefix: function () {
                            return $scope.imageSetPrefix;
                        },
                        resource: function () {
                            return endpoint + "/create";
                        }
                    }
                }).result.then(function (newImageSetName) {
                    loadImageSets(newImageSetName);
                    $scope.showFeedback = true;
                });
            };

            $scope.addVariant = function () {
                $uibModal.open({
                    animation: true,
                    templateUrl: 'variantAddModal.html',
                    controller: 'GalleryManagerVariantAddModalCtrl',
                    resolve: {
                        imageSet: function() {
                            return $scope.selectedImageSet;
                        },
                        resource: function () {
                            return endpoint + "/addvariant";
                        }
                    }
                }).result.then(function (variantName) {
                    loadImageSets().then(function() {
                        // immediately open edit dialog
                        if ($scope.selectedImageSet) {
                            $scope.selectedImageSet.models.forEach(function(variant) {
                                if (variant.name === variantName) {
                                    $scope.editVariant(variant);
                                }
                            });
                        }
                    });
                });
            };

            $scope.viewVariant = function (variant) {
                $uibModal.open({
                    animation: true,
                    templateUrl: 'variantViewModal.html',
                    controller: 'GalleryManagerVariantViewModalCtrl',
                    resolve: {
                        variant: function() {
                            return variant;
                        }
                    }
                });
            };

            $scope.editVariant = function (variant) {
                $uibModal.open({
                    animation: true,
                    templateUrl: 'variantEditModal.html',
                    controller: 'GalleryManagerVariantEditModalCtrl',
                    resolve: {
                        variant: function() {
                            return variant;
                        },
                        resource: function() {
                            return endpoint + "/update";
                        }
                    }
                }).result.then(function () {
                    loadImageSets();
                    $scope.showFeedback = true;
                });
            };

            $scope.deleteVariant = function(variant) {
                $uibModal.open({
                    animation: true,
                    templateUrl: 'variantDeleteModal.html',
                    controller: 'GalleryManagerVariantDeleteModalCtrl',
                    resolve: {
                        variant: function () {
                            return variant;
                        },
                        resource: function () {
                            return endpoint + "/remove";
                        }
                    }
                }).result.then(function () {
                    loadImageSets();
                    $scope.showFeedback = true;
                });
            };

            function loadImageSets(imageSetNameToSelect) {
                return $http.get(endpoint).success(function (data) {
                    $scope.imageSets = data;

                    if (!imageSetNameToSelect) {
                        if ($scope.selectedImageSet) {
                            imageSetNameToSelect = $scope.selectedImageSet.name;
                        } else if ($scope.imageSets.length > 0) {
                            imageSetNameToSelect = $scope.imageSets[0].name;
                        }
                    }

                    $scope.selectedImageSet = undefined;
                    if (imageSetNameToSelect) {
                        angular.forEach($scope.imageSets, function(imageSet) {
                            if (imageSet.name === imageSetNameToSelect) {
                                $scope.selectedImageSet = imageSet;
                            }
                        });
                    }
                });
            }

            //############################################
            // INIT APP
            //############################################
            $scope.imageSetPrefix = $rootScope.projectSettings.projectNamespace;
            loadImageSets();
        }]);
}());
