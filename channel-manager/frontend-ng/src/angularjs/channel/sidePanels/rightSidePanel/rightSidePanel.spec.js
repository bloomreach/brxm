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

describe('ChannelRightSidePanel', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let ChannelSidePanelService;
  let parentScope;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _ChannelSidePanelService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      ChannelSidePanelService = _ChannelSidePanelService_;
    });

    spyOn(ChannelSidePanelService, 'initialize');
    spyOn(ChannelSidePanelService, 'close');
  });

  function instantiateController(editMode) {
    parentScope = $rootScope.$new();
    parentScope.editMode = editMode;
    const el = angular.element('<channel-right-side-panel edit-mode="editMode"></channel-right-side-panel>');
    $compile(el)(parentScope);
    $rootScope.$digest();
    return el.controller('channel-right-side-panel');
  }

  it('initializes the channel right side panel service upon instantiation', () => {
    instantiateController(false);

    expect(ChannelSidePanelService.initialize).toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalled();
  });

  it('closes the panel', () => {
    const $ctrl = instantiateController(false);

    $ctrl.close();

    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
  });
});

