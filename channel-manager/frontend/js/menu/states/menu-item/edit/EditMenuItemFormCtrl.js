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

    angular.module('hippo.channel.menu')

        .controller('hippo.channel.menu.EditMenuItemFormCtrl', [
            '$scope',
            '$translate',
            'hippo.channel.FormStateService',
            'hippo.channel.Container',
            'hippo.channel.menu.FocusService',
            'hippo.channel.menu.MenuService',
            function ($scope, $translate, FormStateService, ContainerService, FocusService, MenuService) {
                $scope.focus = FocusService.focusElementWithId;

                // The following logic will check the client-side validation of the
                // edit menu item form. When a field is invalid, the FormStateService
                // will be updated, so the window can't be closed.
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

                    FormStateService.setValid(isValid);
                }

                $scope.$watch('form.$dirty', function () {
                    FormStateService.setDirty($scope.form.$dirty);
                });

                // The following logic will set the correct error messages when the
                // client-side validation status updates.
                $scope.$watch('form.title.$error.required', function () {
                    $scope.$parent.fieldFeedbackMessage.title = $translate('LABEL_REQUIRED');
                });

                $scope.$watch('form.title.$valid', function (value) {
                    if (value) {
                        $scope.$parent.fieldFeedbackMessage.title = '';
                    }
                });

                $scope.$watch('form.url.$error.required', function () {
                    if (!$scope.form.url.$pristine) {
                        $scope.$parent.fieldFeedbackMessage.link = $translate('EXTERNAL_LINK_REQUIRED');
                    }
                });

                $scope.$watch('form.url.$valid', function (value) {
                    if (value) {
                        $scope.$parent.fieldFeedbackMessage.link = '';
                    }
                });

                function saveFieldIfDirty(fieldName, propertyName) {
                    if ($scope.form[fieldName].$dirty) {
                        $scope.$parent.saveSelectedMenuItem(propertyName);
                    }
                }

                $scope.$on('container:before-close', function (event) {
                    if ($scope.form.$dirty) {
                        saveFieldIfDirty('title', 'title');
                        saveFieldIfDirty('sitemapItem', 'link');
                        saveFieldIfDirty('url', 'link');
                    }
                });

                $scope.$on('container:close', function (event) {
                    // prevent close, process all menu changes first and then trigger the close ourselves
                    event.preventDefault();
                    MenuService.processAllChanges().then(function() {
                        ContainerService.performClose();
                    });
                });
            }
        ]);
}());
