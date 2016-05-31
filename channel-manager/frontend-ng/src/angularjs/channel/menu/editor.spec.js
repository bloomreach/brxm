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

/* eslint-disable prefer-const */

describe('MenuEditor', () => {
  'use strict';

  let $q;
  let $element;
  let $scope;
  let $rootScope;
  let $compile;
  let SiteMenuService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let ChannelService;
  let menu;
  let MenuEditorCtrl;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _SiteMenuService_, _DialogService_, _FeedbackService_, _HippoIframeService_,
            _ChannelService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      SiteMenuService = _SiteMenuService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      ChannelService = _ChannelService_;
    });

    menu = { items: [] };

    spyOn(SiteMenuService, 'loadMenu').and.returnValue($q.when(menu));
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $scope.onError = jasmine.createSpy('onError');
    $scope.menuUuid = 'testUuid';
    $element = angular.element('<menu-editor menu-uuid="{{menuUuid}}" on-done="onDone()" on-error="onError(key)"> </menu-editor>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('menu-editor');
  }

  it('initializes correctly', () => {
    MenuEditorCtrl = compileDirectiveAndGetController();

    expect(MenuEditorCtrl.menuUuid).toBe('testUuid');
    expect(MenuEditorCtrl.items).toBe(menu.items);
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

  describe('MenuEditorCtrl', () => {
    beforeEach(() => {
      MenuEditorCtrl = compileDirectiveAndGetController();
      MenuEditorCtrl.editingItem = {
        id: 1,
        localParameters: {},
      };
    });

    describe('_startEditingItem', () => {
      it('sets the item to be the editing item', () => {
        const item = {
          id: 12,
        };
        MenuEditorCtrl._startEditingItem(item);
        expect(MenuEditorCtrl.editingItem).toBe(item);
      });
    });

    describe('stopEditingItem', () => {
      it('stops editing an item', () => {
        MenuEditorCtrl.editingItem = {
          id: 12,
        };
        MenuEditorCtrl.stopEditingItem();
        expect(MenuEditorCtrl.editingItem).toBe(null);
      });
    });

    describe('addItem', () => {
      it('should add an item', () => {
        spyOn(SiteMenuService, 'createEditableMenuItem').and.returnValue($q.when({ id: 15 }));
        spyOn(MenuEditorCtrl, '_startEditingItem');

        MenuEditorCtrl.menuUuid = 33;
        MenuEditorCtrl.editingItem = {
          id: 12,
        };
        MenuEditorCtrl.addItem();
        expect(MenuEditorCtrl.isSaving.newItem).toBe(true);
        expect(SiteMenuService.createEditableMenuItem).toHaveBeenCalled();
        $rootScope.$apply();
        expect(MenuEditorCtrl.isSaving.newItem).toBeFalsy();
        expect(MenuEditorCtrl._startEditingItem).toHaveBeenCalledWith({
          id: 15,
        });
      });

      it('should fail when adding an item', () => {
        const data = { key: 'value' };
        const error = { data };
        spyOn(SiteMenuService, 'createEditableMenuItem').and.returnValue($q.reject(error));
        spyOn(MenuEditorCtrl, 'onError');

        MenuEditorCtrl.menuUuid = 77;

        MenuEditorCtrl.addItem();
        $rootScope.$apply();
        expect(MenuEditorCtrl.onError).toHaveBeenCalledWith({
          key: 'ERROR_MENU_CREATE_FAILED',
          params: data,
        });
      });
    });

    describe('toggleEditState', () => {
      it('calls the appropriate function after checking if an item is already being edited', () => {
        spyOn(SiteMenuService, 'getEditableMenuItem').and.returnValue($q.when({ id: 15 }));
        spyOn(MenuEditorCtrl, '_startEditingItem');

        MenuEditorCtrl.editingItem = { id: 12 };
        MenuEditorCtrl.toggleEditState({ id: 55 });
        expect(SiteMenuService.getEditableMenuItem).toHaveBeenCalledWith(55);
        $rootScope.$apply();
        expect(MenuEditorCtrl._startEditingItem).toHaveBeenCalledWith({ id: 15 });
      });

      it('calls stops editing an item if an item is already being edited', () => {
        spyOn(MenuEditorCtrl, 'stopEditingItem');
        MenuEditorCtrl.editingItem = { id: 12 };
        MenuEditorCtrl.toggleEditState({ id: 12 });
        expect(MenuEditorCtrl.stopEditingItem).toHaveBeenCalled();
      });
    });

    describe('onBack', () => {
      it('returns to the main page with no changes', () => {
        spyOn(HippoIframeService, 'reload');
        spyOn(ChannelService, 'recordOwnChange');
        spyOn(MenuEditorCtrl, 'onDone');

        MenuEditorCtrl.onBack();
        expect(HippoIframeService.reload).not.toHaveBeenCalled();
        expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
        expect(MenuEditorCtrl.onDone).toHaveBeenCalled();
      });

      it('returns to the mainpage with changes', () => {
        spyOn(SiteMenuService, 'saveMenuItem').and.returnValue($q.when());
        MenuEditorCtrl.saveItem();
        $rootScope.$apply();

        spyOn(HippoIframeService, 'reload');
        spyOn(ChannelService, 'recordOwnChange');
        spyOn(MenuEditorCtrl, 'onDone');

        MenuEditorCtrl.onBack();
        expect(HippoIframeService.reload).toHaveBeenCalled();
        expect(ChannelService.recordOwnChange).toHaveBeenCalled();
        expect(MenuEditorCtrl.onDone).toHaveBeenCalled();
      });
    });

    describe('saveItem', () => {
      it('calls SiteMenuService.saveMenuItem and then stops editing item', () => {
        spyOn(SiteMenuService, 'saveMenuItem').and.returnValue($q.when());
        spyOn(MenuEditorCtrl, 'stopEditingItem');

        MenuEditorCtrl.saveItem();
        expect(SiteMenuService.saveMenuItem).toHaveBeenCalledWith(MenuEditorCtrl.editingItem);
        $rootScope.$apply();
        expect(MenuEditorCtrl.stopEditingItem).toHaveBeenCalled();
      });

      it('calls SiteMenuService.saveMenuItem and then catches itself if it fails', () => {
        const data = { key: 'value' };
        const error = { data };
        spyOn(SiteMenuService, 'saveMenuItem').and.returnValue($q.reject(error));
        spyOn(FeedbackService, 'showErrorOnSubpage');

        MenuEditorCtrl.saveItem();
        expect(SiteMenuService.saveMenuItem).toHaveBeenCalledWith(MenuEditorCtrl.editingItem);
        $rootScope.$apply();
        expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_MENU_ITEM_SAVE_FAILED', data);
      });
    });

    describe('_doDelete', () => {
      it('calls SiteMenuService.deleteMenuItem and then stops editing item', () => {
        spyOn(SiteMenuService, 'deleteMenuItem').and.returnValue($q.when());
        spyOn(MenuEditorCtrl, 'stopEditingItem');

        MenuEditorCtrl._doDelete();
        expect(SiteMenuService.deleteMenuItem).toHaveBeenCalledWith(1);
        $rootScope.$apply();
        expect(MenuEditorCtrl.stopEditingItem).toHaveBeenCalled();
      });

      it('calls SiteMenuService.deleteMenuItem and then catches itself if it fails', () => {
        const data = { key: 'value' };
        const error = { data };
        spyOn(SiteMenuService, 'deleteMenuItem').and.returnValue($q.reject(error));
        spyOn(FeedbackService, 'showErrorOnSubpage');

        MenuEditorCtrl._doDelete();
        expect(SiteMenuService.deleteMenuItem).toHaveBeenCalledWith(1);
        $rootScope.$apply();
        expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_MENU_ITEM_DELETE_FAILED', data);
      });
    });

    describe('_confirmDelete', () => {
      it('calls the dialog service', () => {
        spyOn(DialogService, 'confirm').and.callThrough();
        spyOn(DialogService, 'show');

        MenuEditorCtrl._confirmDelete();
        expect(DialogService.confirm).toHaveBeenCalled();
        $rootScope.$apply();
        expect(DialogService.show).toHaveBeenCalled();
      });
    });

    describe('deleteItem', () => {
      it('calls _confirmDelete and then deletes the item', () => {
        spyOn(MenuEditorCtrl, '_confirmDelete').and.returnValue($q.when());
        spyOn(MenuEditorCtrl, '_doDelete');

        MenuEditorCtrl.deleteItem();
        expect(MenuEditorCtrl._confirmDelete).toHaveBeenCalled();
        $rootScope.$apply();
        expect(MenuEditorCtrl._doDelete).toHaveBeenCalled();
      });
    });

    describe('hasLocalParameters', () => {
      it('returns false if there are not local parameters', () => {
        expect(MenuEditorCtrl.hasLocalParameters()).toBe(false);
      });
      it('returns true if there are local parameters', () => {
        MenuEditorCtrl.editingItem.localParameters = {
          test: 1,
        };
        expect(MenuEditorCtrl.hasLocalParameters()).toBe(true);
      });
    });
  });
});
