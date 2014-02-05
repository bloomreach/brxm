/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

describe('Menu Service', function () {
    'use strict';

    var menuService;

    beforeEach(module('hippo.channelManager.menuManager'));

    beforeEach(function() {
        var iFrameService = {
            enableLiveReload: function (){}
        };
        var configService = function (_) {
            return {
                menuId: 'menuId',
                apiUrlPrefix: 'url'
            };
        };
        module(function($provide) {
            $provide.value('_hippo.channelManager.menuManagement.IFrameService', iFrameService);
            $provide.value('hippo.channelManager.menuManager.ConfigService', configService);
        });
    });

    beforeEach(inject(['hippo.channelManager.menuManager.MenuService', function (MenuService) {
        menuService = MenuService;
    }]));


    it('should exist', function() {
        expect(menuService).toBeDefined();
    });

    it('should get menu by id', function() {
        // TODO: add assertions
        var menu = menuService.getMenu('menuId');
    });

    it('should delete menu by id', function() {
        // TODO: add assertions
        var menuId = menuService.deleteMenuItem('menuId');
    });
});
