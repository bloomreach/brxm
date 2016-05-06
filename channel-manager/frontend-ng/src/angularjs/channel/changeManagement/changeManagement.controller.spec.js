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

describe('ChangeManagementCtrl', () => {
  let $compile;
  let $q;
  let $rootScope;
  let ChangeManagementCtrl;
  let ChannelService;
  let CmsService;
  let HippoIframeService;
  let HstService;

  beforeEach(() => {
    module('hippo-cm');

    inject((
      _$compile_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _CmsService_,
      _HippoIframeService_,
      _HstService_
    ) => {
      $compile = _$compile_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      HippoIframeService = _HippoIframeService_;
      HstService = _HstService_;
    });

    spyOn(HstService, 'doPost').and.returnValue($q.resolve());
    spyOn(ChannelService, 'resetUserChanges');
    spyOn(ChannelService, 'getChannel').and.returnValue({
      changedBySet: ['testuser', 'otheruser'],
    });

    const el = angular.element(`
      <change-management on-done="">
      </change-management>
    `);
    $compile(el)($rootScope.$new());
    $rootScope.$apply();

    ChangeManagementCtrl = el.controller('change-management');
  });

  it('should get changes by users', () => {
    expect(ChangeManagementCtrl.usersWithChanges).toEqual(['testuser', 'otheruser']);
  });

  it('should start with no selected users', () => {
    expect(ChangeManagementCtrl.selectedUsers).toEqual([]);
  });

  it('should publish selected users changes', () => {
    spyOn(ChangeManagementCtrl, 'resetSelection');

    ChangeManagementCtrl.selectedUsers = ['testuser'];
    ChangeManagementCtrl.publishSelectedChanges();
    $rootScope.$apply();

    expect(HstService.doPost).toHaveBeenCalled();
    expect(ChangeManagementCtrl.resetSelection).toHaveBeenCalled();
  });

  it('should discard selected users changes', () => {
    spyOn(ChangeManagementCtrl, 'resetSelection');

    ChangeManagementCtrl.selectedUsers = ['testuser'];
    ChangeManagementCtrl.discardSelectedChanges();
    $rootScope.$apply();

    expect(HstService.doPost).toHaveBeenCalled();
    expect(ChangeManagementCtrl.resetSelection).toHaveBeenCalled();
  });

  it('should reset changes', () => {
    spyOn(ChangeManagementCtrl, 'onDone');
    spyOn(CmsService, 'publish');
    spyOn(HippoIframeService, 'reload');

    ChangeManagementCtrl.selectedUsers = ['testuser', 'otheruser'];
    ChangeManagementCtrl.resetSelection();

    expect(ChangeManagementCtrl.selectedUsers).toEqual([]);
    expect(ChangeManagementCtrl.onDone).toHaveBeenCalled();
  });

  it('should check a user', () => {
    ChangeManagementCtrl.selectedUsers = [];
    ChangeManagementCtrl.checkUser('testuser');

    expect(ChangeManagementCtrl.selectedUsers).toEqual(['testuser']);
    expect(ChangeManagementCtrl.isChecked('testuser')).toBe(true);
  });

  it('should uncheck a user', () => {
    ChangeManagementCtrl.selectedUsers = ['testuser'];
    ChangeManagementCtrl.uncheckUser('testuser');

    expect(ChangeManagementCtrl.selectedUsers).toEqual([]);
    expect(ChangeManagementCtrl.isChecked('testuser')).toBe(false);
  });

  it('should toggle a user', () => {
    ChangeManagementCtrl.selectedUsers = ['testuser', 'otheruser'];
    ChangeManagementCtrl.toggle('testuser');

    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser']);

    ChangeManagementCtrl.selectedUsers = ['otheruser'];
    ChangeManagementCtrl.toggle('testuser');

    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser', 'testuser']);
  });

  it('should toggle all users', () => {
    ChangeManagementCtrl.usersWithChanges = ['testuser', 'otheruser'];

    ChangeManagementCtrl.selectedUsers = ['testuser', 'otheruser'];
    expect(ChangeManagementCtrl.allAreChecked()).toBe(true);
    ChangeManagementCtrl.toggleAll();
    expect(ChangeManagementCtrl.selectedUsers).toEqual([]);
    expect(ChangeManagementCtrl.allAreChecked()).toBe(false);

    ChangeManagementCtrl.selectedUsers = [];
    expect(ChangeManagementCtrl.allAreChecked()).toBe(false);
    ChangeManagementCtrl.toggleAll();
    expect(ChangeManagementCtrl.selectedUsers).toEqual(['testuser', 'otheruser']);
    expect(ChangeManagementCtrl.allAreChecked()).toBe(true);

    ChangeManagementCtrl.selectedUsers = ['otheruser'];
    expect(ChangeManagementCtrl.allAreChecked()).toBe(false);
    ChangeManagementCtrl.toggleAll();
    expect(ChangeManagementCtrl.selectedUsers).toEqual(['otheruser', 'testuser']);
    expect(ChangeManagementCtrl.allAreChecked()).toBe(true);
  });
});
