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
  let CmsService;
  let DialogService;
  let FeedbackService;
  let SessionService;

  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$q_, _$translate_, _ChannelService_, _CmsService_, _DialogService_, _FeedbackService_,
            _SessionService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $q = _$q_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      SessionService = _SessionService_;
    });

    spyOn($translate, 'instant');
    spyOn(ChannelService, 'getChannel').and.returnValue({ hasCustomProperties: true });
    spyOn(ChannelService, 'getName').and.returnValue('test-channel');
    spyOn(ChannelService, 'deleteChannel').and.returnValue($q.when());
    spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
    spyOn(DialogService, 'alert').and.returnValue(confirmDialog);
    spyOn(DialogService, 'hide');
    spyOn(DialogService, 'show').and.returnValue($q.when());
    spyOn(FeedbackService, 'showErrorResponse');
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
    expect(ChannelService.deleteChannel).not.toHaveBeenCalled();
  });

  it('flashes a toast when the deletion of a channel failed for an unknown reason', () => {
    const ChannelActionsCtrl = compileDirectiveAndGetController();

    ChannelService.deleteChannel.and.returnValue($q.reject());
    ChannelActionsCtrl.deleteChannel();

    $rootScope.$digest();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].templateUrl).toBeDefined();
    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_CHANNEL_DELETE_FAILED');
  });

  it('flashes a toast when the deletion of a channel failed for an known reason', () => {
    const ChannelActionsCtrl = compileDirectiveAndGetController();
    const response = { trans: 'parent' };
    ChannelService.deleteChannel.and.returnValue($q.reject(response));
    ChannelActionsCtrl.deleteChannel();

    $rootScope.$digest();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].templateUrl).toBeDefined();
    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(response, 'ERROR_CHANNEL_DELETE_FAILED');
  });

  it('shows extended error feedback when child mounts prevented channel deletion', () => {
    const ChannelActionsCtrl = compileDirectiveAndGetController();
    const parameterMap = { trans: 'parent' };
    const response = {
      error: 'CHILD_MOUNT_EXISTS',
      parameterMap,
    };
    ChannelService.deleteChannel.and.returnValue($q.reject(response));
    ChannelActionsCtrl.deleteChannel();

    $rootScope.$digest();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(DialogService.alert).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('ERROR_CHANNEL_DELETE_FAILED_DUE_TO_CHILD_MOUNTS', parameterMap);
  });

  it('successfully deletes a channel', () => {
    const ChannelActionsCtrl = compileDirectiveAndGetController();

    spyOn(CmsService, 'subscribeOnce').and.callThrough();
    spyOn(CmsService, 'publish');

    ChannelActionsCtrl.deleteChannel();
    $rootScope.$digest();

    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].templateUrl).toBeDefined();
    expect(CmsService.publish).toHaveBeenCalledWith('channel-deleted');
    expect(CmsService.subscribeOnce).toHaveBeenCalledWith('channel-removed-from-overview', jasmine.any(Function));
    const channelRemovedFromOverviewCallback = CmsService.subscribeOnce.calls.mostRecent().args[1];

    channelRemovedFromOverviewCallback();
    expect(DialogService.hide).toHaveBeenCalled();
  });
});
