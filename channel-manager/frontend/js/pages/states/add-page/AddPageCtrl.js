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

    angular.module('hippo.channel.pages')

        .controller('hippo.channel.pages.AddPageCtrl', [
            '$scope',
            'hippo.channel.FeedbackService',
            'hippo.channel.pages.PrototypeService',
            'hippo.channel.pages.PageService',
            'hippo.channel.Container',
            'lowercaseFilter',
            'alphanumericFilter',
            function ($scope, FeedbackService, PrototypeService, PageService, ContainerService, lowercaseFilter, alphanumericFilter) {
                var updateURLAutomatically = true;

                $scope.page = {
                    title: '',
                    url: '',
                    prototype: {}
                };

                $scope.title = {
                    focus: true
                };

                $scope.validation = {
                    illegalCharacters: '/'
                };

                $scope.host = '';
                $scope.prototypes = [];

                $scope.submit = function () {
                    var pageModel = {
                        pageTitle: $scope.page.title,
                        name: $scope.page.url,
                        componentConfigurationId: $scope.page.prototype.id
                    };

                    PageService.savePage(pageModel).then(function (response) {
                        ContainerService.showPage(pageModel.name);
                    }, function (errorResponse) {
                        $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                        $scope.title.focus = true;
                    });
                };

                // fetch prototypes
                PrototypeService.getPrototypes().then(function (response) {
                    $scope.prototypes = response;
                    $scope.page.prototype = response[0];
                }, function (errorResponse) {
                    $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                });

                // fetch host
                PageService.getHost().then(function (response) {
                    $scope.host = response;
                }, function (errorResponse) {
                    $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                });

                // update url according to page title
                $scope.$watch('page.title', function (value) {
                    if (updateURLAutomatically) {
                        $scope.page.url = alphanumericFilter(lowercaseFilter(value));
                    }
                });

                $scope.disableAutomaticUrlUpdate = function () {
                    updateURLAutomatically = false;
                };
            }
        ]);
})();