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
    angular.module('hippo.essentials')
        .controller('selectionPluginCtrl', function ($scope, $filter, $sce, $log, $rootScope, $http) {
            $scope.pluginId = "selectionPlugin";
            $scope.$on('update-plugin-install-state', function(event, args) {
                if ($scope.pluginId === args.pluginId && $scope.plugin !== undefined) {
                    $scope.plugin.installState = args.state;
                }
            });

            var restEndpoint = $rootScope.REST.dynamic + 'selectionplugin/';
            $scope.tickle = function() {
                $http.post(restEndpoint, {}).success(function (data) {
                    alert('Hurray!');
                });
            };
            $scope.showDocument = function(documentType) {
                return documentType.name !== 'basedocument';
            };

            $scope.positionMap = {
                '${cluster.id}.right': 'right',
                '${cluster.id}.left' : 'left'
            };
            $scope.positionName = function(pos) {
                return $scope.positionMap[pos];
            };
            $scope.fieldPosition = '${cluster.id}.right'; // default to adding selection fields in the right column
            $scope.selectionTypes = [ 'single', 'multiple' ];
            $scope.selectionType = 'single';

            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.plugin = plugin;
            });
            $http.get($rootScope.REST.documents).success(function (data){
                $scope.documentTypes = data;
            });
            loadValueLists();

            function loadValueLists() {
                $http.get($rootScope.REST.documents + "selection:valuelist").success(function (data) {
                    $scope.valueLists = data;
                });
            }
        })
})();
