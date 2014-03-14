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

    function prefixWithSlash(str) {
        if (str === '' || str.charAt(0) !== '/') {
            return '/' + str;
        }
        return str;
    }

    angular.module('hippo.channel.page')

        .controller('hippo.channel.page.OverviewPagesCtrl', [
            '$scope',
            '$state',
            '$translate',
            'hippo.channel.FeedbackService',
            'hippo.channel.ConfigService',
            'hippo.channel.page.PageService',
            '_hippo.channel.IFrameService',
            function ($scope, $state, $translate, FeedbackService, ConfigService, PageService, IFrameService) {
                // scope variables
                $scope.pages = [];
                $scope.tooltip = '';
                $scope.userCanEdit = ConfigService.userCanEdit;
                $scope.userIsEditing = ConfigService.userIsEditing;

                // fetch pages
                PageService.getPages().then(function (pages) {
                    $scope.pages = pages;
                }, function (errorResponse) {
                    $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
                });

                $scope.showTooltip = function() {
                    if (!$scope.userIsEditing) {
                        $scope.tooltip = $translate('ADD_NEW_PAGE_DISABLED_TOOLTIP');
                    }
                };

                $scope.hideTooltip = function() {
                    $scope.tooltip = '';
                };

                $scope.showPage = function(page) {
                    var iframePanel = IFrameService.getContainer();
                    iframePanel.iframeToHost.publish('browseTo', prefixWithSlash(page.pathInfo));
                };
            }
        ]);
}());
