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

import angular from 'angular';
import 'angular-mocks';

describe('ChangesMenu', () => {
  let ChannelService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let ConfigService;
  let SessionService;
  let SiteMapService;
  let $q;
  let $rootScope;
  let $compile;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$compile_,
      _$rootScope_,
      _$q_,
      _ChannelService_,
      _DialogService_,
      _FeedbackService_,
      _HippoIframeService_,
      _ConfigService_,
      _SessionService_,
      _SiteMapService_,
    ) => {
      $compile = _$compile_;
      $q = _$q_;
      ChannelService = _ChannelService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      ConfigService = _ConfigService_;
      SiteMapService = _SiteMapService_;
      SessionService = _SessionService_;
      $rootScope = _$rootScope_;
    });

    spyOn(ChannelService, 'publishOwnChanges').and.returnValue($q.resolve());
    spyOn(ChannelService, 'discardOwnChanges').and.returnValue($q.resolve());
    spyOn(ChannelService, 'getChannel').and.returnValue({
      changedBySet: [],
    });
    spyOn(HippoIframeService, 'reload');
    spyOn(DialogService, 'confirm').and.callThrough();
    spyOn(FeedbackService, 'showError');
    spyOn(SessionService, 'canManageChanges').and.returnValue(true);
    spyOn(SiteMapService, 'load');

    ConfigService.cmsUser = 'testUser';
  });

  function createChangesMenuCtrl() {
    const el = angular.element(`
      <changes-menu on-manage-changes="">
      </changes-menu>
    `);
    $compile(el)($rootScope.$new());
    $rootScope.$apply();

    return el.controller('changes-menu');
  }

  it('determines if there are own changes', () => {
    const ChangesMenuCtrl = createChangesMenuCtrl();
    expect(ChangesMenuCtrl.hasOwnChanges()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect(ChangesMenuCtrl.hasOwnChanges()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect(ChangesMenuCtrl.hasOwnChanges()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser', 'testUser'] });
    expect(ChangesMenuCtrl.hasOwnChanges()).toBe(true);

    ChannelService.getChannel.and.returnValue({});
    expect(ChangesMenuCtrl.hasOwnChanges()).toBe(false);
  });

  it('enables the manage changes option when there are changes by other users', () => {
    let ChangesMenuCtrl = createChangesMenuCtrl();
    expect(ChangesMenuCtrl.isManageChangesEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser'] });
    expect(ChangesMenuCtrl.isManageChangesEnabled()).toBe(false);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['otherUser'] });
    expect(ChangesMenuCtrl.isManageChangesEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({ changedBySet: ['testUser', 'otherUser'] });
    expect(ChangesMenuCtrl.isManageChangesEnabled()).toBe(true);

    ChannelService.getChannel.and.returnValue({});
    expect(ChangesMenuCtrl.isManageChangesEnabled()).toBe(false);

    SessionService.canManageChanges.and.returnValue(false);
    ChangesMenuCtrl = createChangesMenuCtrl();
    expect(ChangesMenuCtrl.isManageChangesEnabled()).toBe(false);
  });

  it('publishes changes', () => {
    const ChangesMenuCtrl = createChangesMenuCtrl();
    ChangesMenuCtrl.publish();
    $rootScope.$digest();

    expect(ChannelService.publishOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('flashes a toast when publication failed', () => {
    const ChangesMenuCtrl = createChangesMenuCtrl();

    const params = { };
    ChannelService.publishOwnChanges.and.returnValue($q.reject({ data: params }));
    ChangesMenuCtrl.publish();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', params);

    ChannelService.publishOwnChanges.and.returnValue($q.reject());
    ChangesMenuCtrl.publish();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', undefined);
  });

  it('discards changes', () => {
    spyOn(DialogService, 'show').and.returnValue($q.resolve());

    const ChangesMenuCtrl = createChangesMenuCtrl();
    ChangesMenuCtrl.discard();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('flashes a toast when discarding failed', () => {
    const ChangesMenuCtrl = createChangesMenuCtrl();
    spyOn(DialogService, 'show').and.returnValue($q.resolve());

    const params = { };
    ChannelService.discardOwnChanges.and.returnValue($q.reject({ data: params }));
    ChangesMenuCtrl.discard();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', params);

    ChannelService.discardOwnChanges.and.returnValue($q.reject());
    ChangesMenuCtrl.discard();
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', undefined);
  });

  it('does not discard changes if not confirmed', () => {
    spyOn(DialogService, 'show').and.returnValue($q.reject());

    const ChangesMenuCtrl = createChangesMenuCtrl();
    ChangesMenuCtrl.discard();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).not.toHaveBeenCalled();
  });
});
