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

    angular.module('hippo.channelManager.menuManager')

        .controller('hippo.channelManager.menuManager.AddPageFormCtrl', [
            '$scope',
            '$routeParams',
            '$location',
            '$log',
            function ($scope, $routeParams, $location, $log) {
                $scope.templates = [{name: 'Template 1'}, {name: 'TODO: fetch templates via HTTP-call'}];

                $scope.cancel = function () {
                    $location.path($routeParams.menuItemId + '/edit');
                };

                $scope.submit = function (page) {
                    // TODO: save page implementation
                    $log.info('Submit add page form');
                    $log.info(page);

                    // redirect to the active menu item with the new page data as destination
                };
            }
        ]);
}());