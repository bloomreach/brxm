/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('ChannelActionsService', () => {
  let $rootScope;
  let $q;
  let $translate;
  let $window;
  let ConfigService;
  let ChannelService;
  let CmsService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let SessionService;
  let SiteMapService;
  let SidePanelService;

  let ChannelActionsService;

  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$rootScope_,
      _$q_,
      _$translate_,
      _$window_,
      _ChannelActionsService_,
      _ConfigService_,
      _ChannelService_,
      _CmsService_,
      _DialogService_,
      _HippoIframeService_,
      _FeedbackService_,
      _SessionService_,
      _SiteMapService_,
      _SidePanelService_,
    ) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      $translate = _$translate_;
      $window = _$window_;
      ChannelActionsService = _ChannelActionsService_;
      ConfigService = _ConfigService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
      SidePanelService = _SidePanelService_;
    });

    spyOn($translate, 'instant');
    spyOn(ChannelService, 'getChannel').and.returnValue({
      changedBySet: [],
      hasCustomProperties: true,
    });
    spyOn(ChannelService, 'getName').and.returnValue('test-channel');
    spyOn(ChannelService, 'isEditable').and.returnValue(true);
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

  const getItem = name => ChannelActionsService.menu.items.find(item => item.name === name);

  // menu button
  it('shows an icon in the menu button if there are any changes', () => {
    const menu = ChannelActionsService.menu;
    expect(menu.isIconVisible()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect(menu.isIconVisible()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect(menu.isIconVisible()).toBe(true);

    ChannelService.getChannel.and.returnValue({});
    expect(menu.isIconVisible()).toBe(false);
  });

  // channel settings
  it('does not expose the "settings" option if the channel is not editable', () => {
    const settings = getItem('settings');
    expect(settings.isEnabled()).toBe(true);

    ChannelService.isEditable.and.returnValue(false);
    expect(settings.isEnabled()).toBe(false);
  });

  it('does not expose the "settings" option if the channel has no custom properties', () => {
    const settings = getItem('settings');
    expect(settings.isEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({ hasCustomProperties: false });
    expect(settings.isEnabled()).toBe(false);
  });

  it('opens the channel-settings subpage when "settings" option is clicked', () => {
    spyOn(ChannelActionsService, 'showSubPage');

    getItem('settings').onClick();
    expect(ChannelActionsService.showSubPage).toHaveBeenCalledWith('channel-settings');
  });

  // first divider
  it('should show a divider between the "settings" option and the "changes" options', () => {
    const divider = getItem('divider-0');
    expect(divider.isVisible()).toBe(true);
  });

  // publish changes
  it('shows the "publish" option when the user has changes', () => {
    const publish = getItem('publish');
    expect(publish.isEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect(publish.isEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect(publish.isEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });
    expect(publish.isEnabled()).toBe(true);
  });

  it('publishes own changes when "publish" is clicked', () => {
    getItem('publish').onClick();
    $rootScope.$digest();

    expect(ChannelService.publishOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('flashes a toast when publication failed', () => {
    const params = {};
    ChannelService.publishOwnChanges.and.returnValue($q.reject({ data: params }));
    getItem('publish').onClick();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', params);

    ChannelService.publishOwnChanges.and.returnValue($q.reject());
    getItem('publish').onClick();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', undefined);
  });

  // discard changes
  it('shows the "discard changes" option when the user has changes', () => {
    const discard = getItem('discard-changes');
    expect(discard.isEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect(discard.isEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect(discard.isEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });
    expect(discard.isEnabled()).toBe(true);
  });

  it('discards changes when "discard changes" option is clicked', () => {
    getItem('discard-changes').onClick();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('flashes a toast when discarding failed', () => {
    const params = {};
    ChannelService.discardOwnChanges.and.returnValue($q.reject({ data: params }));

    getItem('discard-changes').onClick();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', params);

    ChannelService.discardOwnChanges.and.returnValue($q.reject());
    getItem('discard-changes').onClick();
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', undefined);
  });

  it('does not discard changes if not confirmed', () => {
    DialogService.show.and.returnValue($q.reject());

    getItem('discard-changes').onClick();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).not.toHaveBeenCalled();
  });

  // manage changes
  it('enables the "manage changes" option when there are changes by other users', () => {
    const manageChanges = getItem('manage-changes');
    expect(manageChanges.isEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    SessionService.canManageChanges.and.returnValue(false);
    expect(manageChanges.isEnabled()).toBe(false);

    SessionService.canManageChanges.and.returnValue(true);
    expect(manageChanges.isEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    SessionService.canManageChanges.and.returnValue(false);
    expect(manageChanges.isEnabled()).toBe(false);

    SessionService.canManageChanges.and.returnValue(true);
    expect(manageChanges.isEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser', 'otherUser'] });
    SessionService.canManageChanges.and.returnValue(false);
    expect(manageChanges.isEnabled()).toBe(false);

    SessionService.canManageChanges.and.returnValue(true);
    expect(manageChanges.isEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({});
    expect(manageChanges.isEnabled()).toBe(false);
  });

  it('opens the manages-changes subpage when "manages changes" option is clicked', () => {
    spyOn(ChannelActionsService, 'showSubPage');
    getItem('manage-changes').onClick();

    expect(ChannelActionsService.showSubPage).toHaveBeenCalledWith('manage-changes');
  });

  // second divider
  it('shows a divider between either the "settings" or the "change" option(s), and the "close" option', () => {
    const divider = getItem('divider-1');
    expect(divider.isVisible()).toBe(true);
  });

  // close channel
  it('closes a channel by publishing a close-channel event', () => {
    const close = getItem('close');

    spyOn(CmsService, 'publish');
    spyOn(SidePanelService, 'close').and.returnValue($q.resolve());

    close.onClick();
    $rootScope.$apply();

    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('close-channel');
  });

  it('closes a channel when receiving a close-channel event from the CMS', () => {
    spyOn(CmsService, 'publish');
    spyOn(SidePanelService, 'close').and.returnValue($q.resolve());

    $window.CMS_TO_APP.publish('close-channel');
    $rootScope.$apply();

    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('close-channel');
  });

  it('does not close a channel when closing the right side-panel failed', () => {
    const close = getItem('close');
    spyOn(CmsService, 'publish');

    spyOn(SidePanelService, 'close').and.returnValue($q.reject());

    close.onClick();
    $rootScope.$apply();

    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).not.toHaveBeenCalled();
  });

  // delete channel
  it('doesn\'t expose the "delete" option if a user cannot delete a channel', () => {
    const del = getItem('delete');
    expect(del.isEnabled()).toBe(true);

    SessionService.canDeleteChannel.and.returnValue(false);
    expect(del.isEnabled()).toBe(false);
  });

  it('successfully deletes a channel', () => {
    spyOn(CmsService, 'subscribeOnce').and.callThrough();
    spyOn(CmsService, 'publish');

    getItem('delete').onClick();
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

  it('does not delete a channel when canceling the confirmation dialog', () => {
    DialogService.show.and.returnValue($q.reject()); // cancel
    getItem('delete').onClick();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DELETE_CHANNEL_TITLE', { channel: 'test-channel' });
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DELETE_CHANNEL_MESSAGE');
    expect(DialogService.show).toHaveBeenCalled();

    $rootScope.$digest();
    expect(ChannelService.deleteChannel).not.toHaveBeenCalled();
  });

  it('flashes a toast when the deletion of a channel failed for an unknown reason', () => {
    ChannelService.deleteChannel.and.returnValue($q.reject());
    getItem('delete').onClick();

    $rootScope.$digest();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].template).toBeDefined();
    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_CHANNEL_DELETE_FAILED');
  });

  it('flashes a toast when the deletion of a channel failed for an known reason', () => {
    const response = { trans: 'parent' };
    ChannelService.deleteChannel.and.returnValue($q.reject(response));
    getItem('delete').onClick();

    $rootScope.$digest();
    // make sure the mask was shown
    expect(DialogService.show.calls.mostRecent().args[0].template).toBeDefined();
    expect(ChannelService.deleteChannel).toHaveBeenCalled();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(response, 'ERROR_CHANNEL_DELETE_FAILED');
  });

  it('shows extended error feedback when child mounts prevented channel deletion', () => {
    const parameterMap = { trans: 'parent' };
    const response = {
      error: 'CHILD_MOUNT_EXISTS',
      parameterMap,
    };
    ChannelService.deleteChannel.and.returnValue($q.reject(response));
    getItem('delete').onClick();

    $rootScope.$digest();
    expect(DialogService.hide).toHaveBeenCalled();
    expect(DialogService.alert).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('ERROR_CHANNEL_DELETE_FAILED_DUE_TO_CHILD_MOUNTS', parameterMap);
  });
});
