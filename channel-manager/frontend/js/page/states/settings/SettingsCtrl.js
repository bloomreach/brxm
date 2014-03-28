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

    angular.module('hippo.channel.page')

        .controller('hippo.channel.page.SettingsCtrl', [
            '$scope',
            '$filter',
            'hippo.channel.FeedbackService',
            'hippo.channel.PageService',
            'hippo.channel.PrototypeService',
            'hippo.channel.ConfigService',
            'hippo.channel.Container',
            function ($scope, $filter, FeedbackService, PageService, PrototypeService, ConfigService, ContainerService) {
                var translate = $filter('translate');

                $scope.page = {
                    id: null,
                    title: '',
                    url: '',
                    prototype: {
                        id: null
                    }
                };

                $scope.state = {
                    isEditable: false,
                    isLocked: false
                };

                $scope.lock = {
                    owner: null,
                    timestamp: null
                };

                $scope.template = {
                    isVisible: false
                };

                $scope.validation = {
                    illegalCharacters: '/ :'
                };

                $scope.host = '';
                $scope.isConfirmationVisible = false;
                $scope.isHomePage = false;
                $scope.tooltip = '';

                // error feedback
                function setErrorFeedback(errorResponse) {
                    $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                }

                // fetch data
                loadHost()
                    .then(loadPage)
                    .then(loadPrototypes);

                $scope.submit = function () {
                    var pageModel = {
                        id: $scope.page.id,
                        pageTitle: $scope.page.title,
                        name: $scope.page.url,
                        componentConfigurationId: $scope.page.prototype.id
                    };

                    PageService.updatePage(pageModel).then(function () {
                        ContainerService.showPage(pageModel.name);
                    }, setErrorFeedback);
                };

                $scope.closeContainer = function() {
                    ContainerService.performClose();
                };

                $scope.showTooltip = function() {
                    if (!$scope.state.isEditable) {
                        $scope.tooltip = translate('TOOLTIP_NOT_EDITABLE');
                    } else if ($scope.page.isHomePage) {
                        $scope.tooltip = translate('TOOLTIP_IS_HOMEPAGE');
                    } else {
                        $scope.tooltip = '';
                    }
                };

                $scope.hideTooltip = function() {
                    $scope.tooltip = '';
                };

                $scope.delete = function () {
                    $scope.isConfirmationVisible = true;
                };

                $scope.confirmDelete = function () {
                    $scope.isConfirmationVisible = false;

                    PageService.deletePage($scope.page.id).then(function () {
                        ContainerService.showPage('/');
                    }, setErrorFeedback);
                };

                $scope.cancelDelete = function () {
                    $scope.isConfirmationVisible = false;
                };

                function loadHost() {
                    return PageService.getHost()
                        .then(function (host) {
                            $scope.host = host;
                            return host;
                        }, setErrorFeedback);
                }

                function loadPrototypes() {
                    return PrototypeService.getPrototypes()
                        .then(function (prototypes) {
                            $scope.prototypes = prototypes;
                            return prototypes;
                        }, setErrorFeedback);
                }

                function loadPage() {
                    return PageService.getCurrentPage()
                        .then(function (currentPage) {
                            $scope.page.id = currentPage.id;
                            $scope.page.title = currentPage.pageTitle;
                            $scope.page.url = currentPage.name;
                            $scope.page.hasContainerItem = currentPage.hasContainerItemInPageDefinition;
                            $scope.page.isHomePage = currentPage.isHomePage;

                            // only pages whose sitemap item is located in the HST workspace are editable,
                            // unless they are locked by someone else
                            $scope.state.isLocked = angular.isString(currentPage.lockedBy) && currentPage.lockedBy !== ConfigService.cmsUser;
                            $scope.state.isEditable = !$scope.state.isLocked && currentPage.workspaceConfiguration;

                            // lock information
                            $scope.lock.owner = currentPage.lockedBy;
                            $scope.lock.timestamp = currentPage.lockedOn;

                            return currentPage;
                        }, setErrorFeedback);
                }
            }
        ]);
}());
