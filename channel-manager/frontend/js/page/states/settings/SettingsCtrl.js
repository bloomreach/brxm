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
            'hippo.channel.FeedbackService',
            'hippo.channel.PageService',
            'hippo.channel.Container',
            'lowercaseFilter',
            'alphanumericFilter',
            function ($scope, FeedbackService, PageService, ContainerService, lowercaseFilter, alphanumericFilter) {
                $scope.page = {
                    title: '',
                    url: ''
                };

                $scope.isPageEditable = false;

                $scope.title = {
                    focus: true
                };

                $scope.validation = {
                    illegalCharacters: '/ :'
                };

                $scope.host = '';

                // fetch host
                PageService.getHost().then(function (response) {
                    $scope.host = response;
                }, function (errorResponse) {
                    $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                });

                // fetch page
                PageService.getCurrentPage().then(function (currentPage) {
                    $scope.page.title = currentPage.pageTitle;
                    $scope.page.url = currentPage.name;

                    // only pages whose sitemap item is located in the HST workspace are editable
                    $scope.isPageEditable = currentPage.workspaceConfiguration;
                }, function (errorResponse) {
                    $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                });

                $scope.submit = function () {
                    var pageModel = {
                        id: $scope.page.id,
                        pageTitle: $scope.page.title,
                        name: $scope.page.url
                    };

                    PageService.updatePage(pageModel).then(function (response) {
                        ContainerService.showPage(pageModel.name);
                    }, function (errorResponse) {
                        $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                        $scope.title.focus = true;
                    });
                };

                $scope.closeContainer = function() {
                    ContainerService.performClose();
                };
            }
        ]);
}());
