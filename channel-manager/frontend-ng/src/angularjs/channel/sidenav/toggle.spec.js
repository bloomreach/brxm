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

import angular from 'angular';
import 'angular-mocks';

describe('ChannelSidenavToggle', () => {
  let $rootScope;
  let $compile;
  let ChannelSidenavService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$compile_, _ChannelSidenavService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      ChannelSidenavService = _ChannelSidenavService_;
    });

    spyOn(ChannelSidenavService, 'toggle');
    spyOn(ChannelSidenavService, 'isOpen');
  });

  function instantiateController() {
    const scope = $rootScope.$new();
    const el = angular.element('<channel-sidenav-toggle edit-mode="editMode"></channel-sidenav-toggle>');
    $compile(el)(scope);
    $rootScope.$digest();
    return el.controller('channel-sidenav-toggle');
  }

  it('forwards the toggle call to the sidenav service', () => {
    const ToggleCtrl = instantiateController();
    expect(ChannelSidenavService.toggle).not.toHaveBeenCalled();

    ToggleCtrl.toggleSidenav();
    expect(ChannelSidenavService.toggle).toHaveBeenCalled();
  });

  it('forwards the is open call to the sidenav service', () => {
    const ToggleCtrl = instantiateController();
    ChannelSidenavService.isOpen.and.returnValue(false);
    expect(ToggleCtrl.isSidenavOpen()).toBe(false);
    ChannelSidenavService.isOpen.and.returnValue(true);
    expect(ToggleCtrl.isSidenavOpen()).toBe(true);
  });
});
