/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('ChannelSidenav', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let ChannelSidenavService;
  let SiteMapService;
  let ChannelService;
  let HippoIframeService;
  let parentScope;
  const catalogComponents = [
    { label: 'dummy' },
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$compile_, _ChannelSidenavService_, _ChannelService_, _SiteMapService_, _HippoIframeService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      ChannelSidenavService = _ChannelSidenavService_;
      ChannelService = _ChannelService_;
      SiteMapService = _SiteMapService_;
      HippoIframeService = _HippoIframeService_;
    });

    spyOn(ChannelService, 'getCatalog').and.returnValue([]);
    spyOn(ChannelSidenavService, 'initialize');
    spyOn(ChannelSidenavService, 'close');
    spyOn(SiteMapService, 'get');
    spyOn(HippoIframeService, 'load');
    spyOn(HippoIframeService, 'getCurrentRenderPathInfo');
  });

  function instantiateController(editMode) {
    parentScope = $rootScope.$new();
    parentScope.editMode = editMode;
    const el = angular.element('<channel-sidenav edit-mode="editMode"></channel-sidenav>');
    $compile(el)(parentScope);
    $rootScope.$digest();
    return el.controller('channel-sidenav');
  }

  it('initializes the channel sidenav service upon instantiation', () => {
    instantiateController(false);

    expect(ChannelSidenavService.initialize).toHaveBeenCalled();
    expect(ChannelSidenavService.close).toHaveBeenCalled();
  });

  it('retrieves the catalog from the channel service', () => {
    ChannelService.getCatalog.and.returnValue(catalogComponents);
    const ChannelSidenavCtrl = instantiateController();

    expect(ChannelSidenavCtrl.getCatalog()).toBe(catalogComponents);
  });

  it('only shows the components tab in edit mode, and if there are catalog items', () => {
    const ChannelSidenavCtrl = instantiateController(false);
    expect(ChannelSidenavCtrl.showComponentsTab()).toBe(false);

    parentScope.editMode = true;
    $rootScope.$digest();
    expect(ChannelSidenavCtrl.showComponentsTab()).toBe(false);

    ChannelService.getCatalog.and.returnValue(catalogComponents);
    expect(ChannelSidenavCtrl.showComponentsTab()).toBe(true);

    parentScope.editMode = false;
    $rootScope.$digest();
    expect(ChannelSidenavCtrl.showComponentsTab()).toBe(false);
  });

  it('retrieves the sitemap items from the channel siteMap service', () => {
    const siteMapItems = ['dummy'];
    const ChannelSidenavCtrl = instantiateController(false);
    SiteMapService.get.and.returnValue(siteMapItems);

    expect(ChannelSidenavCtrl.getSiteMap()).toBe(siteMapItems);
  });

  it('asks the HippoIframeService to load the requested siteMap item', () => {
    const siteMapItem = {
      renderPathInfo: 'dummy',
    };
    const ChannelSidenavCtrl = instantiateController(false);

    ChannelSidenavCtrl.showPage(siteMapItem);

    expect(HippoIframeService.load).toHaveBeenCalledWith('dummy');
  });

  it('compares the siteMap item\'s renderPathInfo to the current one', () => {
    HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/current/path');
    const siteMapItem = {
      renderPathInfo: '/current/path',
    };
    const ChannelSidenavCtrl = instantiateController(false);
    expect(ChannelSidenavCtrl.isActiveSiteMapItem(siteMapItem)).toBe(true);

    siteMapItem.renderPathInfo = '/other/path';
    expect(ChannelSidenavCtrl.isActiveSiteMapItem(siteMapItem)).toBe(false);
  });
});

