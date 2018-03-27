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
    spyOn(SiteMapService, 'get').and.returnValue([]);
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

  describe('property isOpen', () => {
    it('defaults to false', () => {
      const ctrl = instantiateController();
      expect(ctrl.isOpen).toBe(false);
    });

    it('stores the open state as a boolean', () => {
      const ctrl = instantiateController();
      ctrl.isOpen = true;
      expect(ctrl.localStorageService.get('leftSidePanelOpen')).toBe(true);
      expect(ctrl.isOpen).toBe(true);

      ctrl.isOpen = false;
      expect(ctrl.localStorageService.get('leftSidePanelOpen')).toBe(false);
      expect(ctrl.isOpen).toBe(false);
    });

    it('falls back to false if the stored value is not a boolean', () => {
      const ctrl = instantiateController();
      spyOn(ctrl.localStorageService, 'get').and.returnValue(null);

      expect(ctrl.isOpen).toBe(false);
    });

    it('ignores input that is not a boolean', () => {
      const ctrl = instantiateController();
      spyOn(ctrl.localStorageService, 'set').and.callThrough();

      ctrl.isOpen = null;
      ctrl.isOpen = 1;
      ctrl.isOpen = 'a string';
      expect(ctrl.localStorageService.set).not.toHaveBeenCalled();
    });
  });

  describe('property selectedTab', () => {
    it('defaults to zero', () => {
      const ctrl = instantiateController();
      expect(ctrl.selectedTab).toBe(0);
    });

    it('stores the selected tab as a number', () => {
      const ctrl = instantiateController();

      ctrl.selectedTab = 0;
      expect(ctrl.localStorageService.get('leftSidePanelSelectedTab')).toBe('0');
      expect(ctrl.selectedTab).toBe(0);

      ctrl.selectedTab = 1;
      expect(ctrl.localStorageService.get('leftSidePanelSelectedTab')).toBe('1');
      expect(ctrl.selectedTab).toBe(1);
    });

    it('falls back to zero if the stored value is NaN', () => {
      const ctrl = instantiateController();
      spyOn(ctrl.localStorageService, 'get').and.returnValue('a string');

      expect(ctrl.selectedTab).toBe(0);
    });

    it('ignores input that is not a number', () => {
      const ctrl = instantiateController();
      ctrl.selectedTab = 1;
      spyOn(ctrl.localStorageService, 'set').and.callThrough();

      ctrl.selectedTab = null;
      ctrl.selectedTab = false;
      ctrl.selectedTab = 'a string';
      expect(ctrl.localStorageService.set).not.toHaveBeenCalled();
      expect(ctrl.selectedTab).toBe(1);
    });

    it('ignores numbers lower than zero', () => {
      const ctrl = instantiateController();
      ctrl.selectedTab = 1;
      spyOn(ctrl.localStorageService, 'set').and.callThrough();

      ctrl.selectedTab = -1;
      expect(ctrl.localStorageService.set).not.toHaveBeenCalled();
      expect(ctrl.selectedTab).toBe(1);
    });
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
