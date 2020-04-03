/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
  let $componentController;
  let $rootScope;
  let CatalogService;
  let SidePanelService;
  let SiteMapService;

  let $ctrl;
  let $element;
  let sideNavElement;

  const catalogComponents = [
    { label: 'dummy' },
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$rootScope_, _CatalogService_, _SidePanelService_, _SiteMapService_) => {
      $componentController = _$componentController_;
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

    $element = angular.element('<div></div>');
    sideNavElement = angular.element('<div class="left-side-panel"></div>');
    $element.append(sideNavElement);

    const $scope = $rootScope.$new();
    $ctrl = $componentController('leftSidePanel', {
      $element,
      $scope,
      SidePanelService,
    });
    $rootScope.$digest();
  });

  describe('$onInit', () => {
    it('restores the panel width from local storage when stored as a number', () => {
      spyOn($ctrl.localStorageService, 'get').and.returnValue('800');

      $ctrl.$onInit();

      expect($ctrl.width).toBe(800);
    });

    it('restores the panel width from local storage when stored as a dimension', () => {
      spyOn($ctrl.localStorageService, 'get').and.returnValue('600px');

      $ctrl.$onInit();

      expect($ctrl.width).toBe(600);
    });

    it('falls back to the minimum width if the panel width is unknown', () => {
      spyOn($ctrl.localStorageService, 'get').and.returnValue(null);

      $ctrl.$onInit();

      expect($ctrl.width).toBe(290);
    });
  });

  describe('$postLink', () => {
    it('initializes the channel left side panel service upon instantiation', () => {
      $ctrl.$onInit();
      $ctrl.$postLink();

      expect(SidePanelService.initialize).toHaveBeenCalledWith('left', $element, sideNavElement);
    });
  });

  describe('onResize', () => {
    it('updates the panel width', () => {
      $ctrl.width = 400;
      $ctrl.onResize(800);

      expect($ctrl.width).toBe(800);
    });

    it('respects the minimum width boundary', () => {
      $ctrl.width = 400;

      $ctrl.onResize(280);

      expect($ctrl.width).toBe(290);
    });

    it('stores the new panel width in local storage', () => {
      spyOn($ctrl.localStorageService, 'set');
      $ctrl.onResize(800);

      expect($ctrl.localStorageService.set).toHaveBeenCalledWith('channelManager.sidePanel.left.width', 800);
    });

    it('ignores new width if less than the minimum width while current width is equal to the minimum width', () => {
      $ctrl.width = 290;
      spyOn($ctrl.localStorageService, 'set');

      $ctrl.onResize(280);

      expect($ctrl.width).toBe(290);
      expect($ctrl.localStorageService.set).not.toHaveBeenCalled();
    });
  });

  it('knows when it is locked open', () => {
    spyOn(SidePanelService, 'isOpen').and.returnValue(true);

    expect($ctrl.isLockedOpen()).toBe(true);
  });

  it('knows when it is not locked open', () => {
    spyOn(SidePanelService, 'isOpen').and.returnValue(false);

    expect($ctrl.isLockedOpen()).toBe(false);
  });

  it('retrieves the catalog from the channel service', () => {
    CatalogService.getComponents.and.returnValue(catalogComponents);

    expect($ctrl.getCatalog()).toBe(catalogComponents);
  });

  it('only shows the components tab when components are visible, and if there are catalog items', () => {
    $ctrl.componentsVisible = false;
    expect($ctrl.showComponentsTab()).toBe(false);

    $ctrl.componentsVisible = true;
    expect($ctrl.showComponentsTab()).toBe(false);

    CatalogService.getComponents.and.returnValue(catalogComponents);
    expect($ctrl.showComponentsTab()).toBe(true);

    $ctrl.componentsVisible = false;
    expect($ctrl.showComponentsTab()).toBe(false);
  });

  it('retrieves the site map items from the channel siteMap service', () => {
    const siteMapItems = ['dummy'];
    SiteMapService.get.and.returnValue(siteMapItems);

    expect($ctrl.getSiteMapItems()).toBe(siteMapItems);
  });
});
