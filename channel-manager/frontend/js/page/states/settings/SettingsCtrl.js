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
  'use strict';

  angular.module('hippo.channel.page')

    .controller('hippo.channel.page.SettingsCtrl', [
      '$scope',
      '$filter',
      'hippo.channel.FeedbackService',
      'hippo.channel.PageService',
      'hippo.channel.PrototypeService',
      'hippo.channel.ConfigService',
      'hippo.channel.Container',
      function ($scope, $filter, FeedbackService, PageService, PrototypeService, ConfigService, ContainerService) {
        var translate = $filter('translate');

        $scope.page = {
          id: null,
          title: '',
          primaryDocument: '',
          availableDocumentRepresentations: [],
          url: '',
          prototype: {
            id: null
          }
        };

        $scope.state = {
          isEditable: false,
          isLocked: false
        };

        $scope.lock = {
          owner: null,
          timestamp: null
        };

        $scope.template = {
          isVisible: false
        };

        $scope.validation = {
          illegalCharacters: '/ :'
        };

        $scope.tooltips = {
          url: function () {
            if ($scope.form.$dirty) {
              if ($scope.form.url.$error.required) {
                return translate('URL_REQUIRED');
              } else if ($scope.form.url.$error.illegalCharacters) {
                return translate('URL_ILLEGAL_CHARACTERS', $scope.validation);
              }
            }
            return '';
          },
          deleteButton: function () {
            if ($scope.page.isHomePage) {
              return translate('TOOLTIP_IS_HOMEPAGE');
            } else if (!$scope.state.isEditable) {
              return translate('TOOLTIP_NOT_EDITABLE');
            }
            return '';
          }
        };

        $scope.host = '';
        $scope.mountPath = '';
        $scope.isConfirmationVisible = false;
        $scope.isHomePage = false;

        // error feedback
        function setErrorFeedback (errorResponse) {
          $scope.errorFeedback = FeedbackService.getFeedback(errorResponse);
        }

        // fetch data
        loadHost()
          .then(loadPage)
          .then(loadPrototypes);

        $scope.showAssignNewTemplate = function () {
          $scope.template.isVisible = true;
        };

        $scope.submit = function () {
          var pageModel = {
            id: $scope.page.id,
            pageTitle: $scope.page.title,
            name: $scope.page.url,
            componentConfigurationId: $scope.page.prototype.id,
            primaryDocumentRepresentation: $scope.page.primaryDocumentRepresentation
          };

          PageService.updatePage(pageModel).then(function () {
            ContainerService.showPage($scope.mountPath + '/' + pageModel.name);
          }, setErrorFeedback);
        };

        $scope.closeContainer = function () {
          ContainerService.performClose();
        };

        $scope.deletePage = function () {
          $scope.isConfirmationVisible = true;
        };

        $scope.confirmDelete = function () {
          $scope.isConfirmationVisible = false;

          PageService.deletePage($scope.page.id).then(function () {
            ContainerService.showPage($scope.mountPath);
          }, setErrorFeedback);
        };

        $scope.cancelDelete = function () {
          $scope.isConfirmationVisible = false;
        };

        $scope.removePrimaryDocument = function () {
          $scope.isConfirmationRemovePrimaryDocumentVisible = true;
        };

        $scope.confirmRemovePrimaryDocument = function () {
          $scope.isConfirmationRemovePrimaryDocumentVisible = false;
          var pageModel = {
            id: $scope.page.id,
            pageTitle: $scope.page.title,
            name: $scope.page.url,
            componentConfigurationId: $scope.page.prototype.id,
            primaryDocumentRepresentation: {
              path: '',
              displayName: '',
              exists: false,
              document: false
            }
          };
          PageService.updatePage(pageModel).then(function () {
            ContainerService.showPage($scope.mountPath + '/' + $scope.page.url);
          }, setErrorFeedback);
        };

        $scope.cancelRemovePrimaryDocument = function () {
          $scope.isConfirmationRemovePrimaryDocumentVisible = false;
        };

        function loadHost () {
          return PageService.getMountInfo()
            .then(function (mountInfo) {
              $scope.host = mountInfo.hostName + mountInfo.mountPath;
              $scope.mountPath = mountInfo.mountPath;
              return mountInfo;
            }, setErrorFeedback);
        }

        function loadPrototypes () {
          return PrototypeService.getPrototypes()
            .then(function (data) {
              $scope.prototypes = data.prototypes;
              return data.prototypes;
            }, setErrorFeedback);
        }

        function loadPage () {
          function arrayObjectIndexOf (arr, obj) {
            for (var i = 0; i < arr.length; i++) {
              if (angular.equals(arr[i], obj)) {
                return i;
              }
            }
            return -1;
          }

          return PageService.getCurrentPage()
            .then(function (currentPage) {
              var defaultRepresentation = [
                {
                  displayName: $filter('translate')('NONE'),
                  path: ''

                }
              ];
              $scope.page.id = currentPage.id;
              $scope.page.title = currentPage.pageTitle;
              $scope.page.availableDocumentRepresentations = defaultRepresentation.concat(currentPage.availableDocumentRepresentations);

              if (currentPage.primaryDocumentRepresentation) {
                var indexOfPrimaryDoc = arrayObjectIndexOf($scope.page.availableDocumentRepresentations, currentPage.primaryDocumentRepresentation);
                $scope.page.primaryDocumentRepresentation = $scope.page.availableDocumentRepresentations[indexOfPrimaryDoc];
              } else {
                $scope.page.primaryDocumentRepresentation = $scope.page.availableDocumentRepresentations[0];
              }

              $scope.page.url = currentPage.name;
              $scope.page.hasContainerItem = currentPage.hasContainerItemInPageDefinition;
              $scope.page.isHomePage = currentPage.isHomePage;

              // pages are only editable when the sitemap item is:
              // 1. located in the HST workspace
              // 2. the page is not the homepage
              // 3. the page is not locked by someone else
              $scope.state.isLocked = angular.isString(currentPage.lockedBy) && currentPage.lockedBy !== ConfigService.cmsUser;
              $scope.state.isEditable = !$scope.page.isHomePage && !$scope.state.isLocked && currentPage.workspaceConfiguration;

              // lock information
              $scope.lock.owner = currentPage.lockedBy;
              $scope.lock.timestamp = currentPage.lockedOn;

              return currentPage;
            }, setErrorFeedback);
        }
      }
    ]);
}());
