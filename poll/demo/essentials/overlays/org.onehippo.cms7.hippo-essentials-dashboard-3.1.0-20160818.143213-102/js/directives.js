/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
        //############################################
        // DIRECTIVES
        //############################################
        .directive("essentialsFolderPicker", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    title: '@',
                    buttonText: '@',
                    endPoint: '@',
                    selectedPath: '=',
                    selected: '='
                },
                templateUrl: 'directives/essentials-folder-picker.html',
                controller: function ($scope, $rootScope, $uibModal, $log, $http) {
                    $scope.open = function (size) {
                        var modalInstance = $uibModal.open({
                            templateUrl: 'tree-picker.html',
                            controller: ModalInstanceCtrl,
                            size: size,
                            resolve: {
                                endPoint: function () {
                                    return $scope.endPoint;
                                }, title: function () {
                                    return $scope.title;
                                }, buttonText: function () {
                                    return $scope.buttonText;
                                }, selectedPath: function () {
                                    return $scope.selectedPath;
                                }, selected: function () {
                                    return $scope.selected;
                                }
                            }

                        });
                        modalInstance.result.then(function (selected) {
                            if (selected) {
                                $scope.selected = selected;
                                $scope.selectedPath = selected.id;
                            }
                        });
                    };


                    //############################################
                    // MODAL
                    //############################################
                    var ModalInstanceCtrl = function ($scope, $uibModalInstance, endPoint, title) {
                        $scope.title = title;
                        $http.get(endPoint).success(function (data) {
                            $scope.treeItems = data.items;
                        });
                        $scope.ok = function () {
                            $uibModalInstance.close($scope.selected);
                        };

                        $scope.cancel = function () {
                            $uibModalInstance.dismiss('cancel');
                        };
                        $scope.callbacks = {
                            accept: function () {
                                // disable drag/drop stuff
                                return false;
                            },
                            dragStart: function (event) {
                                $scope.selected = event.source.nodeScope.$modelValue;
                                $scope.selectedPath = $scope.selected.id;
                            },

                            dragStop: function (event) {
                                // noop
                            },

                            dropped: function (event) {
                                // noop
                            }
                        };

                    };
                }
            };
        }).directive("essentialsTemplateSettings", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    label: '@',
                    params: '=',
                    hasNoTemplates: '@',
                    hasSampleData: '@',
                    hasExtraTemplates: '@'
                },
                templateUrl: 'directives/essentials-template-settings.html',
                controller: function () {
                }
            };
        })
        .directive("essentialsHelp", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    helpText: '@',
                    helpReference: '=',
                    showHideVariable: '='
                },
                templateUrl: 'directives/essentials-help.html',
                controller: function ($scope) {
                    $scope.text = $scope.helpText ? $scope.helpText : $scope.helpReference;
                }
            };
        })
        .directive("essentialsMessages", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    pluginId: '=',
                    params: '='
                },
                templateUrl: 'directives/essentials-messages.html',
                controller: function ($scope, $rootScope, $http) {
                    // refresh messages when changes are made:
                    $scope.$watch('params', function () {
                        getMessages();
                    }, true);
                    getMessages();

                    var url = $rootScope.REST.PLUGINS.changesById($scope.pluginId);
                    function getMessages() {
                        if ($scope.params) {
                            var payload = {values: $scope.params};
                            $http.post(url, payload).success(function (data) {
                                $scope.packageMessages = data;
                            });
                        }
                    }
                }
            };
        }).directive("essentialsSimpleInstallPlugin", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    label: '@',
                    pluginId: '@',
                    hasNoTemplates: '@',
                    hasSampleData: '@',
                    hasExtraTemplates: '@'
                },
                templateUrl: 'directives/essentials-simple-install-plugin.html',
                controller: function ($scope, $log, $location, $rootScope, $http) {
                    // initialize fields to system defaults.
                    $http.get($rootScope.REST.PROJECT.settings).success(function (data) {
                        var params = {};

                        params.templateName   = data.templateLanguage;
                        params.sampleData     = data.useSamples;
                        params.extraTemplates = data.extraTemplates;

                        $scope.params = params;
                    });

                    $scope.apply = function () {
                        $http.post($rootScope.REST.PLUGINS.setupById($scope.pluginId), { values: $scope.params } )
                            .success(function () {
                                $rootScope.$broadcast('update-plugin-install-state', {
                                    'pluginId': $scope.pluginId,
                                    'state': 'installing'
                                });
                                $location.path('/installed-features');
                            });
                    };
                    $http.get($rootScope.REST.PLUGINS.byId($scope.pluginId)).success(function (plugin) {
                        $scope.plugin = plugin;
                    });
                }
            };
        }).directive("essentialsCmsDocumentTypeDeepLink", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    nameSpace: '@',
                    documentName: '@',
                    label: '@'
                },
                templateUrl: 'directives/essentials-cms-document-type-deep-link.html',
                controller: function ($scope, $rootScope) {
                    $scope.label = 'CMS Document Type Editor';
                    $scope.defaultNameSpace = $rootScope.projectSettings.projectNamespace;
                }
            };
        }).directive("essentialsNotifier", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    messages: '='
                },
                templateUrl: 'directives/essentials-notifier.html',
                controller: function ($scope, $filter, $log, $rootScope, $http, $timeout, $document) {
                    var promisesQueue = [];
                    var lastLength = 0;
                    var ERROR_SHOW_TIME = 1000;
                    $scope.visible = true;
                    $scope.messages = [];

                    $scope.activeMessages = [];
                    $scope.archiveMessages = [$scope.messages[0]];
                    $scope.archiveOpen = false;

                    $scope.$watch('messages', function () {
                        // don't execute if message count is not changed, e.g. when changing visibility only
                        if (lastLength == $scope.messages.length) {
                            return;
                        }
                        var date = new Date();
                        var now = date.toLocaleTimeString();
                        // cancel all hide promises
                        angular.forEach(promisesQueue, function (promise) {
                            $timeout.cancel(promise);
                        });
                        promisesQueue = [];
                        // keep messages which are not older than time showed + ERROR_SHOW_TIME:
                        /*  var elapsedTime = new Date();
                         elapsedTime.setSeconds(elapsedTime.getSeconds() + ERROR_SHOW_TIME);
                         var keepValuesCounter =0;
                         angular.forEach($scope.activeMessages, function (value) {
                         if(value.fullDate && value.fullDate.getDate() < elapsedTime){
                         keepValuesCounter++;
                         }
                         });*/
                        var currentLength = $scope.messages.length;
                        var startIdx = lastLength;
                        lastLength = currentLength;
                        $scope.activeMessages = [];
                        $scope.activeMessages = $scope.messages.slice(startIdx, currentLength);
                        $scope.archiveMessages = $scope.messages.slice(0, startIdx);
                        angular.forEach($scope.messages, function (value) {
                            value.visible = true;
                            if (!value.date) {
                                value.date = now;
                                value.fullDate = date;
                            }
                        });
                        if ($scope.archiveMessages.length === 0) {
                            $scope.archiveMessages.push({type: "info", message: 'No archived messages', visible: true, date: now, fullDate: date});
                        }
                        // newer messages first:
                        $scope.archiveMessages.reverse();
                        if ($scope.activeMessages.length > 1) {
                            // animate close:
                            var counter = 1;
                            var copy = $scope.activeMessages.slice(0);
                            angular.forEach(copy, function (value) {
                                if (counter > 1) {
                                    var promise = $timeout(function () {
                                        value.visible = false;
                                        $scope.archiveMessages.unshift(value);
                                    }, ERROR_SHOW_TIME * counter);
                                    promisesQueue.push(promise);
                                }
                                counter = counter + 0.5;
                            });
                        }
                    }, true);

                    $scope.toggleArchive = function ($event) {
                        $scope.archiveOpen = !$scope.archiveOpen;
                        $event.stopPropagation();
                    };
                    $scope.hide = function() {
                        $scope.visible = false;
                        $rootScope.$broadcast('hide-messages');
                    };
                    $rootScope.$on('show-messages', function() {
                        $scope.visible = true;
                    });
                    $scope.toggleErrors = function ($event) {
                        $event.stopPropagation();
                        $scope.showErrors = !$scope.showErrors;
                    };

                    $document.bind('click', function () {
                        $scope.archiveOpen = false;
                        $scope.$apply();
                    });
                }
            };
        }).directive("essentialsPlugin", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-plugin.html',
                controller: function ($scope, $filter, $log, $rootScope, $http) {
                    $scope.slides = [];
                    angular.forEach($scope.plugin.imageUrls, function(url) {
                        $scope.slides.push({
                            image: url
                        });
                    });
                    $scope.interval = 5000; // carousel change interval
                    $scope.showDescription = false;
                    $scope.toggleDescription = function ($event) {
                        $event.preventDefault();
                        $scope.showDescription = !$scope.showDescription;
                    };
                    $scope.isDiscovered = function() {
                        return $scope.plugin.type === 'feature' && $scope.plugin.installState === 'discovered';
                    };
                    $scope.isBoarding = function() {
                        return $scope.plugin.installState === 'boarding' || $scope.plugin.installState === 'installing';
                    };
                    $scope.isOnBoard = function() {
                        return $scope.plugin.type === 'tool' ||
                               $scope.plugin.installState === 'onBoard' ||
                               $scope.plugin.installState === 'installed';
                    };
                    $scope.installPlugin = function () {
                        $scope.installButtonDisabled = true; // avoid double-clicking
                        $rootScope.pluginsCache = null;
                        var pluginId = $scope.plugin.id;
                        $http.post($rootScope.REST.plugins + '/' + pluginId + '/install').success(function () {
                            // reload because of install state change:
                            $http.get($rootScope.REST.PLUGINS.byId(pluginId)).success(function (data) {
                                $scope.plugin = data;
                            });
                        });
                    };
                }
            };
        }).directive("essentialsInstalledFeature", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-feature.html',
                controller: function ($scope) {
                    $scope.needsRebuild = function() {
                        var state = $scope.plugin.installState;
                        return state === 'boarding' || state === 'installing';
                    };
                    $scope.needsConfiguration = function() {
                        return $scope.plugin.installState === 'onBoard';
                    };
                    $scope.hasConfiguration = function() {
                        return $scope.plugin.installState === 'installed' && $scope.plugin.hasConfiguration;
                    };
                }
            };
        }).directive("essentialsInstalledTool", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-installed-tool.html',
                controller: function ($scope, $location) {
                    $scope.useTool = function () {
                        $location.path('/tools/' + $scope.plugin.id);
                    };
                }
            };
        }).directive("essentialsFeatureFooter", function () {
            return {
                replace: true,
                restrict: 'E',
                scope: {
                    plugin: '='
                },
                templateUrl: 'directives/essentials-feature-footer.html',
                controller: function ($scope, $filter, $log, $rootScope, $http) {
                    $scope.toggleChanges = function($event) {
                        $event.preventDefault();
                        $scope.showChanges = !$scope.showChanges;

                        // Lazy-loading messages
                        if (!$scope.messagesLoaded) {
                            var payload = {
                                values: {
                                    sampleData: $rootScope.projectSettings.useSamples,
                                    templateName: $rootScope.projectSettings.templateLanguage
                                }
                            };
                            $http.post($rootScope.REST.PLUGINS.changesById($scope.plugin.id), payload).success(function (data){
                                // If there are no messages, the backend sends a single "no messages" message
                                // with the group not set. Filter those out.
                                if (data.items.length > 1 || data.items[0].group) {
                                    $scope.messages = data.items;
                                }
                                $scope.messagesLoaded = true;
                            });
                        }
                    };
                    $scope.isCreateFile          = function (message) { return message.group === 'FILE_CREATE'; };
                    $scope.isRegisterDocument    = function (message) { return message.group === 'DOCUMENT_REGISTER'; };
                    $scope.isCreateXmlNode       = function (message) { return message.group === 'XML_NODE_CREATE'; };
                    $scope.isCreateXmlFolderNode = function (message) { return message.group === 'XML_NODE_FOLDER_CREATE'; };
                    $scope.isExecute             = function (message) { return message.group === 'EXECUTE'; };

                    $scope.messagesLoaded = false; // Flag for lazy loading
                    $scope.showChanges = false;
                    $scope.hasMessages = !!$scope.plugin.packageFile;
                }
            };
        }).directive("essentialsDraftWarning", function () {
            return {
                replace: false,
                restrict: 'E',
                scope: {},
                templateUrl: 'directives/essentials-draft-warning.html',
                controller: function ($scope, $rootScope, $http) {
                    $scope.hasDraftDocuments = false;
                    $scope.isDraft = function (doc) {
                        return doc.draftMode;
                    };
                    $http.get($rootScope.REST.documents).success(function (data) {
                        $scope.documentTypes = data;
                        if (data) {
                            for (var i = 0; i < data.length; i++) {
                                var val = data[i];
                                if (val && val.draftMode) {
                                    $scope.hasDraftDocuments = true;
                                    break;
                                }
                            }
                        }
                    });

                }
            };
        });
})();
