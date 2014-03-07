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
    'use strict';

    angular.module('hippo.channelManager.menuManager')

        .controller('hippo.channelManager.menuManager.EditMenuItemFormCtrl', [
            '$scope',
            'hippo.channelManager.menuManager.FormValidationService',
            'hippo.channelManager.menuManager.FocusService',
            function ($scope, FormValidationService, FocusService) {
                // focus fields
                $scope.focus = FocusService.focusElementWithId;

                // form validation service
                $scope.$watch('form.title.$valid', function () {
                    checkFormValidity();
                });

                $scope.$watch('selectedMenuItem.linkType', function () {
                    checkFormValidity();
                });

                $scope.$watch('form.sitemapItem.$valid', function () {
                    checkFormValidity();
                });

                $scope.$watch('form.url.$valid', function () {
                    checkFormValidity();
                });

                function checkFormValidity() {
                    var isValid = $scope.form.title.$valid &&
                        $scope.form.destination.$valid;

                    if ($scope.selectedMenuItem.linkType === 'EXTERNAL') {
                        isValid = isValid && ($scope.form.url.$valid);
                    } else if ($scope.selectedMenuItem.linkType === 'SITEMAPITEM') {
                        isValid = isValid && ($scope.form.sitemapItem.$valid);
                    }

                    FormValidationService.setValidity(isValid);
                }
            }
        ]);
}());