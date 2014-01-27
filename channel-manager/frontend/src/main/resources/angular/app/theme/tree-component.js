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
         * This directive is a modification of
         * https://github.com/nickperkinslondon/angular-bootstrap-nav-tree
         */
        .directive('fileTree', ['$timeout', function($timeout) {
            return {
                restrict: 'A',
                template: '<div class="list-group abn-tree">' +
                    '<a data-ng-repeat="row in treeRows | filter:{visible:true} track by row.branch.uid"' +
                    'ng-animate="\'abn-tree-animate\'" ng-class="\'level-\' + {{ row.level }} + (row.branch.selected ? \' active\':\'\')"' +
                    'class="list-group-item abn-tree-row level-1" ng-click="selectBranch(row.branch)">' +
                    '<i class="indented fa" data-ng-class="row.tree_icon" data-ng-click="row.branch.expanded = !row.branch.expanded"></i>' +
                    '<span class="indented tree-label">{{ row.name }}</span>' +
                    '<span class="badge"><i class="fa fa-bars"></i></span>' +
                    '</a>' +
                    '</div>',
                scope: {
                    treeData: '=',
                    onSelect: '&',
                    initialSelection: '='
                },
                link: function (scope, element, attrs) {
                    var expandLevel, selectedBranch, treeData;

                    // default options
                    attrs.iconExpand = attrs.iconExpand || 'fa-plus-square-o';
                    attrs.iconCollapse = attrs.iconCollapse || 'fa-minus-square-o';
                    attrs.iconLeaf = attrs.iconLeaf || 'fa-file-text';
                    attrs.expandLevel = parseInt(attrs.expandLevel) || 5;

                    // validation
                    if (!scope.treeData) {
                        console.warn('No treeData defined for the tree');
                        return;
                    }

                    if (!scope.treeData.length) {
                        if (treeData && treeData.name) {
                            scope.treeData = [ treeData ];
                        } else {
                            console.warn('treeData should be an array of root branches');
                            return;
                        }
                    }

                    // scope values
                    scope.treeRows = [];

                    // public methods
                    scope.selectBranch = function(branch) {
                        if (branch !== selectedBranch) {
                            selectBranch(branch);
                        }
                    };

                    function selectBranch(branch) {
                        if (branch !== selectedBranch) {
                            if (selectedBranch) {
                                selectedBranch.selected = false;
                            }

                            branch.selected = true;
                            selectedBranch = branch;

                            if (branch.onSelect) {
                                $timeout(function() {
                                    branch.onSelect(branch);
                                });
                            } else {
                                if (scope.onSelect) {
                                    $timeout(function() {
                                        scope.onSelect({
                                            branch: branch
                                        });
                                    });
                                }
                            }
                        }
                    }

                    // general method used to loop through the tree structure
                    function forEachBranch(f) {
                        function doF(branch, level) {
                            f(branch, level);

                            if (branch.children) {
                                for (var i = 0; i < branch.children.length; i++) {
                                    doF(branch.children[i], level + 1);
                                }
                            }
                        }

                        for (var i = 0; i < scope.treeData.length; i++) {
                            doF(scope.treeData[i], 1);
                        }
                    }

                    // make sure each branch has a name and a children property
                    function formatBranchStructure(branch) {
                        if (branch.children && branch.children.length > 0) {
                            branch.children = branch.children.map(function(e) {
                                if (typeof e === 'string') {
                                    return {
                                        name: e,
                                        children: []
                                    };
                                } else {
                                    return e;
                                }
                            });
                        } else {
                            branch.children = [];
                        }
                    }

                    function addBranchToList(level, branch, visible) {
                        var treeIcon;

                        if (!branch.expanded) {
                            branch.expanded = false;
                        }

                        if (!branch.children || branch.children.length === 0) {
                            treeIcon = attrs.iconLeaf;
                        } else {
                            treeIcon = (branch.expanded) ? attrs.iconCollapse : attrs.iconExpand;
                        }

                        scope.treeRows.push({
                            'level': level,
                            'branch': branch,
                            'name': branch.name,
                            'tree_icon': treeIcon,
                            'visible': visible
                        });

                        if (branch.children) {
                            for (var i = 0; i < branch.children.length; i++) {
                                var childVisible = visible && branch.expanded;
                                addBranchToList(level + 1, branch.children[i], childVisible);
                            }
                        }
                    }

                    function onTreedataChange() {
                        scope.treeRows = [];

                        forEachBranch(function(branch) {
                            formatBranchStructure(branch);

                            if (!branch.uid) {
                                branch.uid = '' + Math.random();
                            }
                        });

                        for (var i = 0; i < scope.treeData.length; i++) {
                            addBranchToList(1, scope.treeData[i], true);
                        }

                        onInitialSelectionChange();
                    }

                    forEachBranch(function(b, level) {
                        b.level = level;
                        b.expanded = b.level < expandLevel;
                    });

                    if (attrs.initialSelection) {
                        console.log('Initial selection value: ' + attrs.initialSelection);

                        forEachBranch(function (b) {
                            if (b.id === attrs.initialSelection) {
                                selectBranch(b);
                            }
                        });
                    }

                    function onInitialSelectionChange() {
                        console.log('Initial selection change.');
                        console.log(scope.initialSelection);

                        forEachBranch(function (b) {
                            console.log(b.id + ': ' + scope.initialSelection);
                            if (b.id === scope.initialSelection) {
                                selectBranch(b);
                            }
                        });
                    }

                    // watch for data changes
                    scope.$watch('treeData', onTreedataChange, true);
                    scope.$watch('initialSelection', onInitialSelectionChange, true);
                }
            };
        }]);

})();