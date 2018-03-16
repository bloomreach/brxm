/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('LeftSidePanel', () => {
  let $compile;
  let $rootScope;
  let CatalogService;
  let SidePanelService;
  let SiteMapService;

  const catalogComponents = [
    { label: 'dummy' },
  ];
  let parentScope;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$compile_, _$rootScope_, _CatalogService_, _SidePanelService_, _SiteMapService_) => {
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      CatalogService = _CatalogService_;
      SidePanelService = _SidePanelService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn(CatalogService, 'getComponents').and.returnValue([]);
    spyOn(CatalogService, 'load');
    spyOn(SidePanelService, 'close');
    spyOn(SidePanelService, 'initialize');
    spyOn(SiteMapService, 'get');
  });

  function instantiateController(componentsVisible) {
    parentScope = $rootScope.$new();
    parentScope.componentsVisible = componentsVisible;
    const el = angular.element('<left-side-panel components-visible="componentsVisible"></left-side-panel>');
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

  it('only shows the components tab when components are visible, and if there are catalog items', () => {
    const ChannelLeftSidePanelCtrl = instantiateController(false);
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(false);

    parentScope.componentsVisible = true;
    $rootScope.$digest();
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(false);

    CatalogService.getComponents.and.returnValue(catalogComponents);
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(true);

    parentScope.componentsVisible = false;
    $rootScope.$digest();
    expect(ChannelLeftSidePanelCtrl.showComponentsTab()).toBe(false);
  });

  it('retrieves the site map items from the channel siteMap service', () => {
    const siteMapItems = ['dummy'];
    const ChannelLeftSidePanelCtrl = instantiateController(false);
    SiteMapService.get.and.returnValue(siteMapItems);

    expect(ChannelLeftSidePanelCtrl.getSiteMapItems()).toBe(siteMapItems);
  });
});
