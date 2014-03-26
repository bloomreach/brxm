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

describe('Settings Controller', function () {
    'use strict';

    var scope, PageService, PrototypeService,
        setCmsUser, createController,
        isPageInWorkspace,
        cmsUser, pageLockedBy, pageLockedOn;

    beforeEach(module('hippo.channel.page', function($provide) {
        setCmsUser = function(cmsUser) {
            $provide.value('hippo.channel.ConfigService', {
                cmsUser: cmsUser
            });
        };

        setCmsUser('admin');

        PageService = jasmine.createSpyObj('PageService', ['getHost', 'getCurrentPage']);
        $provide.value('hippo.channel.PageService', PageService);

        PrototypeService = jasmine.createSpyObj('PrototypeService', ['getPrototypes']);
        $provide.value('hippo.channel.PrototypeService', PrototypeService);
    }));

    beforeEach(inject(function ($rootScope, $controller, $q) {
        scope = $rootScope.$new();

        function resolvePromises() {
            $rootScope.$apply();
        }

        function resolvedPromise(value) {
            var deferred = $q.defer();
            deferred.resolve(value);
            return deferred.promise;
        }

        createController = function() {
            PageService.getHost.and.callFake(function() {
                return resolvedPromise('www.onehippo.com');
            });

            PageService.getCurrentPage.and.callFake(function() {
                return resolvedPromise({
                    id: 'pageId',
                    pageTitle: 'Page Title',
                    name: 'pageName',
                    workspaceConfiguration: isPageInWorkspace,
                    lockedBy: pageLockedBy,
                    lockedOn: pageLockedOn
                });
            });

            PrototypeService.getPrototypes.and.callFake(function() {
                return resolvedPromise([
                    {
                        displayName: 'Prototype One'
                    },
                    {
                        displayName: 'Prototype Two'
                    }
                ]);
            });

            var controller = $controller('hippo.channel.page.SettingsCtrl', { $scope: scope });

            resolvePromises();

            return controller;
        };
    }));

    it('should get the host', function () {
        createController();
        expect(PageService.getHost).toHaveBeenCalled();
        expect(scope.host).toEqual('www.onehippo.com');
    });

    it('should get the prototypes', function () {
        createController();
        expect(PrototypeService.getPrototypes).toHaveBeenCalled();
        expect(scope.prototypes.length).toEqual(2);
        expect(scope.prototypes[0].displayName).toEqual('Prototype One');
    });

    it('should get the current page', inject(function ($rootScope) {
        createController();
        expect(PageService.getCurrentPage).toHaveBeenCalled();
        expect(scope.page.id).toEqual('pageId');
        expect(scope.page.title).toEqual('Page Title');
        expect(scope.page.url).toEqual('pageName');
    }));

    it('should get the lock information', function() {
        pageLockedBy = 'editor';
        pageLockedOn = 12345;

        createController();

        expect(scope.lock.owner).toEqual('editor');
        expect(scope.lock.timestamp).toEqual(12345);
    });

    it('should lock the page when it is locked by another user', function() {
        pageLockedBy = 'editor';
        setCmsUser('admin');

        createController();

        expect(scope.state.isLocked).toEqual(true);
    });

    it('should not lock the page when it is not locked by anyone', function() {
        pageLockedBy = null;

        createController();

        expect(scope.state.isLocked).toEqual(false);
    });

    it('should not lock the page when it is locked by the current user', function() {
        pageLockedBy = 'editor';
        setCmsUser('editor');

        createController();

        expect(scope.state.isLocked).toEqual(false);
    });

    it('should make a page editable when it is located in the HST workspace and not locked by anyone', function () {
        isPageInWorkspace = true;
        pageLockedBy = null;

        createController();

        expect(scope.state.isEditable).toEqual(true);
    });

    it('should not make a page editable when it is located in the HST workspace but locked by another user', function () {
        isPageInWorkspace = true;

        pageLockedBy = 'editor';
        setCmsUser('admin');

        createController();

        expect(scope.state.isEditable).toEqual(false);
    });

    it('should make a page editable when it is located in the HST workspace and locked by the current CMS user', function () {
        isPageInWorkspace = true;

        pageLockedBy = 'editor';
        setCmsUser('editor');

        createController();

        expect(scope.state.isEditable).toEqual(true);
    });

    it('should not make a page editable when it not located in the HST workspace', function () {
        isPageInWorkspace = false;

        createController();

        expect(scope.state.isEditable).toEqual(false);
    });

});
