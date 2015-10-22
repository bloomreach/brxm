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

  function getLink (menuItem) {
    var link;

    if (menuItem.pathInfo) {
      link = menuItem.mountPath + '/' + menuItem.pathInfo;
    } else {
      link = menuItem.link;
    }

    return link;
  }

  angular.module('hippo.channel.menu')

    .controller('hippo.channel.menu.EditMenuItemCtrl', [
      '$rootScope',
      '$scope',
      '$state',
      '$stateParams',
      '$window',
      'hippo.channel.FeedbackService',
      'hippo.channel.menu.MenuService',
      'hippo.channel.FormStateService',
      'hippo.channel.Container',
      function ($rootScope, $scope, $state, $stateParams, $window, FeedbackService, MenuService, FormStateService, ContainerService) {
        var EditMenuItemCtrl = this,
          savedMenuItem;

        if ($scope.MenuItemCtrl && (!$scope.MenuItemCtrl.selectedMenuItem || $stateParams.menuItemId !== $scope.MenuItemCtrl.selectedMenuItem.id)) {
          MenuService.getMenuItem($stateParams.menuItemId).then(function (item) {
            $scope.MenuItemCtrl.selectedMenuItem = item;
            $scope.MenuItemCtrl.showParameters = Object.keys(item.localParameters).length > 0;

            EditMenuItemCtrl.isSaving = (function(){
              var result = {
                title: false,
                linkType: false,
                link: false
              };

              result.localParameters= (function() {
                var parameters={};
                var localParameters = item.localParameters;
                if (localParameters!==undefined){
                  for (var key in localParameters ){
                    parameters[key]=false;
                  }
                }

                return parameters;
              })();
              return result;
            }());

            EditMenuItemCtrl.isSaved = (function(){
              var result = {
                title: true,
                linkType: true,
                link: true
              };
              result.localParameters= (function() {
                var parameters={};
                var localParameters = item.localParameters;
                if (localParameters!==undefined){
                  for (var key in localParameters ){
                    parameters[key]=true;
                  }
                }
                return parameters;
              })();
              return result;
            }());

          });
        }


        EditMenuItemCtrl.remove = {
          isVisible: false,
          show: function () {
            EditMenuItemCtrl.remove.isVisible = true;
          },
          execute: function () {
            EditMenuItemCtrl.remove.isVisible = false;
            remove();
          },
          cancel: function () {
            EditMenuItemCtrl.remove.isVisible = false;
          }
        };

        EditMenuItemCtrl.internalLink = {
          openPicker: function () {
            var menuData = MenuService.getMenuData();
            $state.go('picker', {
              menuItemId: $scope.MenuItemCtrl.selectedMenuItem.id,
              siteContentIdentifier: menuData.siteContentIdentifier,
              siteMapIdentifier: menuData.siteMapIdentifier,
              link: $scope.MenuItemCtrl.selectedMenuItem.link
            });
          },
          showPage: function () {
            var link = getLink($scope.MenuItemCtrl.selectedMenuItem);
            ContainerService.showPage(link);
          }
        };

        EditMenuItemCtrl.externalLink = {
          isVisible: false,
          show: function () {
            EditMenuItemCtrl.externalLink.isVisible = true;
          },
          execute: function () {
            EditMenuItemCtrl.externalLink.isVisible = false;
            $window.open($scope.MenuItemCtrl.selectedMenuItem.link);
          },
          cancel: function () {
            EditMenuItemCtrl.externalLink.isVisible = false;
          }
        };

        EditMenuItemCtrl.fieldFeedbackMessage = {};

        EditMenuItemCtrl.saveTitle = function (form) {
          if (form.title.$dirty && form.title.$valid) {
            EditMenuItemCtrl.saveSelectedMenuItem('title');
          }
        };

        EditMenuItemCtrl.saveLocalParameters = function(form,key){
          if (form.value.$dirty && form.value.$valid){
            EditMenuItemCtrl.saveSelectedMenuItem('localParameters',key);
          }
        };

        EditMenuItemCtrl.updateLinkDestination = function (form) {
          var formItem;
          if ($scope.MenuItemCtrl.selectedMenuItem.linkType === 'NONE') {
            EditMenuItemCtrl.saveSelectedMenuItem('linkType');
          } else {
            if ($scope.MenuItemCtrl.selectedMenuItem.linkType === 'SITEMAPITEM') {
              formItem = form.sitemapItem;
            } else if ($scope.MenuItemCtrl.selectedMenuItem.linkType === 'EXTERNAL') {
              formItem = form.url;
              formItem.$pristine = false;
              formItem.$dirty = true;
            }
            if (formItem.$dirty && formItem.$valid) {
              EditMenuItemCtrl.saveSelectedMenuItem('link');
            }
          }
        };

        EditMenuItemCtrl.saveSelectedMenuItem = function (propertyName, key) {
          if (shouldSaveSelectedMenuItemProperty(propertyName, key)) {
            saveSelectedMenuItemProperty(propertyName, key);
          }
        };

        EditMenuItemCtrl.dismissFeedback = function () {
          if ($scope.MenuItemCtrl.feedback.message) {
            $scope.MenuItemCtrl.feedback.message = '';
          }
          EditMenuItemCtrl.fieldFeedbackMessage = {};
        };

        if ($scope.MenuItemCtrl) {
          savedMenuItem = angular.copy($scope.MenuItemCtrl.selectedMenuItem);
        }

        function shouldSaveSelectedMenuItemProperty () {
          EditMenuItemCtrl.dismissFeedback();
          return angular.isDefined($scope.MenuItemCtrl.selectedMenuItem);
        }

        function saveSelectedMenuItemProperty (propertyName,key) {
          savedMenuItem = angular.copy($scope.MenuItemCtrl.selectedMenuItem);

          // child properties haven't changed, so don't send them
          delete savedMenuItem.items;


          if (key!==undefined){
            EditMenuItemCtrl.isSaving[propertyName][key] = true;
          }
          else{
            EditMenuItemCtrl.isSaving[propertyName] = true;
          }


          MenuService.saveMenuItem(savedMenuItem)
            .then(function () {
                if (key!==undefined){
                  EditMenuItemCtrl.isSaving[propertyName][key] = false;
                  EditMenuItemCtrl.isSaved[propertyName][key] = true;
                }
                else{
                  EditMenuItemCtrl.isSaving[propertyName] = false;
                  EditMenuItemCtrl.isSaved[propertyName] = true;
                }

              FormStateService.setValid(true);
            },
            function (errorResponse) {
              if (key!==undefined){
                EditMenuItemCtrl.fieldFeedbackMessage[propertyName][key] = FeedbackService.getFeedback(errorResponse).message;
                EditMenuItemCtrl.isSaving[propertyName][key] = false;
                EditMenuItemCtrl.isSaved[propertyName][key] = false;
              }
              else{
                EditMenuItemCtrl.fieldFeedbackMessage[propertyName] = FeedbackService.getFeedback(errorResponse).message;
                EditMenuItemCtrl.isSaving[propertyName] = false;
                EditMenuItemCtrl.isSaved[propertyName] = false;
              }

              FormStateService.setValid(false);
              $scope.MenuItemCtrl.feedback = FeedbackService.getFeedback(errorResponse);
            });
        }

        function remove () {
          MenuService.getPathToMenuItem($scope.MenuItemCtrl.selectedMenuItem.id).then(function (path) {
            var nextState = (function () {
              var item, parent, items;
              if (!path || path.length < 2) {
                return {state: 'none'};
              }

              item = path.pop();
              parent = path.pop();
              items = parent.items;
              if (items.length == 1) {
                // item to delete has no siblings, so parent will be selected
                if (path.length > 0) {
                  return {state: 'edit', id: parent.id};
                } else {
                  return {state: 'none'};
                }
              }
              var itemIndex = items.indexOf(item);
              if (itemIndex === 0) {
                // Item to delete is first child, so select next child
                return {state: 'edit', id: items[itemIndex + 1].id};
              } else {
                // Item to delete is not first child, so select previous child
                return {state: 'edit', id: items[itemIndex - 1].id};
              }
            }());

            // HTTP-request to delete the menu item
            MenuService.deleteMenuItem($scope.MenuItemCtrl.selectedMenuItem.id).then(function () {
              $state.go('menu-item.' + nextState.state, {
                menuItemId: nextState.id
              });
            }, function (errorResponse) {
              $scope.MenuItemCtrl.feedback = FeedbackService.getFeedback(errorResponse);
            });
          });
        }
      }
    ]);
}());
