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

describe('PageActions', () => {
  let $q;
  let $rootScope;
  let $compile;
  let $translate;
  let $scope;
  let FeedbackService;
  let ChannelService;
  let SessionService;
  let SiteMapService;
  let SiteMapItemService;
  let DialogService;
  let HippoIframeService;
  let PageMetaDataService;
  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _$compile_, _$translate_, _FeedbackService_, _ChannelService_, _SiteMapService_,
            _SiteMapItemService_, _DialogService_, _HippoIframeService_, _PageMetaDataService_, _SessionService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      FeedbackService = _FeedbackService_;
      ChannelService = _ChannelService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
      SiteMapItemService = _SiteMapItemService_;
      DialogService = _DialogService_;
      HippoIframeService = _HippoIframeService_;
      PageMetaDataService = _PageMetaDataService_;
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
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(ChannelService, 'loadPageModifiableChannels');
    spyOn(ChannelService, 'getPageModifiableChannels');
    spyOn(SessionService, 'isCrossChannelPageCopySupported').and.returnValue(true);
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(SiteMapService, 'load');
    spyOn(SiteMapItemService, 'get').and.returnValue({ name: 'name' });
    spyOn(SiteMapItemService, 'hasItem').and.returnValue(true);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(false);
    spyOn(SiteMapItemService, 'isLocked').and.returnValue(false);
    spyOn(SiteMapItemService, 'deleteItem');
    spyOn(SiteMapItemService, 'clear');
    spyOn(SiteMapItemService, 'loadAndCache');
    spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
    spyOn(DialogService, 'show');
    spyOn(HippoIframeService, 'load');
    spyOn(PageMetaDataService, 'getSiteMapItemId').and.returnValue('siteMapItemId');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onActionSelected = jasmine.createSpy('onActionSelected');
    const $element = angular.element('<page-actions on-action-selected="onActionSelected(subpage)"></page-actions>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('pageActions');
  }

  it('displays a menu with 5 actions', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();

    expect(PageActionsCtrl.actions.length).toBe(4);
    expect(PageActionsCtrl.actions[0].id).toBe('edit');
    expect(PageActionsCtrl.actions[1].id).toBe('copy');
    expect(PageActionsCtrl.actions[2].id).toBe('move');
    expect(PageActionsCtrl.actions[3].id).toBe('delete');

    expect(PageActionsCtrl.createAction.id).toBe('create');
  });

  it('calls the passed in callback when selecting an action', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();

    PageActionsCtrl.trigger(PageActionsCtrl.actions[0]);
    expect($scope.onActionSelected).toHaveBeenCalledWith('page-edit');

    PageActionsCtrl.trigger(PageActionsCtrl.actions[1]);
    expect($scope.onActionSelected).toHaveBeenCalledWith('page-copy');
  });

  it('enables the create action if the current channel has both a workspace and prototypes', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const createAction = PageActionsCtrl.createAction;

    $translate.instant.calls.reset();
    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect(createAction.isEnabled()).toBe(false);
    expect(PageActionsCtrl.getSitemapNotEditableMarker()).not.toBe('');
    expect($translate.instant).toHaveBeenCalledWith('TOOLBAR_MENU_PAGE_SITEMAP_NOT_EDITABLE');

    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect(createAction.isEnabled()).toBe(false);
    expect(PageActionsCtrl.getSitemapNotEditableMarker()).not.toBe('');

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect(createAction.isEnabled()).toBe(false);
    expect(PageActionsCtrl.getSitemapNotEditableMarker()).not.toBe('');

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect(createAction.isEnabled()).toBe(true);
    expect(PageActionsCtrl.getSitemapNotEditableMarker()).toBe('');
  });

  it('enables the delete action if the current page is editable', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find(action => action.id === 'delete');

    SiteMapItemService.isEditable.and.returnValue(false);
    expect(deleteAction.isEnabled()).toBe(false);
    expect(PageActionsCtrl.getPageNotEditableMarker(deleteAction)).toBe('');

    SiteMapItemService.isEditable.and.returnValue(true);
    expect(deleteAction.isEnabled()).toBe(true);
    expect(PageActionsCtrl.getPageNotEditableMarker(deleteAction)).toBe('');
  });

  it('enables the edit action if the current page is editable', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const editAction = PageActionsCtrl.actions.find(action => action.id === 'edit');

    $translate.instant.calls.reset();
    SiteMapItemService.isEditable.and.returnValue(false);
    expect(editAction.isEnabled()).toBe(false);
    expect(PageActionsCtrl.getPageNotEditableMarker(editAction)).not.toBe('');
    expect($translate.instant).toHaveBeenCalledWith('TOOLBAR_MENU_PAGE_PAGE_NOT_EDITABLE');

    SiteMapItemService.isEditable.and.returnValue(true);
    expect(editAction.isEnabled()).toBe(true);
    expect(PageActionsCtrl.getPageNotEditableMarker(editAction)).toBe('');
  });

  it('enables the copy action if the page can be copied', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const copyAction = PageActionsCtrl.actions.find(action => action.id === 'copy');

    SiteMapItemService.isLocked.and.returnValue(true);
    expect(copyAction.isEnabled()).toBe(false);

    SiteMapItemService.isLocked.and.returnValue(false);
    ChannelService.hasWorkspace.and.returnValue(true);
    expect(copyAction.isEnabled()).toBe(true);

    ChannelService.hasWorkspace.and.returnValue(false);
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    expect(copyAction.isEnabled()).toBe(false);

    SessionService.isCrossChannelPageCopySupported.and.returnValue(true);
    ChannelService.getPageModifiableChannels.and.returnValue(undefined);
    expect(copyAction.isEnabled()).toBe(false);

    ChannelService.getPageModifiableChannels.and.returnValue([]);
    expect(copyAction.isEnabled()).toBe(false);

    ChannelService.getPageModifiableChannels.and.returnValue(['dummy']);
    expect(copyAction.isEnabled()).toBe(true);
  });

  it('disables the copy action if the page is undefined', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const copyAction = PageActionsCtrl.actions.find(action => action.id === 'copy');

    SiteMapItemService.hasItem.and.returnValue(false);
    expect(copyAction.isEnabled()).toBe(false);
  });

  it('does nothing when not confirming the deletion of a page', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find(action => action.id === 'delete');

    DialogService.show.and.returnValue($q.reject());
    PageActionsCtrl.trigger(deleteAction);
    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(confirmDialog);
    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).not.toHaveBeenCalled();
  });

  it('flashes a toast when failing to delete the current page', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();
    const deleteAction = PageActionsCtrl.actions.find(action => action.id === 'delete');

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
    const deleteAction = PageActionsCtrl.actions.find(action => action.id === 'delete');

    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.when());
    PageActionsCtrl.trigger(deleteAction);
    $rootScope.$digest(); // process confirm action
    $rootScope.$digest(); // necessary?
    expect(HippoIframeService.load).toHaveBeenCalledWith('');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(SiteMapItemService.clear).toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
  });

  it('loads the meta data of the current page when opening the page menu', () => {
    const PageActionsCtrl = compileDirectiveAndGetController();

    PageActionsCtrl.onOpenMenu();
    expect(SiteMapItemService.loadAndCache).toHaveBeenCalledWith('siteMapId', 'siteMapItemId');
    expect(ChannelService.loadPageModifiableChannels).toHaveBeenCalled();
  });
});
