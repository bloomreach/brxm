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
            '$translate',
            'hippo.channelManager.FormValidationService',
            'hippo.channelManager.menuManager.FocusService',
            function ($scope, $translate, FormValidationService, FocusService) {
                $scope.focus = FocusService.focusElementWithId;

                // The following logic will check the client-side validation of the
                // edit menu item form. When a field is invalid, the FormValidationService
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

                    FormValidationService.setValidity(isValid);
                }

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
            }
        ]);
}());