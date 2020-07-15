/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

describe('ChannelMenuService', () => {
  let $q;
  let $rootScope;
  let $state;
  let $translate;
  let $window;
  let ChannelMenuService;
  let ChannelService;
  let CmsService;
  let ConfigService;
  let DialogService;
  let EditComponentService;
  let FeedbackService;
  let HippoIframeService;
  let PageService;
  let ProjectService;
  let SessionService;
  let SiteMapService;

  let confirmDialog;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
    confirmDialog.title.and.returnValue(confirmDialog);
    confirmDialog.textContent.and.returnValue(confirmDialog);
    confirmDialog.ok.and.returnValue(confirmDialog);
    confirmDialog.cancel.and.returnValue(confirmDialog);

    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _$translate_,
      _$window_,
      _ChannelMenuService_,
      _ChannelService_,
      _CmsService_,
      _ConfigService_,
      _DialogService_,
      _EditComponentService_,
      _HippoIframeService_,
      _FeedbackService_,
      _PageService_,
      _SessionService_,
      _SiteMapService_,
      _ProjectService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      ChannelMenuService = _ChannelMenuService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      ConfigService = _ConfigService_;
      DialogService = _DialogService_;
      EditComponentService = _EditComponentService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      PageService = _PageService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
      ProjectService = _ProjectService_;
    });
  });

  function getItem(name) {
    return ChannelMenuService.menu.items.find(item => item.name === name);
  }

  function expectMenuActionAfterComponentEditorIsClosed(item, ...spies) {
    EditComponentService.stopEditing.and.returnValue($q.reject());
    getItem(item).onClick();
    $rootScope.$digest();

    spies.forEach(spy => expect(spy).not.toHaveBeenCalled());

    EditComponentService.stopEditing.and.returnValue($q.resolve());
    getItem(item).onClick();
    $rootScope.$digest();

    spies.forEach(spy => expect(spy).toHaveBeenCalled());
  }

  function addAction(name, enabled = true) {
    if (!PageService.actions) {
      PageService.actions = {
        channel: {
          items: {},
        },
      };
    }
    PageService.actions.channel.items[name] = {
      enabled,
    };
  }

  beforeEach(() => {
    spyOn($translate, 'instant');
    spyOn(ChannelMenuService, 'showSubPage');
    spyOn(ChannelService, 'getChannel').and.returnValue({ changedBySet: [] });
    spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
    spyOn(DialogService, 'hide');
    spyOn(DialogService, 'show').and.returnValue($q.resolve());
    spyOn(EditComponentService, 'stopEditing').and.returnValue($q.resolve());
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'reload');

    PageService.actions = null;
    ConfigService.cmsUser = 'testUser';
  });

  describe('channel menu', () => {
    it('should hide the menu button', () => {
      expect(ChannelMenuService.menu.isVisible()).toBe(false);
    });

    it('should show the menu button', () => {
      PageService.actions = {
        channel: {},
      };

      expect(ChannelMenuService.menu.isVisible()).toBe(true);
    });

    it('shows an icon in the menu button if there are any changes', () => {
      spyOn(SessionService, 'canManageChanges').and.returnValue(true);

      const { menu } = ChannelMenuService;
      expect(menu.isIconVisible()).toBe(false);

      ChannelService.getChannel.and.returnValue({});
      expect(menu.isIconVisible()).toBe(false);

      ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
      expect(menu.isIconVisible()).toBe(true);

      ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
      expect(menu.isIconVisible()).toBe(true);
    });
  });

  describe('settings', () => {
    it('should hide the "settings" action', () => {
      expect(getItem('settings').isVisible()).toBe(false);
    });

    it('should show the "settings" action', () => {
      addAction('settings');

      expect(getItem('settings').isVisible()).toBe(true);
      expect(getItem('settings').isEnabled()).toBe(true);
    });

    it('should show a disabled "settings" action', () => {
      addAction('settings', false);

      expect(getItem('settings').isVisible()).toBe(true);
      expect(getItem('settings').isEnabled()).toBe(false);
    });

    it('opens the channel-settings subpage when "settings" action is clicked', () => {
      addAction('settings');

      getItem('settings').onClick();
      expect(ChannelMenuService.showSubPage).toHaveBeenCalledWith('channel-settings');
    });
  });

  describe('first divider', () => {
    it('should not show a divider if the "settings" option is not visible', () => {
      expect(getItem('divider-0').isVisible()).toBe(false);
    });

    it('should show a divider between the "settings" option and the "changes" options', () => {
      addAction('settings');

      expect(getItem('divider-0').isVisible()).toBe(true);
    });
  });

  describe('publish', () => {
    it('should hide the "publish" action', () => {
      expect(getItem('publish').isVisible()).toBe(false);
    });

    it('should hide the "publish" action if the selected project is not the master branch', () => {
      addAction('publish');
      expect(getItem('publish').isVisible()).toBe(true);
      addAction('publish', false);
      expect(getItem('publish').isEnabled()).toBe(false);
    });

    it('should show the "publish" action if the selected project is the master branch', () => {
      addAction('publish');
      spyOn(ProjectService, 'isBranch').and.returnValue(false);

      expect(getItem('publish').isVisible()).toBe(true);
    });

    it('should show a disabled "publish" action if user has no changes', () => {
      addAction('publish');

      const publish = getItem('publish');
      expect(publish.isEnabled()).toBe(true);

      addAction('publish', false);
      expect(publish.isEnabled()).toBe(false);
    });

    it('should show an enabled "publish" action if user has changes', () => {
      addAction('publish');
      ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });

      expect(getItem('publish').isEnabled()).toBe(true);
    });

    it('publishes own changes when "publish" is clicked', () => {
      addAction('publish');
      spyOn(ChannelService, 'publishOwnChanges').and.returnValue($q.resolve());

      expectMenuActionAfterComponentEditorIsClosed(
        'publish',
        ChannelService.publishOwnChanges,
        HippoIframeService.reload,
      );
    });

    it('flashes a toast when publication failed', () => {
      const params = { data: {} };
      spyOn(ChannelService, 'publishOwnChanges').and.returnValue($q.reject(params));

      getItem('publish').onClick();
      $rootScope.$digest();

      expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(params, 'ERROR_CHANGE_PUBLICATION_FAILED');

      ChannelService.publishOwnChanges.and.returnValue($q.reject());
      getItem('publish').onClick();
      $rootScope.$digest();

      expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(params, 'ERROR_CHANGE_PUBLICATION_FAILED');
    });
  });

  describe('confirm', () => {
    it('should hide the "confirm" action', () => {
      expect(getItem('confirm').isVisible()).toBe(false);
    });

    it('should hide the "confirm" action if the selected project is the master branch', () => {
      addAction('confirm');
      expect(getItem('confirm').isVisible()).toBe(true);
      addAction('confirm', false);
      expect(getItem('confirm').isEnabled()).toBe(false);
    });

    it('should show the "confirm" action if the selected project is not the master branch', () => {
      addAction('confirm');
      spyOn(ProjectService, 'isBranch').and.returnValue(true);

      expect(getItem('confirm').isVisible()).toBe(true);
    });

    it('should show a disabled "confirm" action if user has no changes', () => {
      addAction('confirm');

      const confirm = getItem('confirm');
      expect(confirm.isEnabled()).toBe(true);

      addAction('confirm', false);
      expect(confirm.isEnabled()).toBe(false);
    });

    it('should show an enabled "confirm" action if user has changes', () => {
      addAction('confirm');
      ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });

      expect(getItem('confirm').isEnabled()).toBe(true);
    });

    it('publishes changes when "confirm" is clicked', () => {
      addAction('confirm');
      spyOn(ChannelService, 'publishOwnChanges').and.returnValue($q.resolve());

      expectMenuActionAfterComponentEditorIsClosed(
        'confirm',
        ChannelService.publishOwnChanges,
        HippoIframeService.reload,
      );
    });
  });

  describe('discard changes', () => {
    it('should hide the "discard-changes" action', () => {
      expect(getItem('discard-changes').isVisible()).toBe(false);
    });

    it('should show a disabled "discard changes" action if user has no changes', () => {
      addAction('discard-changes');

      const discard = getItem('discard-changes');
      expect(discard.isEnabled()).toBe(true);

      addAction('discard-changes', false);
      expect(discard.isEnabled()).toBe(false);
    });

    it('should show an enabled "discard changes" option if user has changes', () => {
      addAction('discard-changes');

      ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });

      expect(getItem('discard-changes').isEnabled()).toBe(true);
    });

    it('discards changes when "discard changes" option is clicked', () => {
      spyOn(ChannelService, 'discardOwnChanges').and.returnValue($q.resolve());
      spyOn(SiteMapService, 'load');

      expectMenuActionAfterComponentEditorIsClosed(
        'discard-changes',
        DialogService.confirm,
        DialogService.show,
        ChannelService.discardOwnChanges,
        HippoIframeService.reload,
        SiteMapService.load,
      );
    });

    it('flashes a toast when discarding failed', () => {
      const params = { data: {} };
      spyOn(ChannelService, 'discardOwnChanges').and.returnValue($q.reject(params));

      getItem('discard-changes').onClick();
      $rootScope.$digest();

      expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(params, 'ERROR_CHANGE_DISCARD_FAILED');

      ChannelService.discardOwnChanges.and.returnValue($q.reject());
      getItem('discard-changes').onClick();
      $rootScope.$digest();

      expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(params, 'ERROR_CHANGE_DISCARD_FAILED');
    });

    it('does not discard changes if not confirmed', () => {
      DialogService.show.and.returnValue($q.reject());
      spyOn(ChannelService, 'discardOwnChanges');

      getItem('discard-changes').onClick();
      $rootScope.$digest();

      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(ChannelService.discardOwnChanges).not.toHaveBeenCalled();
    });
  });

  describe('manage changes', () => {
    it('should hide the "manage-changes" action', () => {
      expect(getItem('manage-changes').isVisible()).toBe(false);
    });

    it('should show a disabled "manage changes" action if there are no changes by other users', () => {
      const manage = getItem('manage-changes');

      addAction('manage-changes', false);
      expect(manage.isEnabled()).toBe(false);

      addAction('manage-changes', true);
      expect(manage.isEnabled()).toBe(true);
    });

    it('should show an enabled "manage changes" action if there are changes by other users', () => {
      addAction('manage-changes');

      ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser', 'otherUser'] });
      expect(getItem('manage-changes').isEnabled()).toBe(true);
    });

    it('opens the manages-changes subpage when "manages changes" action is clicked', () => {
      expectMenuActionAfterComponentEditorIsClosed('manage-changes', ChannelMenuService.showSubPage);

      expect(ChannelMenuService.showSubPage).toHaveBeenCalledWith('manage-changes');
    });
  });

  describe('accept', () => {
    it('should hide the "accept" action', () => {
      expect(getItem('accept').isVisible()).toBe(false);
    });

    it('should show a disabled "accept" action if a branch is selected for a project not in review', () => {
      addAction('accept');
      expect(getItem('accept').isVisible()).toBe(true);
      addAction('accept', false);
      expect(getItem('accept').isEnabled()).toBe(false);
    });

    it('should show an enabled "accept" action if a branch is selected for a project in review', () => {
      spyOn(ProjectService, 'isAcceptEnabled').and.returnValue(true);
      spyOn(ProjectService, 'isBranch').and.returnValue(true);
      addAction('accept');

      expect(getItem('accept').isVisible()).toBe(true);
      expect(getItem('accept').isEnabled()).toBe(true);
    });

    it('should call the projects accept method when "accept" action is clicked', () => {
      spyOn(ChannelService, 'getId').and.returnValue('test-channel-preview');
      spyOn(ProjectService, 'accept');

      getItem('accept').onClick();

      expect(ProjectService.accept).toHaveBeenCalledWith(['test-channel']);
    });
  });

  describe('reject', () => {
    it('should hide the "reject" action', () => {
      expect(getItem('reject').isVisible()).toBe(false);
    });

    it('shows a disabled "reject" action if a branch is selected for a project not in review', () => {
      addAction('reject');
      expect(getItem('reject').isVisible()).toBe(true);
      addAction('reject', false);
      expect(getItem('reject').isEnabled()).toBe(false);
    });

    it('shows an enabled reject menu item if a branch is selected for a project in review', () => {
      spyOn(ProjectService, 'isBranch').and.returnValue(true);
      spyOn(ProjectService, 'isRejectEnabled').and.returnValue(true);
      addAction('reject');

      expect(getItem('reject').isVisible()).toBe(true);
      expect(getItem('reject').isEnabled()).toBe(true);
    });

    it('shows a prompt on rejection of a channel', () => {
      spyOn(ProjectService, 'reject');

      ChannelService.getChannel.and.returnValue({ id: 'test-channel-preview' });
      DialogService.show.and.returnValue($q.resolve('test-message'));

      getItem('reject').onClick();
      $rootScope.$digest();

      expect(ProjectService.reject).toHaveBeenCalledWith('test-channel', 'test-message');
    });
  });

  describe('delete', () => {
    beforeEach(() => {
      spyOn(ChannelService, 'deleteChannel').and.returnValue($q.resolve());
    });

    it('should hide the "delete" action', () => {
      expect(getItem('delete').isVisible()).toBe(false);
    });

    it('should show the "delete" action', () => {
      addAction('delete');

      expect(getItem('delete').isVisible()).toBe(true);
      expect(getItem('delete').isEnabled()).toBe(true);
    });

    it('should show a disabled "delete" action', () => {
      addAction('delete', false);

      expect(getItem('delete').isVisible()).toBe(true);
      expect(getItem('delete').isEnabled()).toBe(false);
    });

    it('successfully deletes a channel', () => {
      spyOn(CmsService, 'subscribeOnce').and.callThrough();
      spyOn(CmsService, 'publish');

      expectMenuActionAfterComponentEditorIsClosed(
        'delete',
        ChannelService.deleteChannel,
      );

      // make sure the mask was shown
      expect(DialogService.show.calls.mostRecent().args[0].template).toBeDefined();
      expect(CmsService.publish).toHaveBeenCalledWith('channel-deleted');
      expect(CmsService.subscribeOnce).toHaveBeenCalledWith('channel-removed-from-overview', jasmine.any(Function));

      const channelRemovedFromOverviewCallback = CmsService.subscribeOnce.calls.mostRecent().args[1];
      channelRemovedFromOverviewCallback();

      expect(DialogService.hide).toHaveBeenCalled();
    });

    it('only deletes a channel after any open component editor has been closed', () => {
      expectMenuActionAfterComponentEditorIsClosed(
        'delete',
        ChannelService.deleteChannel,
      );
    });

    it('does not delete a channel when canceling the confirmation dialog', () => {
      spyOn(ChannelService, 'getName').and.returnValue('test-channel');
      DialogService.show.and.returnValue($q.reject()); // cancel
      getItem('delete').onClick();
      $rootScope.$digest();

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
      spyOn(DialogService, 'alert').and.returnValue(confirmDialog);

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
      expect($translate.instant).toHaveBeenCalledWith(
        'ERROR_CHANNEL_DELETE_FAILED_DUE_TO_CHILD_MOUNTS', parameterMap,
      );
    });
  });

  describe('close', () => {
    it('should always show an enabled "close" action', () => {
      addAction('close');
      expect(getItem('close').isVisible()).toBe(true);
      expect(getItem('close').isEnabled()).toBe(true);
    });

    it('clears the channel upon closing', () => {
      spyOn(ChannelService, 'clearChannel');

      getItem('close').onClick();
      $rootScope.$apply();

      expect(ChannelService.clearChannel).toHaveBeenCalled();
    });

    it('closes a channel by publishing a close-channel event', () => {
      spyOn(CmsService, 'publish');
      spyOn($state, 'go').and.returnValue($q.resolve());

      getItem('close').onClick();
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
      spyOn(CmsService, 'publish');

      spyOn($state, 'go').and.returnValue($q.reject());

      getItem('close').onClick();
      $rootScope.$apply();

      expect($state.go).toHaveBeenCalledWith('hippo-cm');
      expect(CmsService.publish).not.toHaveBeenCalled();
    });
  });
});
