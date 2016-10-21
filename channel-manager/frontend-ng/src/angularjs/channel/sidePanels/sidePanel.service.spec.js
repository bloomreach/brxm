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

describe('ChannelSidePanelService', () => {
  let ChannelSidePanelService;
  const leftSidePanel = jasmine.createSpyObj('leftSidePanel', ['isOpen', 'toggle', 'open', 'close']);

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    const $mdSidenav = jasmine.createSpy('$mdSidenav').and.returnValue(leftSidePanel);

    angular.mock.module(($provide) => {
      $provide.value('$mdSidenav', $mdSidenav);
    });

    inject((_ChannelSidePanelService_) => {
      ChannelSidePanelService = _ChannelSidePanelService_;
    });
  });

  it('toggles a named side-panel', () => {
    const element = angular.element('<div></div>');
    element.width(250);
    ChannelSidePanelService.initialize('left', element);

    leftSidePanel.toggle.calls.reset();
    leftSidePanel.isOpen.and.returnValue(false);

    ChannelSidePanelService.toggle('left');

    expect(leftSidePanel.open).toHaveBeenCalled();

    leftSidePanel.toggle.calls.reset();
    leftSidePanel.isOpen.and.returnValue(true);

    ChannelSidePanelService.toggle('left');

    expect(leftSidePanel.close).toHaveBeenCalled();
  });

  it('forwards the is-open check to the mdSidenav service', () => {
    const element = angular.element('<div></div>');
    ChannelSidePanelService.initialize('left', element);

    leftSidePanel.isOpen.and.returnValue(true);
    expect(ChannelSidePanelService.isOpen('left')).toBe(true);

    leftSidePanel.isOpen.and.returnValue(false);
    expect(ChannelSidePanelService.isOpen('left')).toBe(false);
  });

  it('the is-open check works when the left side panel has not been rendered yet', () => {
    leftSidePanel.isOpen.and.throwError('left side panel cannot be found');
    expect(ChannelSidePanelService.isOpen('left')).toBeFalsy();
  });

  it('closes the left side panel if it is open', () => {
    const element = angular.element('<div></div>');
    ChannelSidePanelService.initialize('left', element);

    leftSidePanel.close.calls.reset();
    leftSidePanel.isOpen.and.returnValue(true);

    ChannelSidePanelService.close('left');

    expect(leftSidePanel.close).toHaveBeenCalled();

    leftSidePanel.close.calls.reset();
    leftSidePanel.isOpen.and.returnValue(false);

    ChannelSidePanelService.close('left');

    expect(leftSidePanel.close).not.toHaveBeenCalled();
  });

  it('forwards the open call to the mdSidenav service', () => {
    const element = angular.element('<div></div>');
    ChannelSidePanelService.initialize('left', element);

    ChannelSidePanelService.open('left');
    expect(leftSidePanel.open).toHaveBeenCalled();
  });

  it('calls the onOpen callback when specified', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    const onOpen = jasmine.createSpy('onOpen');

    ChannelSidePanelService.initialize('left', element, onOpen);

    ChannelSidePanelService.open('left');
    expect(onOpen).toHaveBeenCalled();
  });

  it('opens without errors when the onOpen callback is omitted', () => {
    const element = angular.element('<div md-component-id="left"></div>');
    ChannelSidePanelService.initialize('left', element);
    expect(() => {
      ChannelSidePanelService.open('left');
    }).not.toThrow(jasmine.any(Error));
  });
});
