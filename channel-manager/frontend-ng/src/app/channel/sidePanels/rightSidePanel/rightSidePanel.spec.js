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
  let $timeout;
  let $transitions;
  let RightSidePanelService;
  let SidePanelService;

  let $ctrl;
  let $scope;
  let $element;
  let sideNavElement;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _$state_, _$timeout_, _$transitions_, _ChannelService_, _RightSidePanelService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $timeout = _$timeout_;
      $transitions = _$transitions_;
      RightSidePanelService = _RightSidePanelService_;
    });

    SidePanelService = jasmine.createSpyObj('SidePanelService', ['initialize', 'isOpen', 'close', 'open', 'setFullScreen', 'isFullScreen']);

    $element = angular.element('<div></div>');
    sideNavElement = angular.element('<div class="right-side-panel"></div>');
    $element.append(sideNavElement);

    $scope = $rootScope.$new();
    $ctrl = $componentController('rightSidePanel', {
      $element,
      $scope,
      SidePanelService,
    });
    $rootScope.$digest();
  });

  it('initializes the right side panel with the side panel service upon $postLink', () => {
    $ctrl.$onInit();
    $ctrl.$postLink();

    expect(SidePanelService.initialize).toHaveBeenCalledWith('right', $element, sideNavElement);
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
    expect($ctrl.getTooltip()).toEqual('test title');

    RightSidePanelService.setTitle('title', 'tooltip');
    expect($ctrl.getTitle()).toEqual('title');
    expect($ctrl.getTooltip()).toEqual('tooltip');

    RightSidePanelService.clearTitle();
    expect($ctrl.getTitle()).toEqual('');
    expect($ctrl.getTooltip()).toEqual('');
  });

  it('knows the context', () => {
    RightSidePanelService.setContext('test context');
    expect($ctrl.getContext()).toEqual('test context');

    RightSidePanelService.clearContext();
    expect($ctrl.getContext()).toEqual('');
  });

  it('sets full screen mode on and off', () => {
    $ctrl.setFullScreen(true);
    expect(SidePanelService.setFullScreen).toHaveBeenCalledWith('right', true);

    $ctrl.setFullScreen(false);
    expect(SidePanelService.setFullScreen).toHaveBeenCalledWith('right', false);
  });

  it('updates local storage on resize', () => {
    $ctrl.onResize(800);

    expect($ctrl.lastSavedWidth).toBe('800px');
    expect($ctrl.localStorageService.get('channelManager.sidePanel.right.width')).toBe('800px');
  });

  it('detects ESC keypress', () => {
    const e = angular.element.Event('keydown');
    e.which = 27;

    spyOn($state, 'go');
    $ctrl.$element.trigger(e);
    expect($state.go).toHaveBeenCalledWith('^');
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

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('channelManager.sidePanel.right.width');
    expect($ctrl.lastSavedWidth).toBe('800px');

    $ctrl.localStorageService.get.and.callFake(() => null);

    $ctrl.$onInit();

    expect($ctrl.localStorageService.get).toHaveBeenCalledWith('channelManager.sidePanel.right.width');
    expect($ctrl.lastSavedWidth).toBe('440px');
  });

  it('sets the last saved width on the sideNavElement', () => {
    spyOn($ctrl.localStorageService, 'get').and.returnValue('800px');

    $ctrl.$onInit();

    expect(sideNavElement.css('width')).toBe('800px');
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
    $ctrl.$onInit();

    $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
    $rootScope.$digest();

    expect(SidePanelService.open).toHaveBeenCalledWith('right');
  });

  it('closes the panel when transitioning back to state "hippo-cm.channel"', () => {
    SidePanelService.open.and.returnValue($q.resolve());
    SidePanelService.close.and.returnValue($q.resolve());

    $ctrl.$onInit();

    $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
    $rootScope.$digest();

    $state.go('hippo-cm.channel');
    $rootScope.$digest();

    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($element.hasClass('side-panel-open')).toBe(false);
    expect($element.hasClass('full-screen')).toBe(false);
    expect(SidePanelService.setFullScreen).toHaveBeenCalledWith('right', false);
  });

  describe('focus handling', () => {
    let mdSidenav;

    beforeEach(() => {
      $ctrl.$onInit();

      mdSidenav = jasmine.createSpyObj('mdSidenav', ['focus']);
      spyOn($element, 'find').and.returnValue(mdSidenav);

      $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
      $rootScope.$digest();
    });

    it('focuses the sidepanel again when transitioning to the parent state fails (e.g. because a confirmation dialog is cancelled)', () => {
      $transitions.onBefore({ to: 'hippo-cm.channel' }, () => false);
      $state.go('hippo-cm.channel');
      $rootScope.$digest();
    });

    it('focuses the sidepanel again when transitioning to a sibling state fails (e.g. because a confirmation dialog is cancelled)', () => {
      $transitions.onBefore({ to: 'hippo-cm.channel.edit-content' }, () => false);
      $state.go('hippo-cm.channel.edit-content', { documentId: 'docId2' });
      $rootScope.$digest();
    });

    afterEach(() => {
      expect(mdSidenav.focus).toHaveBeenCalled();
    });
  });
});
