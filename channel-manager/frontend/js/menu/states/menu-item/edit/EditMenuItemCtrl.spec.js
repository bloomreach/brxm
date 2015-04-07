/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('Edit Menu Item Controller', function () {
  'use strict';

  var $httpBackend,
    $scope,
    $state,
    EditMenuItemCtrl;

  beforeEach(function () {
    module('hippo.channel.menu', function ($provide) {
      $provide.value('hippo.channel.ConfigService', {
        apiUrlPrefix: 'api',
        menuId: 'menuId'
      });

      $provide.value('hippo.channel.HstApiRequests', jasmine.createSpy());

      $state = jasmine.createSpyObj('$state', ['go']);
      $provide.value('$state', $state);
    });

    inject(function ($injector) {
      var $controller = $injector.get('$controller');
      var $rootScope = $injector.get('$rootScope');
      $httpBackend = $injector.get('$httpBackend');
      $scope = $rootScope.$new();

      $httpBackend.when('GET', 'api/menuId').respond({
        data: {
          items: [
            {
              id: '1',
              title: 'One'
            },
            {
              id: '2',
              title: 'Two',
              items: [
                {
                  id: 'child1',
                  title: 'Child 1'
                }
              ]
            },
            {
              id: '3',
              title: 'One'
            }
          ]
        }
      });

      EditMenuItemCtrl = $controller('hippo.channel.menu.EditMenuItemCtrl as EditMenuItemCtrl', {
        '$scope': $scope
      });
    });
  });

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  function expectGetMenu () {
    $httpBackend.expectGET('api/menuId');
    $httpBackend.flush();
  }

  it('should select parent after delete if deleted item has no siblings left', function () {
    $scope.MenuItemCtrl = {
      selectedMenuItem: {
        id: 'child1'
      }
    };
    $httpBackend.expectPOST('api/menuId./delete/child1').respond('OK');

    EditMenuItemCtrl.remove.execute();

    $httpBackend.flush();

    expect($state.go).toHaveBeenCalledWith('menu-item.edit', {menuItemId: '2'});
  });

});
