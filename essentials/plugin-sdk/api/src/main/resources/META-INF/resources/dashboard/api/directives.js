/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
    .directive("essentialsSimpleInstallPlugin", function () {
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
        templateUrl: 'dashboard/api/templates/essentials-simple-install-plugin.html',
        controller: function ($scope, $location, pluginService, projectService) {
          // initialize fields to system defaults.
          $scope.params = {};
          projectService.getSettings().then(function (settings) {
            $scope.params.templateName = settings.templateLanguage;
            $scope.params.sampleData = settings.useSamples;
            $scope.params.extraTemplates = settings.extraTemplates;
          });
          $scope.apply = function () {
            pluginService.install($scope.pluginId, $scope.params).then(function () {
              $location.path('/installed-features');
            });
          };
          $scope.plugin = pluginService.getPlugin($scope.pluginId);

          $scope.$watch('params', function () {
            getMessages();
          }, true);
          getMessages();

          function getMessages() {
            if ($scope.params) {
              pluginService.getChangeMessages($scope.pluginId, $scope.params).then(function(changeMessages) {
                $scope.changeMessages = changeMessages;
              });
            }
          }
        }
      };
    }).directive("essentialsFolderPicker", function () {
      return {
        replace: false,
        restrict: 'E',
        scope: {
          title: '@',
          buttonText: '@',
          selectedPath: '=',
          selected: '='
        },
        templateUrl: 'dashboard/api/templates/essentials-folder-picker.html',
        controller: function ($scope, $uibModal, $log, $http) {
          $scope.open = function (size) {
            var modalInstance = $uibModal.open({
              templateUrl: 'tree-picker.html',
              controller: ModalInstanceCtrl,
              size: size,
              resolve: {
                title: function () {
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

          var ModalInstanceCtrl = function ($scope, $uibModalInstance, title) {
            $scope.title = title;
            $http.get(window.SERVER_URL + '/essentials/rest/jcrbrowser/folders').success(function (data) {
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
    }).directive("essentialsCmsDocumentTypeDeepLink", function () {
      return {
        replace: true,
        restrict: 'E',
        scope: {
          nameSpace: '@',
          documentName: '@',
          label: '@'
        },
        templateUrl: 'dashboard/api/templates/essentials-cms-document-type-deep-link.html',
        controller: function ($scope, projectService) {
          $scope.label = 'CMS Document Type Editor';
          projectService.getSettings().then(function(settings) {
            $scope.defaultNameSpace = settings.projectNamespace;
          });
        }
      };
    }).directive("essentialsDraftWarning", function () {
      return {
        replace: false,
        restrict: 'E',
        scope: {},
        templateUrl: 'dashboard/api/templates/essentials-draft-warning.html',
        controller: function ($scope, essentialsContentTypeService) {
          $scope.isDraft = function (contentType) {
            return contentType.draftMode;
          };
          essentialsContentTypeService.getContentTypes().success(function(contentTypes) {
            $scope.documentTypes = contentTypes;
            if (contentTypes) {
              $scope.hasDraftDocuments = contentTypes.some(function(contentType) {
                return contentType.draftMode;
              });
            }
          });
        }
      };
    });
})();