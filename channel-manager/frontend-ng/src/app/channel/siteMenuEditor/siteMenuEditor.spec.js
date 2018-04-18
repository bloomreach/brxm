/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('SiteMenuEditor', () => {
  let $compile;
  let $element;
  let $q;
  let $rootScope;
  let $scope;

  let ChannelService;
  let ConfigService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let SiteMenuService;

  let $ctrl;
  let menu;
  const dialog = jasmine.createSpyObj('dialog', ['textContent', 'ok', 'cancel']);
  dialog.textContent.and.returnValue(dialog);
  dialog.ok.and.returnValue(dialog);
  dialog.cancel.and.returnValue(dialog);

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.siteMenuEditor');

    inject((
      _$compile_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _ConfigService_,
      _DialogService_,
      _FeedbackService_,
      _HippoIframeService_,
      _SiteMenuService_,
    ) => {
      $compile = _$compile_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SiteMenuService = _SiteMenuService_;
    });

    menu = { items: [] };

    spyOn(SiteMenuService, 'deleteMenuItem').and.returnValue($q.when());
    spyOn(SiteMenuService, 'getEditableMenuItem').and.callFake(id => $q.when({ id }));
    spyOn(SiteMenuService, 'loadMenu').and.returnValue($q.when(menu));
    spyOn(SiteMenuService, 'saveMenuItem').and.returnValue($q.when());
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(ChannelService, 'reload');
    spyOn(DialogService, 'confirm').and.returnValue(dialog);
    spyOn(DialogService, 'show').and.returnValue($q.when());
    spyOn(FeedbackService, 'showErrorResponse');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $scope.onError = jasmine.createSpy('onError');
    $scope.menuUuid = 'testUuid';
    $element = angular.element('<site-menu-editor menu-uuid="{{menuUuid}}" on-done="onDone()" on-error="onError(key)"> </site-menu-editor>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('siteMenuEditor');
  }

  it('initializes correctly', () => {
    $ctrl = compileDirectiveAndGetController();

    expect($ctrl.menuUuid).toBe('testUuid');
    expect($ctrl.items).toBe(menu.items);
  });

  it('returns to the main page when it fails to load the menu', () => {
    SiteMenuService.loadMenu.and.returnValue($q.reject());
    compileDirectiveAndGetController();

    expect($scope.onError).toHaveBeenCalledWith('ERROR_MENU_LOAD_FAILED');
  });

  it('returns to the page when clicking the "back" button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('checks the locking status of the loaded menu', () => {
    ConfigService.cmsUser = 'testUser';

    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isLockedByOther()).toBeFalsy();

    menu.lockedBy = 'testUser';
    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isLockedByOther()).toBe(false);

    menu.lockedBy = 'anotherUser';
    $ctrl = compileDirectiveAndGetController();
    expect($ctrl.isLockedByOther()).toBe(true);
  });

  describe('SiteMenuEditorCtrl', () => {
    beforeEach(() => {
      $ctrl = compileDirectiveAndGetController();
      $ctrl.editingItem = {
        id: 1,
        localParameters: {},
      };
    });

    describe('_startEditingItem', () => {
      it('sets the item to be the editing item', () => {
        const item = {
          id: 12,
        };
        $ctrl._startEditingItem(item);
        expect($ctrl.editingItem).toBe(item);
      });
    });

    describe('stopEditingItem', () => {
      it('stops editing an item', () => {
        $ctrl.editingItem = {
          id: 12,
        };
        $ctrl.stopEditingItem();
        expect($ctrl.editingItem).toBe(null);
      });
    });

    describe('addItem', () => {
      it('should add an item', () => {
        spyOn(SiteMenuService, 'createEditableMenuItem').and.returnValue($q.when({ id: 15 }));
        spyOn($ctrl, '_startEditingItem');

        $ctrl.menuUuid = 33;
        $ctrl.editingItem = {
          id: 12,
        };
        $ctrl.addItem();
        expect($ctrl.isSaving.newItem).toBe(true);
        expect(SiteMenuService.createEditableMenuItem).toHaveBeenCalled();
        $rootScope.$apply();
        expect($ctrl.isSaving.newItem).toBeFalsy();
        expect($ctrl._startEditingItem).toHaveBeenCalledWith({
          id: 15,
        });
      });

      it('should fail when adding an item', () => {
        const response = { key: 'value' };
        spyOn(SiteMenuService, 'createEditableMenuItem').and.returnValue($q.reject(response));

        $ctrl.addItem();
        $rootScope.$apply();
        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(response, 'ERROR_MENU_CREATE_FAILED', $ctrl.errorMap);
      });

      it('flashes a catch-all toast when the add request is rejected for no specific reason', () => {
        spyOn(SiteMenuService, 'createEditableMenuItem').and.returnValue($q.reject());

        $ctrl.addItem();
        $rootScope.$digest();
        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(undefined, 'ERROR_MENU_CREATE_FAILED', $ctrl.errorMap);
      });
    });

    describe('toggleEditState', () => {
      it('calls the appropriate function after checking if an item is already being edited', () => {
        SiteMenuService.getEditableMenuItem.and.returnValue($q.when({ id: 15 }));
        spyOn($ctrl, '_startEditingItem');

        $ctrl.editingItem = { id: 12 };
        $ctrl.toggleEditState({ id: 55 });
        expect(SiteMenuService.getEditableMenuItem).toHaveBeenCalledWith(55);
        $rootScope.$apply();
        expect($ctrl._startEditingItem).toHaveBeenCalledWith({ id: 15 });
      });

      it('calls stops editing an item if an item is already being edited', () => {
        spyOn($ctrl, 'stopEditingItem');
        $ctrl.editingItem = { id: 12 };
        $ctrl.toggleEditState({ id: 12 });
        expect($ctrl.stopEditingItem).toHaveBeenCalled();
      });
    });

    describe('onBack', () => {
      it('returns to the main page with no changes', () => {
        spyOn(HippoIframeService, 'reload');
        spyOn($ctrl, 'onDone');

        $ctrl.onBack();
        expect(HippoIframeService.reload).not.toHaveBeenCalled();
        expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
        expect($ctrl.onDone).toHaveBeenCalled();
      });

      it('returns to the mainpage with changes', () => {
        $ctrl.saveItem();
        $rootScope.$apply();

        spyOn(HippoIframeService, 'reload');
        spyOn($ctrl, 'onDone');

        $ctrl.onBack();
        expect(HippoIframeService.reload).toHaveBeenCalled();
        expect(ChannelService.recordOwnChange).toHaveBeenCalled();
        expect($ctrl.onDone).toHaveBeenCalled();
      });
    });

    describe('saveItem', () => {
      it('saves and closes the menu item open for editing', () => {
        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // save the selected item
        $ctrl.saveItem();
        expect(SiteMenuService.saveMenuItem).toHaveBeenCalledWith($ctrl.editingItem);
        $rootScope.$digest();
        expect($ctrl.editingItem).toBeNull();

        // update channel when leaving the subpage after modification
        $ctrl.onBack();
        expect(ChannelService.recordOwnChange).toHaveBeenCalled();
      });

      it('flashes a toast if the item name already exists', () => {
        const response = { key: 'value' };
        SiteMenuService.saveMenuItem.and.returnValue($q.reject(response));

        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // save the selected item
        $ctrl.saveItem();
        $rootScope.$digest();

        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(response, 'ERROR_MENU_ITEM_SAVE_FAILED', $ctrl.errorMap);
        expect($ctrl.editingItem).not.toBeNull();
      });

      it('flashes a catch-all toast when the save request is rejected without a specific reason', () => {
        SiteMenuService.saveMenuItem.and.returnValue($q.reject());

        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // save the selected item
        $ctrl.saveItem();
        $rootScope.$digest();

        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(undefined, 'ERROR_MENU_ITEM_SAVE_FAILED', $ctrl.errorMap);
      });
    });

    describe('deleteItem', () => {
      it('deletes the item open for editing', () => {
        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // now request deletion
        $ctrl.deleteItem();
        expect(DialogService.confirm).toHaveBeenCalled();
        expect(DialogService.show).toHaveBeenCalled();

        // confirm deletion
        $rootScope.$digest();
        expect(SiteMenuService.deleteMenuItem).toHaveBeenCalledWith('clickedId');
        // backend reports successful deletion
        expect($ctrl.editingItem).toBeNull();

        // update channel when leaving the subpage after modification
        $ctrl.onBack();
        expect(ChannelService.recordOwnChange).toHaveBeenCalled();
      });

      it('doesn\'t delete the item is deletion is not confirmed', () => {
        DialogService.show.and.returnValue($q.reject());

        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // now request deletion
        $ctrl.deleteItem();
        expect(DialogService.show).toHaveBeenCalled();

        // cancel deletion
        $rootScope.$digest();
        expect(SiteMenuService.deleteMenuItem).not.toHaveBeenCalled();
      });

      it('reloads the menu and channel when the deletion request is rejected because the menu is locked', () => {
        const response = { errorCode: 'ITEM_ALREADY_LOCKED', data: { lockedBy: 'tester' } };
        SiteMenuService.deleteMenuItem.and.returnValue($q.reject(response));
        SiteMenuService.loadMenu.calls.reset();

        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // request and confirm deletion
        $ctrl.deleteItem();
        $rootScope.$digest();

        expect(SiteMenuService.loadMenu).toHaveBeenCalledWith('testUuid');
        expect(ChannelService.reload).toHaveBeenCalled();
        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(response, 'ERROR_MENU_ITEM_DELETE_FAILED', $ctrl.errorMap);
      });

      it('flashes a catch-all toast when the deletion request is rejected for another reason', () => {
        const response = { errorCode: 'ANOTHER_REASON', data: { key: 'value' } };
        SiteMenuService.deleteMenuItem.and.returnValue($q.reject(response));

        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // request and confirm deletion
        $ctrl.deleteItem();
        $rootScope.$digest();

        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(response, 'ERROR_MENU_ITEM_DELETE_FAILED', $ctrl.errorMap);
      });

      it('flashes a catch-all toast when the deletion request is rejected without a specific reason', () => {
        SiteMenuService.deleteMenuItem.and.returnValue($q.reject());

        // select the item to be deleted
        $ctrl.toggleEditState({ id: 'clickedId' });
        $rootScope.$digest();

        // request and confirm deletion
        $ctrl.deleteItem();
        $rootScope.$digest();

        expect(FeedbackService.showErrorResponse)
          .toHaveBeenCalledWith(undefined, 'ERROR_MENU_ITEM_DELETE_FAILED', $ctrl.errorMap);
      });
    });

    describe('hasLocalParameters', () => {
      it('returns false if there are not local parameters', () => {
        expect($ctrl.hasLocalParameters()).toBe(false);
      });
      it('returns true if there are local parameters', () => {
        $ctrl.editingItem.localParameters = {
          test: 1,
        };
        expect($ctrl.hasLocalParameters()).toBe(true);
      });
    });
  });
});
