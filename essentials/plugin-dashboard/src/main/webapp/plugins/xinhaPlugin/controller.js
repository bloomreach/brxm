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
            .controller('xinhaPluginCtrl', function ($scope, $sce, $log, $rootScope, $http) {

                $scope.save = function () {
                    // fetch existing settings:
                    var payload = Essentials.addPayloadData("path", "/hippo:namespaces/hippostd/html/editor:templates/_default_/root");
                    Essentials.addPayloadData("property", "Xinha.config.toolbar", payload);
                    Essentials.addPayloadData("multiple", "true", payload);
                    for (var i = 0; i < $scope.options.length; i++) {
                        var option = $scope.options[i];
                        if (option.checked) {
                            Essentials.addPayloadData(option.name, option.name, payload);
                        }
                    }
                    $http.post($rootScope.REST.setProperty, payload).success(function (data) {
                        $scope.init();
                    });


                };

                $scope.unselect = function () {
                    for (var i = 0; i < $scope.options.length; i++) {
                        var option = $scope.options[i];
                        option.checked = false;

                    }
                };

                $scope.select = function () {
                    for (var i = 0; i < $scope.options.length; i++) {
                        var option = $scope.options[i];
                        option.checked = true;
                    }
                };
                $scope.init = function () {

                    // fetch existing settings:
                    var payload = Essentials.addPayloadData("path", "/hippo:namespaces/hippostd/html/editor:templates/_default_/root");
                    Essentials.addPayloadData("property", "Xinha.config.toolbar", payload);
                    $http.post($rootScope.REST.getProperty, payload).success(function (data) {
                        var items = data.items;
                        if (items) {
                            for (var i = 0; i < items.length; i++) {
                                var item = items[i].key;
                                for (var k = 0; k < $scope.options.length; k++) {
                                    var option = $scope.options[k];
                                    var name = option.name;
                                    if (name == item) {
                                        option.checked = true;
                                    }
                                }
                            }
                        }
                    });


                };


                $scope.options = [
                    {name: 'fullscreen', checked: false, title: 'fullscreen'},
                    {name: 'createlink', checked: false, title: 'createlink'},
                    {name: 'createexternallink', checked: false, title: 'createexternallink'},
                    {name: 'formatblock', checked: false, title: 'formatblock'},
                    {name: 'bold', checked: false, title: 'bold'},
                    {name: 'italic', checked: false, title: 'italic'},
                    {name: 'underline', checked: false, title: 'underline'},
                    {name: 'insertorderedlist', checked: false, title: 'insertorderedlist'},
                    {name: 'insertunorderedlist', checked: false, title: 'insertunorderedlist'},
                    {name: 'insertimage', checked: false, title: 'insertimage'},
                    {name: 'undo', checked: false, title: 'undo'},
                    {name: 'redo', checked: false, title: 'redo'},
                    {name: 'separator', checked: false, title: 'separator'},
                    {name: 'inserttable', checked: false, title: 'inserttable'},
                    {name: 'toggleborders', checked: false, title: 'toggleborders'},
                    {name: 'TO-table-prop', checked: false, title: 'TO-table-prop'},
                    {name: 'TO-row-prop', checked: false, title: 'TO-row-prop'},
                    {name: 'TO-row-insert-above', checked: false, title: 'TO-row-insert-above'},
                    {name: 'TO-row-insert-under', checked: false, title: 'TO-row-insert-under'},
                    {name: 'TO-row-delete', checked: false, title: 'TO-row-delete'},
                    {name: 'TO-row-split', checked: false, title: 'TO-row-split'},
                    {name: 'TO-col-insert-before', checked: false, title: 'TO-col-insert-before'},
                    {name: 'TO-col-insert-after', checked: false, title: 'TO-col-insert-after'},
                    {name: 'TO-col-delete', checked: false, title: 'TO-col-delete'},
                    {name: 'TO-col-split', checked: false, title: 'TO-col-split'},
                    {name: 'TO-cell-prop', checked: false, title: 'TO-cell-prop'},
                    {name: 'TO-cell-insert-before', checked: false, title: 'TO-cell-insert-before'},
                    {name: 'TO-cell-insert-after', checked: false, title: 'TO-cell-insert-after'},
                    {name: 'TO-cell-delete', checked: false, title: 'TO-cell-delete'},
                    {name: 'TO-cell-merge', checked: false, title: 'TO-cell-merge'},
                    {name: 'TO-cell-split', checked: false, title: 'TO-cell-split'},
                    {name: 'subscript', checked: false, title: 'subscript'},
                    {name: 'superscript', checked: false, title: 'superscript'},
                    {name: 'insertcharacter', checked: false, title: 'insertcharacter'},
                    {name: 'pastetext', checked: false, title: 'pastetext'},
                    {name: 'htmlmode', checked: false, title: 'htmlmode'}
                ];
                //############################################
                // init code
                //############################################

                $scope.init();


            });


}());