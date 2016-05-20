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

describe('ChangeManagement', () => {
  let $compile;
  let $q;
  let $rootScope;
  let $scope;
  let ChangeManagementCtrl;
  let ChannelService;
  let CmsService;
  let ConfigService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;

  const dialog = jasmine.createSpyObj('dialog', ['title', 'textContent', 'ok', 'cancel']);
  dialog.title.and.returnValue(dialog);
  dialog.textContent.and.returnValue(dialog);
  dialog.ok.and.returnValue(dialog);
  dialog.cancel.and.returnValue(dialog);

  beforeEach(() => {
    module('hippo-cm');

    inject((
      _$compile_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _CmsService_,
      _ConfigService_,
      _DialogService_,
      _FeedbackService_,
      _HippoIframeService_
    ) => {
      $compile = _$compile_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      ConfigService = _ConfigService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
    });

    spyOn(ChannelService, 'getChannel').and.returnValue({
      changedBySet: ['testuser', 'otheruser'],
    });
    spyOn(ChannelService, 'publishChanges').and.returnValue($q.when());
    spyOn(ChannelService, 'discardChanges').and.returnValue($q.when());
    spyOn(CmsService, 'publish');
    ConfigService.cmsUser = 'testuser';
    spyOn(DialogService, 'confirm').and.returnValue(dialog);
    spyOn(DialogService, 'show').and.returnValue($q.when());
    spyOn(FeedbackService, 'showErrorOnSubpage');
    spyOn(HippoIframeService, 'reload');

    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    const el = angular.element(`
      <change-management on-done="onDone()">
      </change-management>
    `);
    $compile(el)($scope);
    $rootScope.$apply();

    ChangeManagementCtrl = el.controller('change-management');
  });

  it('should initialize correctly', () => {
    expect(ChangeManagementCtrl.isNoneSelected()).toBe(true);
    expect(ChangeManagementCtrl.usersWithChanges).toEqual(['otheruser', 'testuser']); // alphabetical sorting!
  });

  it('should publish selected users changes', () => {
    ChangeManagementCtrl.toggle('testuser');
    ChangeManagementCtrl.publishSelectedChanges();
    expect(ChannelService.publishChanges).toHaveBeenCalled();
    $rootScope.$apply();

    expect(CmsService.publish).toHaveBeenCalledWith('channel-changed-in-angular');
    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('should flash a toast when publication fails', () => {
    const params = { };
    ChannelService.publishChanges.and.returnValue($q.reject({ data: params }));
    ChangeManagementCtrl.toggle('testuser');
    ChangeManagementCtrl.publishSelectedChanges();
    $rootScope.$apply();

    expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', params);
    expect($scope.onDone).not.toHaveBeenCalled();

    ChannelService.publishChanges.and.returnValue($q.reject());
    ChangeManagementCtrl.publishSelectedChanges();
    $rootScope.$apply();
    expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', undefined);
  });

  it('should discard selected users changes on confirm', () => {
    ChangeManagementCtrl.toggle('testuser');
    ChangeManagementCtrl.discardSelectedChanges();
    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(dialog);
    $rootScope.$apply();

    expect(ChannelService.discardChanges).toHaveBeenCalledWith(['testuser']);
    expect(CmsService.publish).toHaveBeenCalledWith('channel-changed-in-angular');
    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('should not discard selected users changes on cancel', () => {
    DialogService.show.and.returnValue($q.reject());
    ChangeManagementCtrl.toggle('testuser');
    ChangeManagementCtrl.discardSelectedChanges();
    $rootScope.$apply();

    expect(ChannelService.discardChanges).not.toHaveBeenCalled();
    expect($scope.onDone).not.toHaveBeenCalled();
  });

  it('should flash a toast when discarding of the selected changes fails', () => {
    const params = { };
    ChannelService.discardChanges.and.returnValue($q.reject({ data: params }));

    ChangeManagementCtrl.toggle('testuser');
    ChangeManagementCtrl.discardSelectedChanges();
    $rootScope.$apply();

    expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', params);
    expect($scope.onDone).not.toHaveBeenCalled();

    ChannelService.discardChanges.and.returnValue($q.reject());
    ChangeManagementCtrl.discardSelectedChanges();
    $rootScope.$apply();
    expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', undefined);
  });

  it('should toggle a user', () => {
    ChangeManagementCtrl.selectedUsers = ['testuser', 'otheruser'];
    ChangeManagementCtrl.toggle('testuser');

    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser']);

    ChangeManagementCtrl.selectedUsers = ['otheruser'];
    ChangeManagementCtrl.toggle('testuser');

    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser', 'testuser']);
  });

  it('should select and deselect all users', () => {
    expect(ChangeManagementCtrl.isNoneSelected()).toBe(true);

    ChangeManagementCtrl.toggle('testuser');
    expect(ChangeManagementCtrl.isNoneSelected()).toBe(false);
    expect(ChangeManagementCtrl.selectedUsers).toEqual(['testuser']);

    ChangeManagementCtrl.selectAll();
    expect(ChangeManagementCtrl.selectedUsers).toEqual(['testuser', 'otheruser']);

    ChangeManagementCtrl.selectNone();
    expect(ChangeManagementCtrl.selectedUsers).toEqual([]);

    ChangeManagementCtrl.selectAll();
    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser', 'testuser']);

    ChangeManagementCtrl.toggle('testuser');
    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser']);

    ChangeManagementCtrl.selectNone();
    expect(ChangeManagementCtrl.selectedUsers).toEqual([]);
  });

  it('should add a suffix to the current user\'s label', () => {
    const label = ChangeManagementCtrl.getLabel('testuser');
    expect(label.startsWith('testuser')).toBeTruthy();
    expect(label).not.toBe('testuser');

    expect(ChangeManagementCtrl.getLabel('otheruser')).toBe('otheruser');
  });
});
