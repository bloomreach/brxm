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

        .controller('hippo.channel.menu.PickerCtrl', [
            '$scope',
            '$state',
            '$stateParams',
            function ($scope, $state, $stateParams) {
                $scope.cancelPicker = function() {
                    $state.go('menu-item.edit', {
                        menuItemId: $stateParams.menuItemId
                    });
                };
                $scope.pickerTreeItems = [
                    {
                        collapsed: true,
                        hasFolders: true,
                        id: 'item-a',
                        title: 'Item A',
                        items: [
                            {
                                collapsed: true,
                                id: 'item-a1',
                                title: 'Item A.1',
                                items: []
                            },
                            {
                                collapsed: true,
                                hasFolders: true,
                                id: 'item-a2',
                                title: 'Item A.2 has a very long name that probably will not fit on a single row when tree becomes very small.' +
                                        'Words are fun, lots of words. Please more words, lots of words.',
                                items: [
                                    {
                                        collapsed: true,
                                        hasFolders: true,
                                        id: 'item-2-1',
                                        title: 'Item A.2.1',
                                        items: [
                                            {
                                                collapsed: true,
                                                id: 'item-2-1-1',
                                                title: 'Item A.2.1.1',
                                                items: []
                                            }
                                        ]
                                    },
                                    {
                                        collapsed: true,
                                        id: 'item-2-2',
                                        title: 'Item A.2.2',
                                        items: []
                                    }
                                ]
                            },
                            {
                                collapsed: true,
                                id: 'item-a3',
                                title: 'Item A.3',
                                items: []
                            }
                        ]
                    },
                    {
                        collapsed: true,
                        id: 'item-b',
                        title: 'Item B',
                        items: []
                    },
                    {
                        collapsed: true,
                        hasFolders: true,
                        id: 'item-c',
                        title: 'Item C',
                        items: [
                            {
                                collapsed: true,
                                id: 'item-c1',
                                title: 'Item C.1',
                                items: []
                            }
                        ]
                    }
                ];
            }
        ]);
}());
