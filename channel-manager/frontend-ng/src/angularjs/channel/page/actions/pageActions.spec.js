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

describe('PageActions', () => {
  'use strict';

  let $q;
  let $rootScope;
  let $compile;
  let $translate;
  let $scope;
  let FeedbackService;
  let ChannelService;
  let SiteMapService;
  let SiteMapItemService;
  let DialogService;
  let HippoIframeService;
  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _$translate_, _FeedbackService_, _ChannelService_, _SiteMapService_,
            _SiteMapItemService_, _DialogService_, _HippoIframeService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      FeedbackService = _FeedbackService_;
      ChannelService = _ChannelService_;
      SiteMapService = _SiteMapService_;
      SiteMapItemService = _SiteMapItemService_;
      DialogService = _DialogService_;
      HippoIframeService = _HippoIframeService_;
    });

    spyOn($translate, 'instant').and.callFake((key) => {
      if (key.startsWith('TOOLBAR_MENU_PAGES_')) {
        return key.replace(/^TOOLBAR_MENU_PAGES_/, '');
      }

      return key;
    });

    spyOn(FeedbackService, 'showError');
    spyOn(ChannelService, 'hasPrototypes');
    spyOn(ChannelService, 'hasWorkspace');
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(SiteMapService, 'load');
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(false);
    spyOn(SiteMapItemService, 'deleteItem');
    spyOn(SiteMapItemService, 'clear');
    spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
    spyOn(DialogService, 'show');
    spyOn(HippoIframeService, 'load');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onActionSelected = jasmine.createSpy('onActionSelected');
    const $element = angular.element('<page-actions on-action-selected="onActionSelected(subpage)"></page-actions>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('page-actions');
  }

  it('displays a menu with 5 actions', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();

    expect(PageActionsCtrl.actions.length).toBe(5);
    expect(PageActionsCtrl.actions[0].id).toBe('edit');
    expect(PageActionsCtrl.actions[0].label).toBe('EDIT');
    expect(PageActionsCtrl.actions[0].isEnabled()).toBe(false);
    expect(PageActionsCtrl.actions[1].id).toBe('add');
    expect(PageActionsCtrl.actions[2].id).toBe('delete');
    expect(PageActionsCtrl.actions[3].id).toBe('move');
    expect(PageActionsCtrl.actions[4].id).toBe('copy');
  });

  it('calls the passed in callback when selecting an action', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();

    PageActionsCtrl.trigger(PageActionsCtrl.actions[0]);
    expect($scope.onActionSelected).not.toHaveBeenCalled();

    PageActionsCtrl.trigger(PageActionsCtrl.actions[1]);
    expect($scope.onActionSelected).toHaveBeenCalledWith('page-add');
  });

  it('enables the add action if the current channel has both a workspace and prototypes', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const addAction = PageActionsCtrl.actions.find((action) => action.id === 'add');

    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect(addAction.isEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect(addAction.isEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect(addAction.isEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect(addAction.isEnabled()).toBe(true);
  });

  it('enables the delete action if the current page is editable', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find((action) => action.id === 'delete');

    SiteMapItemService.isEditable.and.returnValue(false);
    expect(deleteAction.isEnabled()).toBe(false);

    SiteMapItemService.isEditable.and.returnValue(true);
    expect(deleteAction.isEnabled()).toBe(true);
  });

  it('does nothing when not confirming the deletion of a page', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find((action) => action.id === 'delete');

    DialogService.show.and.returnValue($q.reject());
    PageActionsCtrl.trigger(deleteAction);
    expect(DialogService.confirm).toHaveBeenCalled();
    expect(confirmDialog.title).toHaveBeenCalled();
    expect(confirmDialog.textContent).toHaveBeenCalled();
    expect(confirmDialog.ok).toHaveBeenCalled();
    expect(confirmDialog.cancel).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(confirmDialog);
    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).not.toHaveBeenCalled();
  });

  it('flashes a toast when failing to delete the current page', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find((action) => action.id === 'delete');

    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.reject());
    PageActionsCtrl.trigger(deleteAction);
    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).toHaveBeenCalled();
    $rootScope.$digest();
    expect(HippoIframeService.load).not.toHaveBeenCalled();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_PAGE');
  });

  it('navigates to the channel\'s homepage after successfully deleting the current page', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find((action) => action.id === 'delete');

    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.when());
    PageActionsCtrl.trigger(deleteAction);
    $rootScope.$digest(); // process confirm action
    $rootScope.$digest(); // necessary?
    expect(HippoIframeService.load).toHaveBeenCalledWith('');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(SiteMapItemService.clear).toHaveBeenCalled();
  });
});
