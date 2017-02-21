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

describe('ChannelLeftSidePanel', () => {
  let $rootScope;
  let $compile;
  let SidePanelService;
  let SiteMapService;
  let ChannelService;
  let CatalogService;
  let HippoIframeService;
  let parentScope;
  const catalogComponents = [
    { label: 'dummy' },
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$compile_, _SidePanelService_, _ChannelService_, _CatalogService_, _SiteMapService_, _HippoIframeService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      SidePanelService = _SidePanelService_;
      ChannelService = _ChannelService_;
      CatalogService = _CatalogService_;
      SiteMapService = _SiteMapService_;
      HippoIframeService = _HippoIframeService_;
    });

    spyOn(CatalogService, 'getComponents').and.returnValue([]);
    spyOn(CatalogService, 'load');
    spyOn(ChannelService, 'getMountId');
    spyOn(SidePanelService, 'close');
    spyOn(SidePanelService, 'initialize');
    spyOn(SiteMapService, 'get');
    spyOn(HippoIframeService, 'load');
    spyOn(HippoIframeService, 'getCurrentRenderPathInfo');
  });

  function instantiateController(editMode) {
    parentScope = $rootScope.$new();
    parentScope.editMode = editMode;
    const el = angular.element('<left-side-panel edit-mode="editMode"></left-side-panel>');
    $compile(el)(parentScope);
    $rootScope.$digest();
    return el.controller('left-side-panel');
  }

  it('initializes the channel left side panel service upon instantiation', () => {
    instantiateController(false);

    expect(SidePanelService.initialize).toHaveBeenCalled();
  });

  it('knows when it is locked open', () => {
    const ctrl = instantiateController();
    spyOn(SidePanelService, 'isOpen').and.returnValue(true);
    expect(ctrl.isLockedOpen()).toBe(true);
  });

  it('knows when it is not locked open', () => {
    const ctrl = instantiateController();
    spyOn(SidePanelService, 'isOpen').and.returnValue(false);
    expect(ctrl.isLockedOpen()).toBe(false);
  });

  it('retrieves the catalog from the channel service', () => {
    CatalogService.getComponents.and.returnValue(catalogComponents);
    const ChannelLeftSidePanelCtrl = instantiateController();

    expect(ChannelLeftSidePanelCtrl.getCatalog()).toBe(catalogComponents);
  });

  it('only shows the components tab in edit mode, and if there are catalog items', () => {
    const ChannelLeftSidePanelCtrl = instantiateController(false);
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(false);

    parentScope.editMode = true;
    $rootScope.$digest();
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(false);

    CatalogService.getComponents.and.returnValue(catalogComponents);
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(true);

    parentScope.editMode = false;
    $rootScope.$digest();
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(false);
  });

  it('retrieves the sitemap items from the channel siteMap service', () => {
    const siteMapItems = ['dummy'];
    const ChannelLeftSidePanelCtrl = instantiateController(false);
    SiteMapService.get.and.returnValue(siteMapItems);

    expect(ChannelLeftSidePanelCtrl.getSiteMap()).toBe(siteMapItems);
  });

  it('asks the HippoIframeService to load the requested siteMap item', () => {
    const siteMapItem = {
      renderPathInfo: 'dummy',
    };
    const ChannelLeftSidePanelCtrl = instantiateController(false);

    ChannelLeftSidePanelCtrl.showPage(siteMapItem);

    expect(HippoIframeService.load).toHaveBeenCalledWith('dummy');
  });

  it('compares the siteMap item\'s renderPathInfo to the current one', () => {
    HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/current/path');
    const siteMapItem = {
      renderPathInfo: '/current/path',
    };
    const ChannelLeftSidePanelCtrl = instantiateController(false);
    expect(ChannelLeftSidePanelCtrl.isActiveSiteMapItem(siteMapItem)).toBe(true);

    siteMapItem.renderPathInfo = '/other/path';
    expect(ChannelLeftSidePanelCtrl.isActiveSiteMapItem(siteMapItem)).toBe(false);
  });
});

