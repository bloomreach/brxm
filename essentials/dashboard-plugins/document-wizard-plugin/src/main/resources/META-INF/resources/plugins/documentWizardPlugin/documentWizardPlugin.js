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
        .controller('documentWizardCtrl', function ($scope, $filter, $sce, $log, $modal, $rootScope, $http) {
            var endpoint = $rootScope.REST.dynamic + 'documentwizard/';
            $scope.pluginId = "documentWizardPlugin";

            $scope.showDialog = false;
            $scope.valueListPath = null;
            $scope.selectedDocument = null;
            $scope.shortcutName = null;
            $scope.baseFolder = null;
            $scope.classificationType = "list";
            $scope.classificationTypes = ["date", "list"];
            $scope.addOk = function () {
                $scope.showDialog = false;
                console.log("OK");
            };
            $scope.addCancel = function () {
                $scope.showDialog = false;
                console.log("cancel");
            };
            $scope.addWizard = function () {
                var payload = Essentials.addPayloadData("selectedDocument", $scope.selectedDocument, null);
                Essentials.addPayloadData("classificationType", $scope.classificationType, payload);
                Essentials.addPayloadData("baseFolder", $scope.baseFolder, payload);
                Essentials.addPayloadData("shortcutName", $scope.baseFolder, payload);
                $scope.showDialog = true;
                console.log($scope.showDialog);
                /*$http.post($rootScope.REST, payload).success(function (data) {

                 });
                 */

            };


            // default visibility of the dialog
            $scope.dialog = {
                visible: false
            };

            // the message is supposed to come from the ContainerService, that handles
            // the communication with the iFrame
            $rootScope.$on('close-confirmation:show', function () {
                $scope.$apply(function () {
                    $scope.dialog.visible = true;
                });
            });

            // confirm - close the panel
            $scope.confirm = function () {
                console.log("Configrm");
            };

            // cancel - hide the dialog
            $scope.cancel = function () {
                $scope.dialog.visible = false;
            };


            //############################################
            // INIT
            //############################################
            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
            });
            $http.get($rootScope.REST.documents).success(function (data) {
                $scope.documentTypes = data;
            });
            $http.get("http://localhost:8080/essentials/rest/jcrbrowser/folders").success(function (data) {
                $scope.treeItems = data.items;
            });


            //############################################
            // TREE
            //############################################

            $scope.callbacks = {
                accept: function () {
                    return false;
                },
                dragStart: function (event) {
                    var sourceItem = event.source.nodeScope.$modelValue;
                    $scope.selectedItem = sourceItem;
                    $scope.baseFolder = sourceItem.id;
                }
            };
            //############################################
            // MODAL
            //############################################

            $scope.open = function (size) {

                var modalInstance = $modal.open({
                    templateUrl: 'tree-picker.html',
                    controller: ModalInstanceCtrl,
                    size: size
                });

                modalInstance.result.then(function (selectedItem) {
                    $scope.selected = selectedItem;
                }, function () {
                    $log.info('Modal dismissed at: ' + new Date());
                });
            };

            var ModalInstanceCtrl = function ($scope, $modalInstance) {


                $http.get("http://localhost:8080/essentials/rest/jcrbrowser/folders").success(function (data) {
                    $scope.treeItems = data.items;
                });

                $scope.ok = function () {
                    $modalInstance.close($scope.selected.item);
                };

                $scope.cancel = function () {
                    $modalInstance.dismiss('cancel');
                };


                $scope.callbacks = {
                    accept: function (sourceNodeScope, destNodesScope, destIndex) {
                        return destNodesScope && !destNodesScope.nodrop;
                    },

                    dragStart: function (event) {
                        var sourceItem = event.source.nodeScope.$modelValue;
                        $log.info('start dragging ' + sourceItem.title);
                        $scope.selectedItem = sourceItem;
                    },

                    dragStop: function (event) {
                        var sourceItem = event.source.nodeScope.$modelValue;
                        $log.info('stop dragging ' + sourceItem.title);
                    },

                    dropped: function (event) {
                        var source = event.source,
                            dest = event.dest;
                        if (source.nodeScope && dest.nodesScope) {
                            var sourceItem = source.nodeScope.$modelValue;
                            var destItem = dest.nodesScope.$nodeScope ? dest.nodesScope.$nodeScope.$modelValue : {title: 'root'};
                            var destIndex = dest.index;
                            $log.info('dropped ' + sourceItem.title + ' into ' + destItem.title + ' at index ' + destIndex);
                        }
                    }
                };

            };


        })
})();
