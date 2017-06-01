/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

  it('calls the passed in callback when opening a sub-page', () => {
    const $ctrl = compileDirectiveAndGetController();

    $ctrl.openSubPage('page-properties');
    expect($scope.onActionSelected).toHaveBeenCalledWith('page-properties');
  });

  it('enables the "new" action if the current channel has both a workspace and prototypes', () => {
    const $ctrl = compileDirectiveAndGetController();

    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect($ctrl.isNewEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect($ctrl.isNewEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect($ctrl.isNewEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect($ctrl.isNewEnabled()).toBe(true);
  });

  it('knows whether the current page is editable', () => {
    const $ctrl = compileDirectiveAndGetController();

    SiteMapItemService.isEditable.and.returnValue(false);
    expect($ctrl.isPageEditable()).toBe(false);

    SiteMapItemService.isEditable.and.returnValue(true);
    expect($ctrl.isPageEditable()).toBe(true);
  });

  it('enables the copy action if the page can be copied', () => {
    const $ctrl = compileDirectiveAndGetController();

    SiteMapItemService.isLocked.and.returnValue(true);
    expect($ctrl.isCopyEnabled()).toBe(false);

    SiteMapItemService.isLocked.and.returnValue(false);
    ChannelService.hasWorkspace.and.returnValue(true);
    expect($ctrl.isCopyEnabled()).toBe(true);

    ChannelService.hasWorkspace.and.returnValue(false);
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    expect($ctrl.isCopyEnabled()).toBe(false);

    SessionService.isCrossChannelPageCopySupported.and.returnValue(true);
    ChannelService.getPageModifiableChannels.and.returnValue(undefined);
    expect($ctrl.isCopyEnabled()).toBe(false);

    ChannelService.getPageModifiableChannels.and.returnValue([]);
    expect($ctrl.isCopyEnabled()).toBe(false);

    ChannelService.getPageModifiableChannels.and.returnValue(['dummy']);
    expect($ctrl.isCopyEnabled()).toBe(true);
  });

  it('disables the copy action if the page is undefined', () => {
    const $ctrl = compileDirectiveAndGetController();

    SiteMapItemService.hasItem.and.returnValue(false);
    expect($ctrl.isCopyEnabled()).toBe(false);
  });

  it('does nothing when not confirming the deletion of a page', () => {
    const $ctrl = compileDirectiveAndGetController();

    DialogService.show.and.returnValue($q.reject());
    $ctrl.deletePage();
    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(confirmDialog);
    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).not.toHaveBeenCalled();
  });

  it('flashes a toast when failing to delete the current page', () => {
    const $ctrl = compileDirectiveAndGetController();

    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.reject());
    $ctrl.deletePage();
    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).toHaveBeenCalled();
    $rootScope.$digest();
    expect(HippoIframeService.load).not.toHaveBeenCalled();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_PAGE');
  });

  it('navigates to the channel\'s homepage after successfully deleting the current page', () => {
    const $ctrl = compileDirectiveAndGetController();

    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.when());
    $ctrl.deletePage();
    $rootScope.$digest(); // process confirm action
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
