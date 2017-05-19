/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('ChannelActions', () => {
  let $rootScope;
  let $compile;
  let $scope;
  let $element;
  let $q;
  let $translate;
  let ConfigService;
  let ChannelService;
  let CmsService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let SessionService;
  let SiteMapService;

  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$rootScope_,
      _$compile_,
      _$q_,
      _$translate_,
      _ConfigService_,
      _ChannelService_,
      _CmsService_,
      _DialogService_,
      _HippoIframeService_,
      _FeedbackService_,
      _SessionService_,
      _SiteMapService_,
    ) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $q = _$q_;
      $translate = _$translate_;
      ConfigService = _ConfigService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn($translate, 'instant');
    spyOn(ChannelService, 'getChannel').and.returnValue({
      changedBySet: [],
      hasCustomProperties: true,
    });
    spyOn(ChannelService, 'getName').and.returnValue('test-channel');
    spyOn(ChannelService, 'publishOwnChanges').and.returnValue($q.resolve());
    spyOn(ChannelService, 'discardOwnChanges').and.returnValue($q.resolve());
    spyOn(ChannelService, 'deleteChannel').and.returnValue($q.resolve());
    spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
    spyOn(DialogService, 'alert').and.returnValue(confirmDialog);
    spyOn(DialogService, 'hide');
    spyOn(DialogService, 'show').and.returnValue($q.resolve());
    spyOn(FeedbackService, 'showError');
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'reload');
    spyOn(SessionService, 'canDeleteChannel').and.returnValue(true);
    spyOn(SessionService, 'canManageChanges').and.returnValue(true);
    spyOn(SiteMapService, 'load');

    ConfigService.cmsUser = 'testUser';
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onActionSelected = jasmine.createSpy('onActionSelected');
    $element = angular.element('<channel-actions on-action-selected="onActionSelected(subpage)"></channel-actions>');
    $compile($element)($scope);
    $scope.$digest();
    return $element.controller('channelActions');
  }

  it('opens settings by calling the on-action-selected callback', () => {
    const $ctrl = compileDirectiveAndGetController();

    $ctrl.openSettings();

    expect($scope.onActionSelected).toHaveBeenCalledWith('channel-settings');
  });

  it('doesn\'t expose the settings option if the channel has no custom properties', () => {
    let $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isChannelSettingsAvailable()).toBe(true);

    ChannelService.getChannel.and.returnValue({ hasCustomProperties: false });
    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isChannelSettingsAvailable()).toBe(false);
  });

  it('doesn\'t expose the delete option if a user cannot delete a channel', () => {
    const $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isChannelDeletionAvailable()).toBe(true);

    SessionService.canDeleteChannel.and.returnValue(false);
    expect($ctrl.isChannelDeletionAvailable()).toBe(false);
  });

  it('displays the menu if there is at least one option available', () => {
    // both options
    let $ctrl = compileDirectiveAndGetController();
    expect($ctrl.hasMenuOptions()).toBe(true);

    // delete only
    ChannelService.getChannel.and.returnValue({ hasCustomProperties: false });
    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.hasMenuOptions()).toBe(true);

    // no option
    SessionService.canDeleteChannel.and.returnValue(false);
    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.hasMenuOptions()).toBe(false);

    // settings only
    ChannelService.getChannel.and.returnValue({ hasCustomProperties: true });
    $ctrl = compileDirectiveAndGetController();

    expect($ctrl.hasMenuOptions()).toBe(true);
  });

  it('determines if there are own changes', () => {
    const $ctrl = compileDirectiveAndGetController();
    expect($ctrl.hasOwnChanges()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect($ctrl.hasOwnChanges()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect($ctrl.hasOwnChanges()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });
    expect($ctrl.hasOwnChanges()).toBe(true);

    ChannelService.getChannel.and.returnValue({});
    expect($ctrl.hasOwnChanges()).toBe(false);
  });

  it('enables the manage changes option when there are changes by other users', () => {
    let $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isManageChangesEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect($ctrl.isManageChangesEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect($ctrl.isManageChangesEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser', 'otherUser'] });
    expect($ctrl.isManageChangesEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({});
    expect($ctrl.isManageChangesEnabled()).toBe(false);

    SessionService.canManageChanges.and.returnValue(false);
    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isManageChangesEnabled()).toBe(false);
  });

  it('publishes changes', () => {
    const $ctrl = compileDirectiveAndGetController();
    $ctrl.publish();
    $rootScope.$digest();

    expect(ChannelService.publishOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('flashes a toast when publication failed', () => {
    const $ctrl = compileDirectiveAndGetController();

    const params = { };
    ChannelService.publishOwnChanges.and.returnValue($q.reject({ data: params }));
    $ctrl.publish();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', params);

    ChannelService.publishOwnChanges.and.returnValue($q.reject());
    $ctrl.publish();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', undefined);
  });

  it('discards changes', () => {
    const $ctrl = compileDirectiveAndGetController();
    $ctrl.discardChanges();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('flashes a toast with parameters when discarding failed', () => {
    const $ctrl = compileDirectiveAndGetController();
    const params = {};
    ChannelService.discardOwnChanges.and.returnValue($q.reject({ data: params }));

    $ctrl.discardChanges();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', params);
  });

  it('flashes a toast without parameters when discarding failed', () => {
    const $ctrl = compileDirectiveAndGetController();
    ChannelService.discardOwnChanges.and.returnValue($q.reject());

    $ctrl.discardChanges();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', undefined);
  });

  it('does not discard changes if not confirmed', () => {
    DialogService.show.and.returnValue($q.reject());

    const $ctrl = compileDirectiveAndGetController();
    $ctrl.discardChanges();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).not.toHaveBeenCalled();
  });

  it('manages changes by calling the on-action-selected callback', () => {
    const $ctrl = compileDirectiveAndGetController();

    $ctrl.manageChanges();

    expect($scope.onActionSelected).toHaveBeenCalledWith('manage-changes');
  });

  it('does not delete a channel when canceling the confirmation dialog', () => {
    const $ctrl = compileDirectiveAndGetController();

    DialogService.show.and.returnValue($q.reject()); // cancel
    $ctrl.deleteChannel();
    expect(DialogService.confirm).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DELETE_CHANNEL_TITLE', { channel: 'test-channel' });
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DELETE_CHANNEL_MESSAGE');
    expect(DialogService.show).toHaveBeenCalled();

    $rootScope.$digest();
    expect(ChannelService.deleteChannel).not.toHaveBeenCalled();
  });

  it('flashes a toast when the deletion of a channel failed for an unknown reason', () => {
    const $ctrl = compileDirectiveAndGetController();

    ChannelService.deleteChannel.and.returnValue($q.reject());
    $ctrl.deleteChannel();

    $rootScope.$digest();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].template).toBeDefined();
    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_CHANNEL_DELETE_FAILED');
  });

  it('flashes a toast when the deletion of a channel failed for an known reason', () => {
    const $ctrl = compileDirectiveAndGetController();
    const response = { trans: 'parent' };
    ChannelService.deleteChannel.and.returnValue($q.reject(response));
    $ctrl.deleteChannel();

    $rootScope.$digest();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].template).toBeDefined();
    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(response, 'ERROR_CHANNEL_DELETE_FAILED');
  });

  it('shows extended error feedback when child mounts prevented channel deletion', () => {
    const $ctrl = compileDirectiveAndGetController();
    const parameterMap = { trans: 'parent' };
    const response = {
      error: 'CHILD_MOUNT_EXISTS',
      parameterMap,
    };
    ChannelService.deleteChannel.and.returnValue($q.reject(response));
    $ctrl.deleteChannel();

    $rootScope.$digest();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(DialogService.alert).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('ERROR_CHANNEL_DELETE_FAILED_DUE_TO_CHILD_MOUNTS', parameterMap);
  });

  it('successfully deletes a channel', () => {
    const $ctrl = compileDirectiveAndGetController();

    spyOn(CmsService, 'subscribeOnce').and.callThrough();
    spyOn(CmsService, 'publish');

    $ctrl.deleteChannel();
    $rootScope.$digest();

    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].template).toBeDefined();
    expect(CmsService.publish).toHaveBeenCalledWith('channel-deleted');
    expect(CmsService.subscribeOnce).toHaveBeenCalledWith('channel-removed-from-overview', jasmine.any(Function));
    const channelRemovedFromOverviewCallback = CmsService.subscribeOnce.calls.mostRecent().args[1];

    channelRemovedFromOverviewCallback();
    expect(DialogService.hide).toHaveBeenCalled();
  });

  it('closes a channel by publishing a close-channel event', () => {
    const $ctrl = compileDirectiveAndGetController();
    spyOn(CmsService, 'publish');

    $ctrl.closeChannel();

    expect(CmsService.publish).toHaveBeenCalledWith('close-channel');
  });
});
