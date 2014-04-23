/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

    angular.module('hippo.essentials').controller('galleryPluginCtrl', function ($scope, $sce, $log, $rootScope, $http, $modal) {

        $scope.message = "Gallery plugin";
        $scope.selectedTab = 1;

        $scope.invalidated = false;


        $http({
            method: 'GET',
            url: $rootScope.REST.imageSets
        }).success(function (data) {
            $scope.imageSetsData = data;
            $scope.imageSets = $scope.imageSetsData.imageSets;
        });

        /*
         // TODO populate image sets from rest service
         $http.get('plugins/galleryPlugin/testimagesets.json').success(function(data) {
         $scope.imageSets = data;

         //$scope.updateSelectedImageSetsAndVariants();
         });
         */

        $scope.saveGalleryProcessor = function () {

            $http({
                method: 'PUT',
                url: $rootScope.REST.galleryProcessorSave + '2',
                data: $scope.imageProcessor
            }).success(function (data) {
                $log.info(data);
            });
        };

        $scope.saveImageSets = function () {

            $http({
                method: 'PUT',
                url: $rootScope.REST.imageSetsSave,
                data: $scope.imageSetsData
            }).success(function (data) {
                $log.info(data);
            });
        };


        // TODO populate image sets from rest service
        $http.get('plugins/galleryPlugin/cmslanguages.json').success(function (data) {
            $scope.cmsLanguages = data;
        });


        $scope.variantTranslationsModels = {};


        $scope.init = function () {
            $log.info(" **** gallery plugin called ***");
            $http.get($rootScope.REST.galleryProcessor).success(function (data) {
                if(data && data.length > 0){
                    $scope.imageProcessor = data[0];
                }

            });


        };

        /*
         $scope.displayImageSet = function(imageSet) {
         $scope.currentImageSet = imageSet;
         $log.info("selected" + $scope.currentImageSet.name);
         }
         */

        /*
         $scope.addImageSet = function() {
         $log.info("Add image set");
         }

         $scope.deleteCurrentImageSet = function() {
         $log.info("Add image set");
         }
         */

        // TODO change this
        $scope.projectGalleryNamespace = "projectgallery";


        $scope.newVariantTemplate =
        {
            "id": "",
            "namespace": "",
            "name": "",
            "translations": [
                {
                    "locale": "",
                    "message": ""
                }
            ],
            "width": 0,
            "height": 0,
            "properties": [],
            "imageSets": []
        };

        $scope.newImageSetTemplate =
        {
            "id": "",
            "namespace": "",
            "name": "",
            "translations": [
                {
                    "locale": "",
                    "message": ""
                }
            ],
            "variants": [
                {
                    "id": "82759c6b-54a1-4842-8e75-772dfa4d72ec",
                    "name": "thumbnail",
                    "translations": [
                        {
                            "locale": "",
                            "message": "Thumbnail"
                        },
                        {
                            "locale": "en",
                            "message": "Thumbnail"
                        },
                        {
                            "locale": "nl",
                            "message": "Thumbnail"
                        }
                    ],
                    "width": 60,
                    "height": 60,
                    "upscaling": false
                },
                {
                    "id": "aeed4080-c60f-4a3c-ab0a-ad871004cbc3",
                    "name": "original",
                    "translations": [
                        {
                            "locale": "",
                            "message": "Original"
                        },
                        {
                            "locale": "en",
                            "message": "Original"
                        },
                        {
                            "locale": "nl",
                            "message": "Origineel"
                        }
                    ],
                    "width": 0,
                    "height": 0,
                    "upscaling": false
                }
            ]
        };

        /**
         * A
         * @param variant
         * @param language
         * @param message
         */
        $scope.addVariant = function () {
            var newVariant = angular.copy($scope.newVariantTemplate);
            newVariant.namespace = $scope.projectGalleryNamespace;
            newVariant.id = $scope.generateUUID();
            $scope.imageProcessor.variants.push(newVariant);
        };

        $scope.removeVariant = function (variant) {
            var index = $scope.imageProcessor.variants.indexOf(variant)
            if (index >= 0) {
                $scope.imageProcessor.variants.splice(index, 1);
            }
        };

        $scope.removeVariantFromImageSet = function (imageSet, variant) {
            var index = imageSet.variants.indexOf(variant)
            $log.info('removeVariantFromImageSet index:' + index);

            if (index >= 0) {
                imageSet.variants.splice(index, 1);
            }
        };

        $scope.addVariantToImageSet = function (imageSet, variant) {
            imageSet.variants.push(angular.copy(variant));
        };


        $scope.addVariantTranslation = function (variant, language, message) {
            variant.translations.push({"locale": language.locale, "message": message});
        };

        $scope.removeVariantTranslation = function (variant, translation) {
            var index = variant.translations.indexOf(translation)
            if (index >= 0) {
                variant.translations.splice(index, 1);
            }
        };


        $scope.addImageSet = function () {
            var newImageSet = angular.copy($scope.newImageSetTemplate);
            newImageSet.namespace = $scope.projectGalleryNamespace;
            newImageSet.id = $scope.generateUUID();
            $scope.imageSets.push(newImageSet);
        };

        $scope.removeImageSet = function (imageSet) {
            var index = $scope.imageSets.indexOf(imageSet)
            if (index >= 0) {
                $scope.imageSets.splice(index, 1);
            }
        };

        $scope.removeImageSetFromVariant = function (variant, imageSet) {
            var index = variant.imageSets.indexOf(imageSet)
            if (index >= 0) {
                variant.imageSets.splice(index, 1);
            }
        };

        $scope.addImageSetToVariant = function (variant, imageSet) {
            variant.imageSets.push(angular.copy(imageSet));
        };


        $scope.addImageSetTranslation = function (imageSet, language, message) {
            imageSet.translations.push({"locale": language.locale, "message": message});
        };

        $scope.removeImageSetTranslation = function (imageSet, translation) {
            var index = imageSet.translations.indexOf(translation)
            if (index >= 0) {
                imageSet.translations.splice(index, 1);
            }
        };


        $scope.calculateIdForVariant = function (variant) {
            return variant.id;
        };

        $scope.calculateIdForImageSet = function (imageSet) {
            return imageSet.id;
        };

        $scope.generateUUID = function () {
            var d = new Date().getTime();
            var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
                var r = (d + Math.random() * 16) % 16 | 0;
                d = Math.floor(d / 16);
                return (c == 'x' ? r : (r & 0x7 | 0x8)).toString(16);
            });
            return uuid;
        };


        $scope.updateImageSetsForVariant = function (variant) {
            for (var i = 0, length = $scope.imageSets.length; i < length; ++i) {
                var imageSet = $scope.imageSets[i];
                $scope.updateImageSetForVariant(imageSet, variant);
            }
        };

        $scope.updateImageSetForVariant = function (imageSet, variant) {
            $log.info('Update image set ' + imageSet.name + ' for: ' + variant.name);
            if ($scope.variantContainsImageSet(variant, imageSet) && !$scope.imageSetContainsVariant(imageSet, variant)) {
                $log.info('Add variant ' + variant.name + ' to image set: ' + imageSet.name);
                $scope.addVariantToImageSet(imageSet, variant);
            } else if (!$scope.variantContainsImageSet(variant, imageSet) && $scope.imageSetContainsVariant(imageSet, variant)) {
                $log.info('Remove variant ' + variant.name + ' from: ' + imageSet.name);
                $scope.removeVariantFromImageSet(imageSet, $scope.getVariantFromImageSet(imageSet, variant));
            }
        };

        $scope.variantContainsImageSet = function (variant, imageSet) {
            for (var i = 0, length = variant.imageSets.length; i < length; ++i) {
                if (variant.imageSets[i].id == imageSet.id) {
                    return true;
                }
            }
            return false;
        };
        $scope.getImageSetFromVariant = function (variant, imageSet) {
            for (var i = 0, length = variant.imageSets.length; i < length; ++i) {
                if (variant.imageSets[i].id == imageSet.id) {
                    return variant.imageSets[i];
                }
            }
            return null;
        };


        $scope.imageSetContainsVariant = function (imageSet, variant) {
            for (var i = 0, length = imageSet.variants.length; i < length; ++i) {
                if (imageSet.variants[i].id == variant.id) {
                    return true;
                }
            }
            return false;
        };
        $scope.getVariantFromImageSet = function (imageSet, variant) {
            for (var i = 0, length = imageSet.variants.length; i < length; ++i) {
                if (imageSet.variants[i].id == variant.id) {
                    return imageSet.variants[i];
                }
            }
            return null;
        };

        $scope.updateVariantsForImageSet = function (imageSet) {
            for (var i = 0, length = $scope.imageProcessor.variants.length; i < length; ++i) {
                var variant = $scope.imageProcessor.variants[i];
                $scope.updateVariantForImageSet(variant, imageSet);
            }
        };

        $scope.updateVariantForImageSet = function (variant, imageSet) {
            $log.info('Update variant ' + variant.name + ' for: ' + imageSet.name);
            if ($scope.imageSetContainsVariant(imageSet, variant) && !$scope.variantContainsImageSet(variant, imageSet)) {
                $log.info('Add image set ' + imageSet.name + ' to variant: ' + variant.name);
                $scope.addImageSetToVariant(variant, imageSet);
            } else if (!$scope.imageSetContainsVariant(imageSet, variant) && $scope.variantContainsImageSet(variant, imageSet)) {
                $log.info('Remove image set ' + imageSet.name + ' from variant: ' + variant.name);
                $scope.removeImageSetFromVariant(variant, $scope.getImageSetFromVariant(variant, imageSet));
            }
        };


        $scope.saveVariantsAndImageSets = function () {
            if (!$scope.validateVariants() || !$scope.validateImageSets()) {
                $scope.invalidated = true;
                $log.info('Unable to save image sets');
                return
            }
            $scope.invalidated = false;
            $log.info('Save image sets');
            $scope.saveGalleryProcessor();
            $scope.saveImageSets();
        };

        $scope.validateVariants = function () {
            for (var i = 0, length = $scope.imageProcessor.variants.length; i < length; ++i) {
                var variant = $scope.imageProcessor.variants[i];
                if (variant.name === "") {
                    return false;
                }
            }
            return true;
        };

        $scope.validateImageSets = function () {
            for (var i = 0, length = $scope.imageSets.length; i < length; ++i) {
                var imageSet = $scope.imageSets[i];
                if (imageSet.name === "") {
                    return false;
                }
            }
            return true;
        };

        $scope.openDeleteImageSetConfirmation = function ($scope, $modal, currentImageSet) {

            var modalInstance = $modal.open({
                templateUrl: 'deleteImageSetConfirmation.html',
                controller: ModalConfirmationCtrl,
                resolve: {
                    currentImageSet: function () {
                        return $scope.currentImageSet;
                    }
                }
            });

            modalInstance.result.then(function (currentImageSet) {
                $log.info('Delete: ' + currentImageSet.name);

            }, function () {
                $log.info('Modal dismissed at: ' + new Date());
            });
        };


        $scope.imageSetVariants2 = [
            {
                "name": "projectgallery:large",
                "translations": [
                    {
                        "locale": "",
                        "message": "Large"
                    },
                    {
                        "locale": "en",
                        "message": "Large"
                    },
                    {
                        "locale": "nl",
                        "message": "Groot"
                    }
                ],
                "width": 400,
                "height": 400,
                "properties": [
                    {
                        "name": "upscaling",
                        "value": "false"
                    }
                ]
            }
        ];


        $scope.upscalingValues = [
            {
                value: false,
                description: "default (off)",
                default: true
            },
            {
                value: true,
                description: "on"
            }
        ];
        $scope.defaultUpscalingValue = $scope.upscalingValues[0];
        $scope.upscalingValue = $scope.defaultUpscalingValue;


        $scope.optimizeValues = [
            {
                value: "quality",
                description: "default (quality)",
                default: true
            },
            {
                value: "speed",
                description: "speed"
            },
            {
                value: "speed.and.quality",
                description: "speed and quality"
            },
            {
                value: "best.quality",
                description: "best quality"
            },
            {
                value: "auto",
                description: "auto"
            }
        ];
        $scope.defaultOptimizeValue = $scope.optimizeValues[0];
        $scope.optimizeValue = $scope.defaultOptimizeValue;

        $scope.compressionValues = [
            {
                value: 1,
                description: "default (uncompressed)",
                default: true
            },
            {
                value: 0.95,
                description: "best"
            },
            {
                value: 0.9,
                description: "very good"
            },
            {
                value: 0.8,
                description: "good"
            },
            {
                value: 0.7,
                description: "medium"
            },
            {
                value: 0.5,
                description: "low"
            }
        ];
        $scope.defaultCompressionValue = $scope.compressionValues[0];
        $scope.compressionValue = $scope.defaultCompressionValue;

        $scope.init();

    }).controller('ModalConfirmationCtrl', function ($scope, $modalInstance, currentImageSet) {

        $scope.currentImageSet = currentImageSet;

        $scope.ok = function () {
            $modalInstance.close($scope.currentImageSet);
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    })
            .filter('hideHippoGalleryVariantsFilter', function () {
                return function (variants) {
                    var shownVariants = [];
                    angular.forEach(variants, function (variant) {
                        if (variant.namespace !== 'hippogallery') {
                            shownVariants.push(variant);
                        }
                    });
                    return shownVariants;
                };
            })
}());

/**
 Kenan todo:
 - add new gallery namespace cnd with postfix of current namespace name with initialize.
 - restservice gallery namespace retrieval
 **/