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

    angular.module('hippo.theme')

        /*
         * jstree directive
         * via http://plnkr.co/edit/xHIc4J?p=preview
         */
        .directive('jstree', [function() {
            /*jslint unparam: true*/
            return {
                restrict: 'A',
                scope: {
                    data: "="
                },
                template: '<div id="filter">Filter did not load.</div>',
                controller: function($scope) {
                    this.setSelectedItem = function(itemId) {
                        $scope.$parent.setSelectedItemId(itemId);
                    };
                },
                link: function (scope, element, attrs, treeCtrl) {
                    scope.$watch('data', function() {
                        element.jstree('destroy');
                        element.jstree({ 'core' : {
                            data: scope.data
                        }}).bind('select_node.jstree', function(event, item) {
                            treeCtrl.setSelectedItem(item.node.id);
                        }).jstree('select_node', scope.data[0].id);
                    }, true);
                }
            };
        }]);
}());