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

describe('ChannelSidenavControls', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let ChannelSidenavService;
  let ChannelService;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _ChannelSidenavService_, _ChannelService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      ChannelSidenavService = _ChannelSidenavService_;
      ChannelService = _ChannelService_;
    });

    spyOn(ChannelService, 'getCatalog').and.returnValue([]);
    spyOn(ChannelSidenavService, 'toggle');
    spyOn(ChannelSidenavService, 'isOpen');
  });

  function instantiateController() {
    const scope = $rootScope.$new();
    const el = angular.element('<channel-sidenav-controls edit-mode="editMode"></channel-sidenav-controls>');
    $compile(el)(scope);
    $rootScope.$digest();
    return el.controller('channel-sidenav-controls');
  }

  it('forwards the toggle call to the sidenav service', () => {
    const ControlsCtrl = instantiateController();
    expect(ChannelSidenavService.toggle).not.toHaveBeenCalled();

    ControlsCtrl.toggleSidenav();
    expect(ChannelSidenavService.toggle).toHaveBeenCalled();
  });

  it('forwards the is open call to the sidenav service', () => {
    const ControlsCtrl = instantiateController();
    ChannelSidenavService.isOpen.and.returnValue(false);
    expect(ControlsCtrl.isSidenavOpen()).toBe(false);
    ChannelSidenavService.isOpen.and.returnValue(true);
    expect(ControlsCtrl.isSidenavOpen()).toBe(true);
  });

  it('displays an icon depending on whether the sidenav is open or closed', () => {
    const ControlsCtrl = instantiateController();
    ChannelSidenavService.isOpen.and.returnValue(false);
    expect(ControlsCtrl.getSidenavIcon()).toBe('last_page');
    ChannelSidenavService.isOpen.and.returnValue(true);
    expect(ControlsCtrl.getSidenavIcon()).toBe('first_page');
  });
});
