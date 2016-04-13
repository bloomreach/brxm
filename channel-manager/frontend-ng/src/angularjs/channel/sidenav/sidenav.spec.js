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

describe('ChannelSidenav', () => {
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
    spyOn(ChannelSidenavService, 'initialize');
  });

  function instantiateController() {
    const scope = $rootScope.$new();
    const el = angular.element('<channel-sidenav></channel-sidenav>');
    $compile(el)(scope);
    $rootScope.$digest();
    return el.controller('channel-sidenav');
  }

  it('initializes the channel sidenav service upon instantiation', () => {
    instantiateController();

    expect(ChannelSidenavService.initialize).toHaveBeenCalled();
  });

  it('retrieves the catalog from the channel service', () => {
    const catalogComponents = [
      { label: 'dummy' },
    ];
    ChannelService.getCatalog.and.returnValue(catalogComponents);
    const ChannelSidenavCtrl = instantiateController();

    expect(ChannelSidenavCtrl.getCatalog()).toBe(catalogComponents);
  });
});

