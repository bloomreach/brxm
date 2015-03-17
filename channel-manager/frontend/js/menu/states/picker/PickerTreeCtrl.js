/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

    angular.module('hippo.channel.menu')
        .controller('hippo.channel.menu.PickerTreeCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            'hippo.channel.menu.PickerService',
            function ($scope, $state, $stateParams, PickerService) {
                $scope.$watch('selectedItem', function(item) {
                    if(item) {
                        $state.go('picker.docs', {
                            pickerTreeItemId: item.id
                        });
                    }
                });

                $scope.callbacks = {
                    displayTreeItem: function(item) {
                        return item.type === 'folder';
                    },
                    selectItem: function(item) {
                        if(!item.leaf && item.items.length < 1) {
                            PickerService.getData(item);
                            $scope.PickerCtrl.selectedDocument = null;
                        }
                    },
                    toggleItem: function(item) {
                        if(item.collapsed === false && item.items.length === 0) {
                            PickerService.getData(item);
                        }
                    }
                };
            }
        ]);
}());
