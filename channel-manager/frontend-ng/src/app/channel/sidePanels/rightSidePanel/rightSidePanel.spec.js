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

describe('RightSidePanel', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $state;
  let $timeout;
  let $transitions;
  let $window;
  let RightSidePanelService;
  let SidePanelService;

  let $ctrl;
  let $scope;
  let $element;
  let sideNavElement;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel');

    inject((
      _$componentController_,
      _$q_,
      _$rootScope_,
      _$state_,
      _$timeout_,
      _$transitions_,
      _$window_,
      _RightSidePanelService_,
    ) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $timeout = _$timeout_;
      $transitions = _$transitions_;
      $window = _$window_;
      RightSidePanelService = _RightSidePanelService_;
    });

    SidePanelService = jasmine.createSpyObj('SidePanelService', [
      'initialize', 'isOpen', 'close', 'focus', 'open', 'setFullScreen', 'isFullScreen',
    ]);

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

      expect($ctrl.width).toBe(400);
    });
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

  it('closes right side panel', () => {
    spyOn(RightSidePanelService, 'close');
    $ctrl.close();

    expect(RightSidePanelService.close).toHaveBeenCalled();
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

  it('triggers a window resize event after a full width toggle', () => {
    spyOn($window, 'dispatchEvent');

    $ctrl.setFullScreen(true);
    const evt = new Event('resize');
    expect($window.dispatchEvent.calls.mostRecent().args[0]).toEqual(evt);
    expect($window.dispatchEvent.calls.mostRecent().args[0].type).toEqual(evt.type);

    $ctrl.setFullScreen(false);
    expect($window.dispatchEvent.calls.mostRecent().args[0]).toEqual(evt);
    expect($window.dispatchEvent.calls.mostRecent().args[0].type).toEqual(evt.type);
  });

  it('detects ESC keypress', () => {
    const e = angular.element.Event('keydown');
    e.which = 27;

    spyOn($ctrl, 'close');
    $ctrl.$element.trigger(e);
    expect($ctrl.close).toHaveBeenCalled();
  });

  it('ignores other keys', () => {
    const e = angular.element.Event('keydown');
    e.which = 28;

    spyOn($ctrl, 'close');
    $ctrl.$element.trigger(e);
    expect($ctrl.close).not.toHaveBeenCalled();
  });

  it('stores the panel width in local storage on resize', () => {
    spyOn($ctrl.localStorageService, 'set');
    $ctrl.onResize(800);

    expect($ctrl.localStorageService.set).toHaveBeenCalledWith('channelManager.sidePanel.right.width', 800);
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

    $state.go('hippo-cm.channel');
    $rootScope.$digest();

    $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
    $rootScope.$digest();

    expect(SidePanelService.open).toHaveBeenCalledWith('right');
  });

  it('does not open the panel when transitioning from state "hippo-cm.channel.*" to "hippo-cm.channel.*"', () => {
    $ctrl.$onInit();

    $state.go('hippo-cm.channel');
    $rootScope.$digest();

    $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
    $rootScope.$digest();

    SidePanelService.open.calls.reset();
    $state.go('hippo-cm.channel.another-state', { documentId: 'docId2' });
    $rootScope.$digest();

    expect(SidePanelService.open).not.toHaveBeenCalled();
  });

  it('closes the panel when transitioning back to state "hippo-cm.channel"', () => {
    SidePanelService.open.and.returnValue($q.resolve());
    SidePanelService.close.and.returnValue($q.resolve());

    $ctrl.$onInit();

    $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
    $rootScope.$digest();
    SidePanelService.open.calls.reset();

    $state.go('hippo-cm.channel');
    $rootScope.$digest();

    expect(SidePanelService.open).not.toHaveBeenCalled();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($element.hasClass('side-panel-open')).toBe(false);
    expect($element.hasClass('full-screen')).toBe(false);
    expect(SidePanelService.setFullScreen).toHaveBeenCalledWith('right', false);
  });

  describe('focus handling', () => {
    beforeEach(() => {
      $ctrl.$onInit();

      $state.go('hippo-cm.channel.edit-content', { documentId: 'docId' });
      $rootScope.$digest();
    });

    it('focuses the sidepanel again when transitioning to the parent state fails '
      + '(e.g. because a confirmation dialog is cancelled)', () => {
      $transitions.onBefore({ to: 'hippo-cm.channel' }, () => false);
      $state.go('hippo-cm.channel');
      $rootScope.$digest();
    });

    it('focuses the sidepanel again when transitioning to a sibling state fails '
      + '(e.g. because a confirmation dialog is cancelled)', () => {
      $transitions.onBefore({ to: 'hippo-cm.channel.edit-content' }, () => false);
      $state.go('hippo-cm.channel.edit-content', { documentId: 'docId2' });
      $rootScope.$digest();
    });

    afterEach(() => {
      expect(SidePanelService.focus).toHaveBeenCalled();
    });
  });
});
