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

    angular.module('hippo.essentials').controller('galleryPluginCtrl', function ($scope, $sce, $log, $rootScope, $http) {

        var endpoint = $rootScope.REST.dynamic + "galleryplugin";
        $scope.imageSets = [];
        $scope.selectedImageSet = null;
        $scope.imageVariantName = null;
        $scope.selectedImageModel = null;

        $scope.addImageSet = function () {
            var payload = Essentials.addPayloadData("imageSetPrefix", $scope.imageSetPrefix, null);
            Essentials.addPayloadData("imageSetName", $scope.imageSetName, payload);
            $http.post(endpoint + "/create", payload).success(function () {
                loadImageSets(true);
            });
        };
        $scope.resetSelections = function () {
            $scope.selectedImageModel = null;
            $scope.selectedImageSet = null;
            $scope.imageVariantName = null;
        };

        $scope.onSelectedImageSetChange = function () {
            $scope.selectedImageModel = null;
        };

        $scope.addImageVariant = function () {
            var exists = false;
            angular.forEach($scope.selectedImageSet.models, function (model) {
                if (model.name == $scope.imageVariantName) {
                    exists = true;
                }
            });
            if (exists) {
                displayError("Image variant with name: " + $scope.imageVariantName + " already exists");
            }
            var payload = Essentials.addPayloadData("imageVariantName", $scope.imageVariantName, null);
            Essentials.addPayloadData("selectedImageSet", $scope.selectedImageSet.name, payload);
            $http.post(endpoint + "/addvariant", payload).success(function (data) {
                loadImageSets(true);
            });
        };

        $scope.addTranslation = function () {
            if ($scope.selectedImageModel) {
                if (!$scope.selectedImageModel.translations) {
                    $scope.selectedImageModel.translations = [];
                }
                $scope.selectedImageModel.translations.push({"language": "", "message": ""});
            }
        };

        $scope.removeTranslation = function (translation) {
            var idx = $scope.selectedImageModel.translations.indexOf(translation);
            $scope.selectedImageModel.translations.splice(idx, 1);
        };
        $scope.removeImageVariant = function (variant) {
            if (variant.readOnly) {
                displayError("Cannot remove " + variant.name + " because it is required");
                return;
            }
            $http.post(endpoint + "/remove", variant).success(function () {
                loadImageSets(true);
            });
        };
        $scope.save = function () {
            // save only saves translations, advanced settings and height/width:
            $http.post(endpoint + "/update", $scope.selectedImageModel).success(function () {
                loadImageSets(false);
            });


        };

        $scope.init = function () {
            loadImageSets(true);
        };

        function loadImageSets(reset) {
            $http.get(endpoint).success(function (data) {
                if (reset) {
                    $scope.resetSelections();
                }
                $scope.imageSets = data;
            });
        }

        //############################################
        // INIT APP
        //############################################

        $scope.init();

        //############################################
        // UTIL
        //############################################

        function displayError(msg) {
            $rootScope.globalError = [];
            $rootScope.feedbackMessages = [];
            $rootScope.globalError.push(msg);
        }

        //############################################
        // DEFAULTS
        //############################################
        $scope.locales = [
            {"locale": "en", "description": "English"},
            {"locale": "fr", "description": "Français"},
            {"locale": "nl", "description": "Nederlands"},
            {"locale": "it", "description": "Italiano"},
            {"locale": "de", "description": "Deutsch"}
        ];
        $scope.compressionValues = [
            {value: 1, description: "default (uncompressed)", selected: true},
            {value: 0.95, description: "best"},
            {value: 0.9, description: "very good"},
            {value: 0.8, description: "good"},
            {value: 0.7, description: "medium"},
            {value: 0.5, description: "low"}
        ];

        $scope.optimizeValues = [
            {value: "quality", description: "default (quality)", selected: true},
            {value: "speed", description: "speed"},
            {value: "speed.and.quality", description: "speed and quality"},
            {value: "best.quality", description: "best quality"},
            {value: "auto", description: "auto"}
        ];

        $scope.upscalingValues = [
            {value: false, description: "default (off)", selected: true},
            {value: true, description: "on"}
        ];

    })
}());
