/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

describe('RightSidePanel', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let SidePanelService;
  let CmsService;
  let ChannelService;

  let $ctrl;
  let $scope;
  let sidePanelHandlers;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_, _$translate_, _ChannelService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
    });

    SidePanelService = jasmine.createSpyObj('SidePanelService', ['initialize', 'isOpen', 'close']);

    CmsService = jasmine.createSpyObj('CmsService', ['reportUsageStatistic']);

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('rightSidePanel', {
      $scope,
      $element,
      SidePanelService,
      CmsService,
    });
    $rootScope.$apply();

    sidePanelHandlers = {
      onOpen: SidePanelService.initialize.calls.mostRecent().args[2],
      onClose: SidePanelService.initialize.calls.mostRecent().args[3],
    };
  });

  it('should set full width mode on and off', () => {
    $ctrl.setFullWidth(true);
    expect($ctrl.$element.hasClass('fullwidth')).toBe(true);
    expect($ctrl.isFullWidth).toBe(true);
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsFullScreen');

    CmsService.reportUsageStatistic.calls.reset();

    $ctrl.setFullWidth(false);
    expect($ctrl.$element.hasClass('fullwidth')).toBe(false);
    expect($ctrl.isFullWidth).toBe(false);
    expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
  });

  it('should update local storage on resize', () => {
    $ctrl.onResize(800);

    expect($ctrl.lastSavedWidth).toBe('800px');
    expect($ctrl.localStorageService.get('rightSidePanelWidth')).toBe('800px');
  });

  it('should load last saved width of right side panel', () => {
    spyOn($ctrl.localStorageService, 'get').and.callFake(() => '800px');

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('rightSidePanelWidth');
    expect($ctrl.lastSavedWidth).toBe('800px');

    $ctrl.localStorageService.get.and.callFake(() => null);

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('rightSidePanelWidth');
    expect($ctrl.lastSavedWidth).toBe('440px');
  });

  describe('when upon initialization or opening the panel', () => {
    let testId;

    beforeEach(() => {
      spyOn($ctrl, '_onOpen');
      testId = 'documentId';
    });

    it('initializes the channel right side panel service', () => {
      expect(SidePanelService.initialize).toHaveBeenCalled();
      expect($ctrl.documentId).not.toBeDefined();
      expect($ctrl.editing).not.toBeDefined();
      expect($ctrl.createContent).not.toBeDefined();
    });

    it('calls 2 initialization functions on oepn', () => {
      spyOn($ctrl, 'openEditor');
      sidePanelHandlers.onOpen(testId);
      $rootScope.$digest();

      expect($ctrl.openEditor).toHaveBeenCalledWith(testId);
      expect($ctrl._onOpen).toHaveBeenCalled();
    });

    it('initialises editContent if documentId is passed', () => {
      $ctrl.openEditor(testId);

      expect($ctrl.documentId).toBe(testId);
      expect($ctrl.editing).toBeTruthy();
    });

    it('initialises new content if no document id is passed', () => {
      $ctrl.openEditor(null);

      expect($ctrl.documentId).toBeFalsy();
      expect($ctrl.createContent).toBeTruthy();
    });
  });

  it('knows when it is locked open', () => {
    SidePanelService.isOpen.and.returnValue(true);
    expect($ctrl.isLockedOpen()).toBe(true);
  });

  it('knows when it is not locked open', () => {
    SidePanelService.isOpen.and.returnValue(false);
    expect($ctrl.isLockedOpen()).toBe(false);
  });

  it('closes the panel', () => {
    SidePanelService.close.and.returnValue($q.resolve());
    $ctrl._closePanel();
    $rootScope.$digest();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');

    $ctrl.documentId = 'test';
    $ctrl.editing = true;
    $ctrl._closePanel();
    $rootScope.$digest();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($ctrl.documentId).toBeUndefined();
    expect($ctrl.editing).toBeUndefined();
    expect($ctrl.createContent).toBeUndefined();
  });

  it('should close right side panel', () => {
    spyOn(ChannelService, 'setToolbarDisplayed');
    spyOn($ctrl, 'setFullWidth');
    SidePanelService.close.and.returnValue($q.resolve());

    ChannelService.isToolbarDisplayed = false;
    $ctrl._closePanel();
    $scope.$apply();

    expect(ChannelService.setToolbarDisplayed).toHaveBeenCalledWith(true);
    expect($ctrl.setFullWidth).toHaveBeenCalledWith(false);
  });
});

