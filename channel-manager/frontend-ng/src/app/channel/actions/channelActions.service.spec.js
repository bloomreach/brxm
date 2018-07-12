/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
  let $state;
  let $translate;
  let $window;
  let ConfigService;
  let ChannelService;
  let CmsService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMapService;
  let ProjectService;

  let ChannelActionsService;

  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  const SessionServiceMock = jasmine.createSpyObj('SessionServiceMock', [
    'canDeleteChannel',
    'canManageChanges',
    'hasWriteAccess',
  ]);

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    angular.mock.module(($provide) => {
      $provide.value('SessionService', SessionServiceMock);
    });
  });

  const getItem = name => ChannelActionsService.menu.items.find(item => item.name === name);

  function doInject() {
    inject((
      _$rootScope_,
      _$q_,
      _$state_,
      _$translate_,
      _$window_,
      _ChannelActionsService_,
      _ConfigService_,
      _ChannelService_,
      _CmsService_,
      _DialogService_,
      _HippoIframeService_,
      _FeedbackService_,
      _SiteMapService_,
      _ProjectService_,
    ) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      ChannelActionsService = _ChannelActionsService_;
      ConfigService = _ConfigService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMapService = _SiteMapService_;
      ProjectService = _ProjectService_;
    });
  }

  describe('for editors', () => {
    beforeEach(() => {
      SessionServiceMock.canDeleteChannel.and.returnValue(true);
      SessionServiceMock.canManageChanges.and.returnValue(true);
      SessionServiceMock.hasWriteAccess.and.returnValue(true);

      doInject();

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
      spyOn(ProjectService, 'isBranch').and.returnValue(true);
      spyOn(SiteMapService, 'load');

      ProjectService.selectedProject = {
        name: 'testProject',
      };

      ConfigService.cmsUser = 'testUser';
    });

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
      SessionServiceMock.canManageChanges.and.returnValue(false);
      expect(manageChanges.isEnabled()).toBe(false);

      SessionServiceMock.canManageChanges.and.returnValue(true);
      expect(manageChanges.isEnabled()).toBe(false);

      ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
      SessionServiceMock.canManageChanges.and.returnValue(false);
      expect(manageChanges.isEnabled()).toBe(false);

      SessionServiceMock.canManageChanges.and.returnValue(true);
      expect(manageChanges.isEnabled()).toBe(true);

      ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser', 'otherUser'] });
      SessionServiceMock.canManageChanges.and.returnValue(false);
      expect(manageChanges.isEnabled()).toBe(false);

      SessionServiceMock.canManageChanges.and.returnValue(true);
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
      const divider = getItem('divider-2');
      expect(divider.isVisible()).toBe(true);
    });

    // close channel
    it('clears the channel upon closing', () => {
      spyOn(ChannelService, 'clearChannel');
      const close = getItem('close');

      close.onClick();
      $rootScope.$apply();

      expect(ChannelService.clearChannel).toHaveBeenCalled();
    });

    it('closes a channel by publishing a close-channel event', () => {
      const close = getItem('close');

      spyOn(CmsService, 'publish');
      spyOn($state, 'go').and.returnValue($q.resolve());

      close.onClick();
      $rootScope.$apply();

      expect($state.go).toHaveBeenCalledWith('hippo-cm');
      expect(CmsService.publish).toHaveBeenCalledWith('close-channel');
    });

    it('closes a channel when receiving a close-channel event from the CMS', () => {
      spyOn(CmsService, 'publish');
      spyOn($state, 'go').and.returnValue($q.resolve());

      $window.CMS_TO_APP.publish('close-channel');
      $rootScope.$apply();

      expect($state.go).toHaveBeenCalledWith('hippo-cm');
      expect(CmsService.publish).toHaveBeenCalledWith('close-channel');
    });

    it('does not close a channel when closing the right side-panel failed', () => {
      const close = getItem('close');
      spyOn(CmsService, 'publish');

      spyOn($state, 'go').and.returnValue($q.reject());

      close.onClick();
      $rootScope.$apply();

      expect($state.go).toHaveBeenCalledWith('hippo-cm');
      expect(CmsService.publish).not.toHaveBeenCalled();
    });

    // delete channel
    it('doesn\'t expose the "delete" option if a user cannot delete a channel', () => {
      const del = getItem('delete');
      expect(del.isEnabled()).toBe(true);

      SessionServiceMock.canDeleteChannel.and.returnValue(false);
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

    it('shows a disabled reject menu item if a branch is selected for a project not in review', () => {
      ConfigService.projectsEnabled = true;
      spyOn(ProjectService, 'isRejectEnabled').and.returnValue(false);
      expect(getItem('divider-1').isVisible()).toBe(true);
      expect(getItem('reject').isVisible()).toBeTruthy();
      expect(getItem('reject').isEnabled()).toBe(false);
      delete ConfigService.projectsEnabled;
    });

    it('shows an enabled reject menu item if a branch is selected for a project in review', () => {
      ConfigService.projectsEnabled = true;
      spyOn(ProjectService, 'isRejectEnabled').and.returnValue(true);
      expect(getItem('divider-1').isVisible()).toBe(true);
      expect(getItem('reject').isVisible()).toBeTruthy();
      expect(getItem('reject').isEnabled()).toBe(true);
      delete ConfigService.projectsEnabled;
    });

    it('shows a prompt on rejection of a channel', () => {
      const channelId = 'testChannel';
      const message = 'testMessage';

      spyOn(ProjectService, 'reject');
      ChannelService.getChannel.and.returnValue({
        id: `${channelId}-preview`,
      });
      DialogService.show.and.returnValue($q.resolve(message));

      ChannelActionsService._reject();
      $rootScope.$digest();

      expect(ProjectService.reject).toHaveBeenCalledWith(channelId, message);
    });

    it('shows a disabled accept menu item if a branch is selected for a project not in review', () => {
      ConfigService.projectsEnabled = true;
      spyOn(ProjectService, 'isAcceptEnabled').and.returnValue(false);
      expect(getItem('divider-1').isVisible()).toBe(true);
      expect(getItem('accept').isVisible()).toBeTruthy();
      expect(getItem('accept').isEnabled()).toBe(false);
      delete ConfigService.projectsEnabled;
    });

    it('shows an enabled accept menu item if a branch is selected for a project in review', () => {
      ConfigService.projectsEnabled = true;
      spyOn(ProjectService, 'isAcceptEnabled').and.returnValue(true);
      expect(getItem('divider-1').isVisible()).toBe(true);
      expect(getItem('accept').isVisible()).toBeTruthy();
      expect(getItem('accept').isEnabled()).toBe(true);
      delete ConfigService.projectsEnabled;
    });
  });

  describe('for authors', () => {
    beforeEach(() => {
      SessionServiceMock.hasWriteAccess.and.returnValue(false);

      doInject();
    });

    it('the menu has only the close option', () => {
      const close = getItem('close');
      expect(close).toBeDefined();
      expect(ChannelActionsService.menu.items.length).toBe(1);
    });
  });
});
