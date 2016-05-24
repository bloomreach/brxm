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

describe('ChannelActions', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let $scope;
  let $element;
  let ChannelService;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _ChannelService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      ChannelService = _ChannelService_;
    });

    spyOn(ChannelService, 'getChannel').and.returnValue({ hasCustomProperties: true });
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onActionSelected = jasmine.createSpy('onActionSelected');
    $element = angular.element('<channel-actions on-action-selected="onActionSelected(subpage)"></channel-actions>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('channel-actions');
  }

  it('calls the on-action-selected callback when clicking the button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-settings').click();

    expect($scope.onActionSelected).toHaveBeenCalledWith('channel-settings');
  });

  it('doesn\'t expose the functionality if the channel has no custom properties', () => {
    let ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.isChannelSettingsAvailable()).toBe(true);

    ChannelService.getChannel.and.returnValue({ hasCustomProperties: false });
    ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.isChannelSettingsAvailable()).toBe(false);
  });
});
