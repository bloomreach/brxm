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
  let $q;
  let $translate;
  let ChannelService;
  let DialogService;
  let SessionService;

  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$q_, _$translate_, _ChannelService_, _DialogService_, _SessionService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $q = _$q_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      DialogService = _DialogService_;
      SessionService = _SessionService_;
    });

    spyOn($translate, 'instant');
    spyOn(ChannelService, 'getChannel').and.returnValue({ hasCustomProperties: true });
    spyOn(ChannelService, 'getName').and.returnValue('test-channel');
    spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
    spyOn(DialogService, 'show');
    spyOn(SessionService, 'canDeleteChannel').and.returnValue(true);
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
    const ChannelActionsCtrl = compileDirectiveAndGetController();

    ChannelActionsCtrl.openSettings();

    expect($scope.onActionSelected).toHaveBeenCalledWith('channel-settings');
  });

  it('doesn\'t expose the settings option if the channel has no custom properties', () => {
    let ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.isChannelSettingsAvailable()).toBe(true);

    ChannelService.getChannel.and.returnValue({ hasCustomProperties: false });
    ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.isChannelSettingsAvailable()).toBe(false);
  });

  it('doesn\'t expose the delete option if a user cannot delete a channel', () => {
    const ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.isChannelDeletionAvailable()).toBe(true);

    SessionService.canDeleteChannel.and.returnValue(false);
    expect(ChannelActionsCtrl.isChannelDeletionAvailable()).toBe(false);
  });

  it('displays the menu if there is at least one option available', () => {
    // both options
    let ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.hasMenuOptions()).toBe(true);

    // delete only
    ChannelService.getChannel.and.returnValue({ hasCustomProperties: false });
    ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.hasMenuOptions()).toBe(true);

    // no option
    SessionService.canDeleteChannel.and.returnValue(false);
    ChannelActionsCtrl = compileDirectiveAndGetController();
    expect(ChannelActionsCtrl.hasMenuOptions()).toBe(false);

    // settings only
    ChannelService.getChannel.and.returnValue({ hasCustomProperties: true });
    ChannelActionsCtrl = compileDirectiveAndGetController();

    expect(ChannelActionsCtrl.hasMenuOptions()).toBe(true);
  });

  it('doesn\'t delete a channel when canceling the confirmation dialog', () => {
    const ChannelActionsCtrl = compileDirectiveAndGetController();

    DialogService.show.and.returnValue($q.reject()); // cancel
    ChannelActionsCtrl.deleteChannel();
    expect(DialogService.confirm).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DELETE_CHANNEL_TITLE', { channel: 'test-channel' });
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DELETE_CHANNEL_MESSAGE');
    expect(DialogService.show).toHaveBeenCalled();

    $rootScope.$digest();
    // TODO: expect deletion not to have been triggered.
  });

  // TODO; add more tests for deleting a channel (once implemented)
});
