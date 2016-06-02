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

/* eslint-disable prefer-const */

describe('ChannelSidenavService', () => {
  'use strict';

  let ChannelSidenavService;
  let ScalingService;
  const sidenav = jasmine.createSpyObj('sidenav', ['isOpen', 'toggle', 'close']);

  beforeEach(() => {
    module('hippo-cm');

    const $mdSidenav = jasmine.createSpy('$mdSidenav').and.returnValue(sidenav);

    module(($provide) => {
      $provide.value('$mdSidenav', $mdSidenav);
    });

    inject((_ChannelSidenavService_, _ScalingService_) => {
      ChannelSidenavService = _ChannelSidenavService_;
      ScalingService = _ScalingService_;
    });

    spyOn(ScalingService, 'setPushWidth');
  });

  it('forwards the toggle to the mdSidenav service and updates the scaling service', () => {
    const element = angular.element('<div></div>');
    element.width(250);
    ChannelSidenavService.initialize(element);

    ScalingService.setPushWidth.calls.reset();
    sidenav.toggle.calls.reset();
    sidenav.isOpen.and.returnValue(true);

    ChannelSidenavService.toggle();

    expect(sidenav.toggle).toHaveBeenCalled();
    expect(ScalingService.setPushWidth).toHaveBeenCalledWith(250);

    ScalingService.setPushWidth.calls.reset();
    sidenav.toggle.calls.reset();
    sidenav.isOpen.and.returnValue(false);

    ChannelSidenavService.toggle();

    expect(sidenav.toggle).toHaveBeenCalled();
    expect(ScalingService.setPushWidth).toHaveBeenCalledWith(0);
  });

  it('forwards the is-open check to the mdSidenav service', () => {
    const element = angular.element('<div></div>');
    ChannelSidenavService.initialize(element);

    sidenav.isOpen.and.returnValue(true);
    expect(ChannelSidenavService.isOpen()).toBe(true);

    sidenav.isOpen.and.returnValue(false);
    expect(ChannelSidenavService.isOpen()).toBe(false);
  });

  it('the is-open check works when the sidenav has not been rendered yet', () => {
    sidenav.isOpen.and.throwError('Sidenav cannot be found');
    expect(ChannelSidenavService.isOpen()).toBeFalsy();
  });

  it('closes the sidenav if it is open', () => {
    const element = angular.element('<div></div>');
    ChannelSidenavService.initialize(element);

    ScalingService.setPushWidth.calls.reset();
    sidenav.close.calls.reset();
    sidenav.isOpen.and.returnValue(true);

    ChannelSidenavService.close();

    expect(ScalingService.setPushWidth).toHaveBeenCalledWith(0);
    expect(sidenav.close).toHaveBeenCalled();

    ScalingService.setPushWidth.calls.reset();
    sidenav.close.calls.reset();
    sidenav.isOpen.and.returnValue(false);

    ChannelSidenavService.close();

    expect(ScalingService.setPushWidth).not.toHaveBeenCalled();
    expect(sidenav.close).not.toHaveBeenCalled();
  });
});
