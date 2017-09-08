/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('PageActionsService', () => {
  let $q;
  let $rootScope;
  let $translate;

  let ChannelService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let PageActionsService;
  let PageMetaDataService;
  let SessionService;
  let SiteMapItemService;
  let SiteMapService;

  const confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
  confirmDialog.title.and.returnValue(confirmDialog);
  confirmDialog.textContent.and.returnValue(confirmDialog);
  confirmDialog.ok.and.returnValue(confirmDialog);
  confirmDialog.cancel.and.returnValue(confirmDialog);

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$q_,
      _$rootScope_,
      _$translate_,
      _ChannelService_,
      _DialogService_,
      _FeedbackService_,
      _HippoIframeService_,
      _PageActionsService_,
      _PageMetaDataService_,
      _SessionService_,
      _SiteMapItemService_,
      _SiteMapService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      PageActionsService = _PageActionsService_;
      PageMetaDataService = _PageMetaDataService_;
      SessionService = _SessionService_;
      SiteMapItemService = _SiteMapItemService_;
      SiteMapService = _SiteMapService_;
    });

    spyOn($translate, 'instant').and.callFake((key) => {
      if (key.startsWith('TOOLBAR_MENU_PAGES_')) {
        return key.replace(/^TOOLBAR_MENU_PAGES_/, '');
      }

      return key;
    });

    spyOn(FeedbackService, 'showError');
    spyOn(ChannelService, 'isEditable').and.returnValue(false);
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

  const getItem = name => PageActionsService.menu.items.find(item => item.name === name);

  // menu button
  it('only shows the menu button if the channel is editable', () => {
    const menu = PageActionsService.menu;
    expect(menu.isVisible()).toBe(false);

    ChannelService.isEditable.and.returnValue(true);
    expect(menu.isVisible()).toBe(true);
  });

  it('loads the meta data of the current page when opening the page menu', () => {
    const menu = PageActionsService.menu;
    menu.onClick();

    expect(SiteMapItemService.loadAndCache).toHaveBeenCalledWith('siteMapId', 'siteMapItemId');
    expect(ChannelService.loadPageModifiableChannels).toHaveBeenCalled();
  });

  // properties
  it('enables the "properties" option if the page is editable', () => {
    const properties = getItem('properties');
    SiteMapItemService.isEditable.and.returnValue(true);
    expect(properties.isVisible()).toBe(true);
    expect(properties.isEnabled()).toBe(true);

    SiteMapItemService.isEditable.and.returnValue(false);
    expect(properties.isVisible()).toBe(true);
    expect(properties.isEnabled()).toBe(false);
  });

  it('opens the page-properties subpage when "properties" option is clicked', () => {
    spyOn(PageActionsService, 'showSubPage');

    getItem('properties').onClick();
    expect(PageActionsService.showSubPage).toHaveBeenCalledWith('page-properties');
  });

  // copy
  it('enables the "copy" option if the page can be copied', () => {
    const copy = getItem('copy');

    SiteMapItemService.isLocked.and.returnValue(true);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(false);

    SiteMapItemService.isLocked.and.returnValue(false);
    ChannelService.hasWorkspace.and.returnValue(true);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(true);

    ChannelService.hasWorkspace.and.returnValue(false);
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(false);

    SessionService.isCrossChannelPageCopySupported.and.returnValue(true);
    ChannelService.getPageModifiableChannels.and.returnValue(undefined);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(false);

    ChannelService.getPageModifiableChannels.and.returnValue([]);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(false);

    ChannelService.getPageModifiableChannels.and.returnValue(['dummy']);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(true);

    // page is undefined
    SiteMapItemService.hasItem.and.returnValue(false);
    expect(copy.isVisible()).toBe(true);
    expect(copy.isEnabled()).toBe(false);
  });

  it('opens the page-copy subpage when "copy" option is clicked', () => {
    spyOn(PageActionsService, 'showSubPage');

    getItem('copy').onClick();
    expect(PageActionsService.showSubPage).toHaveBeenCalledWith('page-copy');
  });

  // move
  it('enables the "move" option if the page can be moved', () => {
    const move = getItem('move');
    expect(move.isVisible()).toBe(true);
    expect(move.isEnabled()).toBe(false);

    SiteMapItemService.isEditable.and.returnValue(true);
    expect(move.isVisible()).toBe(true);
    expect(move.isEnabled()).toBe(true);
  });

  it('opens the page-move subpage when the "move" option is clicked', () => {
    spyOn(PageActionsService, 'showSubPage');

    getItem('move').onClick();
    expect(PageActionsService.showSubPage).toHaveBeenCalledWith('page-move');
  });

  // delete
  it('enables the "delete" option if the page can be deleted', () => {
    const del = getItem('delete');
    expect(del.isVisible()).toBe(true);
    expect(del.isEnabled()).toBe(false);

    SiteMapItemService.isEditable.and.returnValue(true);
    expect(del.isVisible()).toBe(true);
    expect(del.isEnabled()).toBe(true);
  });

  it('navigates to the channel\'s homepage after successfully deleting the current page', () => {
    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.when());
    getItem('delete').onClick();
    $rootScope.$digest(); // process confirm action

    expect(HippoIframeService.load).toHaveBeenCalledWith('');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(SiteMapItemService.clear).toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
  });

  it('does nothing when not confirming the deletion of a page', () => {
    DialogService.show.and.returnValue($q.reject());
    getItem('delete').onClick();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalledWith(confirmDialog);

    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).not.toHaveBeenCalled();
  });

  it('flashes a toast when failing to delete the current page', () => {
    DialogService.show.and.returnValue($q.when());
    SiteMapItemService.deleteItem.and.returnValue($q.reject());
    getItem('delete').onClick();

    $rootScope.$digest();
    expect(SiteMapItemService.deleteItem).toHaveBeenCalled();

    $rootScope.$digest();
    expect(HippoIframeService.load).not.toHaveBeenCalled();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_PAGE');
  });

  // new
  it('enables the "new" action if the current channel has both a workspace and prototypes', () => {
    const newPage = getItem('new');
    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect(newPage.isEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(false);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect(newPage.isEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(false);
    expect(newPage.isEnabled()).toBe(false);

    ChannelService.hasWorkspace.and.returnValue(true);
    ChannelService.hasPrototypes.and.returnValue(true);
    expect(newPage.isEnabled()).toBe(true);
  });

  it('opens the page-new subpage when the "new" option is clicked', () => {
    spyOn(PageActionsService, 'showSubPage');

    getItem('new').onClick();
    expect(PageActionsService.showSubPage).toHaveBeenCalledWith('page-new');
  });
});
