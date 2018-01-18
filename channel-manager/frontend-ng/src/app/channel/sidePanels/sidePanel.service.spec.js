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

describe('SidePanelService', () => {
  let SidePanelService;
  let $q;
  let $rootScope;
  let leftSidePanel;
  let OverlayService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    leftSidePanel = jasmine.createSpyObj('leftSidePanel', ['isOpen', 'toggle', 'open', 'close', 'onClose', 'onCloseCb']);
    OverlayService = jasmine.createSpyObj('OverlayService', ['sync']);

    const $mdSidenav = jasmine.createSpy('$mdSidenav').and.returnValue(leftSidePanel);

    angular.mock.module(($provide) => {
      $provide.value('$mdSidenav', $mdSidenav);
      $provide.value('OverlayService', OverlayService);
    });

    inject((_SidePanelService_, _$q_, _$rootScope_) => {
      SidePanelService = _SidePanelService_;
      $q = _$q_;
      $rootScope = _$rootScope_;
    });

    leftSidePanel.open.and.returnValue($q.resolve());
    leftSidePanel.close.and.returnValue($q.resolve());
  });

  it('toggles a named side-panel', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    element.width(250);
    SidePanelService.initialize('left', element);

    leftSidePanel.isOpen.and.returnValue(false);

    SidePanelService.toggle('left');

    expect(leftSidePanel.open).toHaveBeenCalled();

    leftSidePanel.toggle.calls.reset();
    leftSidePanel.isOpen.and.returnValue(true);

    SidePanelService.toggle('left');
    $rootScope.$digest();

    expect(leftSidePanel.close).toHaveBeenCalled();
  });

  it('lifts and lowers side panel above mask', () => {
    SidePanelService.isSidePanelLifted = false;
    SidePanelService.liftSidePanelAboveMask();
    expect(SidePanelService.isSidePanelLifted).toBeTruthy();

    SidePanelService.isSidePanelLifted = true;
    SidePanelService.lowerSidePanelBeneathMask();
    expect(SidePanelService.isSidePanelLifted).toBeFalsy();
  });

  it('calls the onCloseCallback when $mdSidenav.close is triggered from AngularJS Material\'s side', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    leftSidePanel.onClose.and.callFake((fn) => { leftSidePanel.onCloseCb = fn; });
    leftSidePanel.close.and.callFake(() => leftSidePanel.onCloseCb());
    SidePanelService.initialize('left', element);

    spyOn(SidePanelService.panels.left, 'onCloseCallback').and.callThrough();
    leftSidePanel.close(); // "Native" AngularJS close function, as if we were pressing Escape
    expect(SidePanelService.panels.left.onCloseCallback).toHaveBeenCalled();
  });

  it('calls the native $mdSidenav close function if closing the sidenav internally', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    SidePanelService.initialize('left', element);
    SidePanelService.close('left');
    expect(leftSidePanel.close).toHaveBeenCalled();
  });

  it('forwards the is-open check to the mdSidenav service', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    SidePanelService.initialize('left', element);

    leftSidePanel.isOpen.and.returnValue(true);
    expect(SidePanelService.isOpen('left')).toBe(true);

    leftSidePanel.isOpen.and.returnValue(false);
    expect(SidePanelService.isOpen('left')).toBe(false);
  });

  it('the is-open check works when the left side panel has not been rendered yet', () => {
    leftSidePanel.isOpen.and.throwError('left side panel cannot be found');
    expect(SidePanelService.isOpen('left')).toBeFalsy();
  });

  it('closes a side panel if it is open', (done) => {
    const element = angular.element('<div md-component-id="left"></div>');
    SidePanelService.initialize('left', element);

    leftSidePanel.isOpen.and.returnValue(false);

    SidePanelService.close('left').then(() => {
      expect(leftSidePanel.close).not.toHaveBeenCalled();
      done();
    });
    $rootScope.$digest();
  });

  it('forwards the open call to the mdSidenav service', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    SidePanelService.initialize('left', element);

    SidePanelService.open('left');
    expect(leftSidePanel.open).toHaveBeenCalled();
  });

  it('ignores the open call when the a side panel has not been rendered yet', () => {
    expect(() => {
      SidePanelService.open('left');
    }).not.toThrow(jasmine.any(Error));
  });

  it('syncs the iframe when the side-panel is closed or opened', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    leftSidePanel.onClose.and.callFake((fn) => { leftSidePanel.onCloseCb = fn; });
    leftSidePanel.close.and.callFake(() => leftSidePanel.onCloseCb());
    SidePanelService.initialize('left', element);
    spyOn(SidePanelService.panels.left, 'onCloseCallback').and.returnValue($q.resolve());

    leftSidePanel.close();
    $rootScope.$digest();
    expect(OverlayService.sync).toHaveBeenCalled();

    OverlayService.sync.calls.reset();

    leftSidePanel.isOpen.and.returnValue(false);
    SidePanelService.open('left');
    $rootScope.$digest();
    expect(OverlayService.sync).toHaveBeenCalled();
  });
});
