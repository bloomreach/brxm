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

    var $httpBackend, $scope, $state, createController;

    beforeEach(module('hippo.channel.menu'));

    beforeEach(function() {
        module(function($provide) {
            $provide.value('hippo.channel.ConfigService', {
                apiUrlPrefix: 'api',
                menuId: 'menuId'
            });
            $provide.value('hippo.channel.HstApiRequests', jasmine.createSpy());
            $state = jasmine.createSpyObj('$state', ['go']);
            $provide.value('$state', $state);
        });

    });

    beforeEach(inject(function($injector) {
        $httpBackend = $injector.get('$httpBackend');
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
    }));

    beforeEach(inject(function ($injector) {
        var $controller = $injector.get('$controller');
        $scope = $injector.get('$rootScope').$new();
        createController = function() {
            return $controller('hippo.channel.menu.EditMenuItemCtrl', {
                '$scope': $scope
            });
        };
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    function expectGetMenu() {
        $httpBackend.expectGET('api/menuId');
        $httpBackend.flush();
    }

    it('should select parent after delete if deleted item has no siblings left', function () {

        $scope.selectedMenuItem = {
            id: 'child1'
        };

        var controller = createController();

        $scope.remove.execute();

        $httpBackend.expectPOST('api/menuId./delete/child1').respond('OK');
        $httpBackend.flush();

        expect($state.go).toHaveBeenCalledWith('menu-item.edit', { menuItemId: '2' });
    });


});
