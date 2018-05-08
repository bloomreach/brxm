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

describe('RightSidePanel', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $state;
  let $stateRegistry;
  let $timeout;
  let ChannelService;
  let CmsService;
  let RightSidePanelService;
  let SidePanelService;

  let $ctrl;
  let $scope;
  let $element;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _$state_, _$stateRegistry_, _$timeout_, _ChannelService_, _RightSidePanelService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $stateRegistry = _$stateRegistry_;
      $timeout = _$timeout_;
      ChannelService = _ChannelService_;
      RightSidePanelService = _RightSidePanelService_;
    });

    CmsService = jasmine.createSpyObj('CmsService', ['reportUsageStatistic']);
    SidePanelService = jasmine.createSpyObj('SidePanelService', ['initialize', 'isOpen', 'close', 'open']);

    $scope = $rootScope.$new();
    $element = angular.element('<div></div>');
    $ctrl = $componentController('rightSidePanel', {
      $element,
      $scope,
      CmsService,
      SidePanelService,
    });
    $rootScope.$digest();
  });

  it('knows the loading state', () => {
    RightSidePanelService.startLoading();
    $timeout.flush();
    expect($ctrl.isLoading()).toBe(true);

    RightSidePanelService.stopLoading();
    expect($ctrl.isLoading()).toBe(false);
  });

  it('knows the title', () => {
    RightSidePanelService.setTitle('test title');
    expect($ctrl.getTitle()).toEqual('test title');

    RightSidePanelService.clearTitle();
    expect($ctrl.getTitle()).toEqual('');
  });

  it('sets full width mode on and off', () => {
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

  it('updates local storage on resize', () => {
    $ctrl.onResize(800);

    expect($ctrl.lastSavedWidth).toBe('800px');
    expect($ctrl.localStorageService.get('rightSidePanelWidth')).toBe('800px');
  });

  it('detects ESC keypress', () => {
    const e = angular.element.Event('keydown');
    e.which = 27;

    spyOn($state, 'go');
    $ctrl.$element.trigger(e);
    expect($state.go).toHaveBeenCalledWith('hippo-cm.channel');
  });

  it('ignores other keypresses', () => {
    const e = angular.element.Event('keydown');
    e.which = 28;

    spyOn($state, 'go');
    $ctrl.$element.trigger(e);
    expect($state.go).not.toHaveBeenCalled();
  });

  it('loads last saved width of right side panel', () => {
    spyOn($ctrl.localStorageService, 'get').and.callFake(() => '800px');

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('rightSidePanelWidth');
    expect($ctrl.lastSavedWidth).toBe('800px');

    $ctrl.localStorageService.get.and.callFake(() => null);

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('rightSidePanelWidth');
    expect($ctrl.lastSavedWidth).toBe('440px');
  });

  it('knows when it is locked open', () => {
    SidePanelService.isOpen.and.returnValue(true);
    expect($ctrl.isLockedOpen()).toBe(true);
  });

  it('knows when it is not locked open', () => {
    SidePanelService.isOpen.and.returnValue(false);
    expect($ctrl.isLockedOpen()).toBe(false);
  });

  it('opens the panel when transitioning to state "hippo-cm.channel.*"', () => {
    spyOn($ctrl.localStorageService, 'get').and.returnValue('800px');
    $ctrl.$onInit();
    SidePanelService.open.and.returnValue($q.resolve());

    $state.go('hippo-cm.channel.edit-content', { channelId: 'channelId', documentId: 'docId' });
    $rootScope.$digest();

    expect($element.hasClass('sidepanel-open')).toBe(true);
    expect($element.css('width')).toBe('800px');
    expect($element.css('max-width')).toBe('800px');
  });

  it('opens the panel when transitioning to state "hippo-cm.channel.**"', () => {
    $stateRegistry.register({ name: 'hippo-cm.channel.page-info.test' });

    spyOn($ctrl.localStorageService, 'get').and.returnValue('800px');
    $ctrl.$onInit();
    SidePanelService.open.and.returnValue($q.resolve());

    $state.go('hippo-cm.channel.page-info.test');
    $rootScope.$digest();

    expect($element.hasClass('sidepanel-open')).toBe(true);
    expect($element.css('width')).toBe('800px');
    expect($element.css('max-width')).toBe('800px');
  });

  it('closes the panel when transitioning back to state "hippo-cm.channel"', () => {
    spyOn(ChannelService, 'setToolbarDisplayed');
    SidePanelService.open.and.returnValue($q.resolve());
    SidePanelService.close.and.returnValue($q.resolve());
    ChannelService.isToolbarDisplayed = false;

    $ctrl.$onInit();

    $state.go('hippo-cm.channel.edit-content', { channelId: 'channelId', documentId: 'docId' });
    $rootScope.$digest();

    $state.go('hippo-cm.channel');
    $rootScope.$digest();

    expect($element.hasClass('sidepanel-open')).toBe(false);
    expect($element.css('max-width')).toBe('0px');
    expect(ChannelService.setToolbarDisplayed).toHaveBeenCalledWith(true);
    expect($element.hasClass('fullwidth')).toBe(false);
  });
});
