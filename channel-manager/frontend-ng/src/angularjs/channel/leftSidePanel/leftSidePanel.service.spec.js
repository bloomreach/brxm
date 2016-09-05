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

describe('ChannelLeftSidePanelService', () => {
  'use strict';

  let ChannelLeftSidePanelService;
  let ScalingService;
  const leftSidePanel = jasmine.createSpyObj('leftSidePanel', ['isOpen', 'toggle', 'close']);

  beforeEach(() => {
    module('hippo-cm');

    const $mdSidenav = jasmine.createSpy('$mdSidenav').and.returnValue(leftSidePanel);

    module(($provide) => {
      $provide.value('$mdSidenav', $mdSidenav);
    });

    inject((_ChannelLeftSidePanelService_, _ScalingService_) => {
      ChannelLeftSidePanelService = _ChannelLeftSidePanelService_;
      ScalingService = _ScalingService_;
    });

    spyOn(ScalingService, 'setPushWidth');
  });

  it('forwards the toggle to the mdSidenav service and updates the scaling service', () => {
    const element = angular.element('<div></div>');
    element.width(250);
    ChannelLeftSidePanelService.initialize(element);

    ScalingService.setPushWidth.calls.reset();
    leftSidePanel.toggle.calls.reset();
    leftSidePanel.isOpen.and.returnValue(true);

    ChannelLeftSidePanelService.toggle();

    expect(leftSidePanel.toggle).toHaveBeenCalled();
    expect(ScalingService.setPushWidth).toHaveBeenCalledWith(250);

    ScalingService.setPushWidth.calls.reset();
    leftSidePanel.toggle.calls.reset();
    leftSidePanel.isOpen.and.returnValue(false);

    ChannelLeftSidePanelService.toggle();

    expect(leftSidePanel.toggle).toHaveBeenCalled();
    expect(ScalingService.setPushWidth).toHaveBeenCalledWith(0);
  });

  it('forwards the is-open check to the mdSidenav service', () => {
    const element = angular.element('<div></div>');
    ChannelLeftSidePanelService.initialize(element);

    leftSidePanel.isOpen.and.returnValue(true);
    expect(ChannelLeftSidePanelService.isOpen()).toBe(true);

    leftSidePanel.isOpen.and.returnValue(false);
    expect(ChannelLeftSidePanelService.isOpen()).toBe(false);
  });

  it('the is-open check works when the left side panel has not been rendered yet', () => {
    leftSidePanel.isOpen.and.throwError('left side panel cannot be found');
    expect(ChannelLeftSidePanelService.isOpen()).toBeFalsy();
  });

  it('closes the left side panel if it is open', () => {
    const element = angular.element('<div></div>');
    ChannelLeftSidePanelService.initialize(element);

    ScalingService.setPushWidth.calls.reset();
    leftSidePanel.close.calls.reset();
    leftSidePanel.isOpen.and.returnValue(true);

    ChannelLeftSidePanelService.close();

    expect(ScalingService.setPushWidth).toHaveBeenCalledWith(0);
    expect(leftSidePanel.close).toHaveBeenCalled();

    ScalingService.setPushWidth.calls.reset();
    leftSidePanel.close.calls.reset();
    leftSidePanel.isOpen.and.returnValue(false);

    ChannelLeftSidePanelService.close();

    expect(ScalingService.setPushWidth).not.toHaveBeenCalled();
    expect(leftSidePanel.close).not.toHaveBeenCalled();
  });
});
