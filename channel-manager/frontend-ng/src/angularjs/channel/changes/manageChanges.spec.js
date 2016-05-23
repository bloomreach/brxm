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
      // Out of alphabetical order
      changedBySet: ['testuser', 'otheruser'],
    });
    spyOn(ChannelService, 'publishChanges').and.returnValue($q.when());
    spyOn(ChannelService, 'discardChanges').and.returnValue($q.when());
    spyOn(CmsService, 'publish');
    spyOn(DialogService, 'confirm').and.returnValue(dialog);
    spyOn(DialogService, 'show').and.returnValue($q.when());
    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'reload');

    ConfigService.cmsUser = 'testuser';

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
    // alphabetical sorting!
    expect(ChangeManagementCtrl.usersWithChanges).toEqual(['otheruser', 'testuser']);
  });

  it('should publish all users changes on confirm', () => {
    ChangeManagementCtrl.publishAllChanges();
    $rootScope.$apply();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(dialog);
    expect(ChannelService.publishChanges).toHaveBeenCalled();
    expect(CmsService.publish).toHaveBeenCalledWith('channel-changed-in-angular');
    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('should not publish all users changes on cancel', () => {
    DialogService.show.and.returnValue($q.reject());

    ChangeManagementCtrl.discardAllChanges();
    $rootScope.$apply();

    expect(ChannelService.publishChanges).not.toHaveBeenCalled();
    expect($scope.onDone).not.toHaveBeenCalled();
  });

  it('should discard all users changes on confirm', () => {
    ChangeManagementCtrl.discardAllChanges();
    $rootScope.$apply();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(dialog);
    expect(ChannelService.discardChanges).toHaveBeenCalled();
    expect(CmsService.publish).toHaveBeenCalledWith('channel-changed-in-angular');
    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('should not discard selected users changes on cancel', () => {
    DialogService.show.and.returnValue($q.reject());

    ChangeManagementCtrl.discardAllChanges();
    $rootScope.$apply();

    expect(ChannelService.discardChanges).not.toHaveBeenCalled();
    expect($scope.onDone).not.toHaveBeenCalled();
  });

  it('should publish one users changes', () => {
    ChangeManagementCtrl.publishChanges('testuser');

    $rootScope.$apply();

    expect(ChannelService.publishChanges).toHaveBeenCalled();
  });

  it('should discard one users changes', () => {
    ChangeManagementCtrl.publishChanges('testuser');

    $rootScope.$apply();

    expect(ChannelService.publishChanges).toHaveBeenCalled();
  });

  it('should show a toast when publication fails', () => {
    const params = { };
    ChannelService.publishChanges.and.returnValue($q.reject({ data: params }));

    ChangeManagementCtrl.publishAllChanges();
    $rootScope.$apply();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', params,
                                                           ChangeManagementCtrl.feedbackParent);
    expect($scope.onDone).not.toHaveBeenCalled();

    ChannelService.publishChanges.and.returnValue($q.reject());

    ChangeManagementCtrl.publishAllChanges();
    $rootScope.$apply();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_PUBLICATION_FAILED', undefined,
                                                           ChangeManagementCtrl.feedbackParent);
  });

  it('should show a toast when discarding of the changes fails', () => {
    const params = { };
    ChannelService.discardChanges.and.returnValue($q.reject({ data: params }));

    ChangeManagementCtrl.discardAllChanges();
    $rootScope.$apply();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', params,
                                                           ChangeManagementCtrl.feedbackParent);
    expect($scope.onDone).not.toHaveBeenCalled();

    ChannelService.discardChanges.and.returnValue($q.reject());

    ChangeManagementCtrl.discardAllChanges();
    $rootScope.$apply();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANGE_DISCARD_FAILED', undefined,
                                                           ChangeManagementCtrl.feedbackParent);
  });

  it('should add a suffix to the current user\'s label', () => {
    const label = ChangeManagementCtrl.getLabel('testuser');
    expect(label.startsWith('testuser')).toBeTruthy();
    expect(label).not.toBe('testuser');

    expect(ChangeManagementCtrl.getLabel('otheruser')).toBe('otheruser');
  });
});
