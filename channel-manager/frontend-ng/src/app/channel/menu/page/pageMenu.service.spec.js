/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

describe('PageMenuService', () => {
  let $q;
  let $rootScope;

  let confirmDialog;
  let ChannelService;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let PageService;
  let PageMenuService;
  let PageToolsService;
  let PageStructureService;
  let SiteMapItemService;
  let SiteMapService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    confirmDialog = jasmine.createSpyObj('confirmDialog', ['title', 'textContent', 'ok', 'cancel']);
    confirmDialog.title.and.returnValue(confirmDialog);
    confirmDialog.textContent.and.returnValue(confirmDialog);
    confirmDialog.ok.and.returnValue(confirmDialog);
    confirmDialog.cancel.and.returnValue(confirmDialog);

    PageToolsService = jasmine.createSpyObj('PageToolsService', [
      'hasExtensions',
      'showPageTools',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('PageToolsService', PageToolsService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _DialogService_,
      _FeedbackService_,
      _HippoIframeService_,
      _PageService_,
      _PageMenuService_,
      _PageStructureService_,
      _SiteMapItemService_,
      _SiteMapService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      PageService = _PageService_;
      PageMenuService = _PageMenuService_;
      PageStructureService = _PageStructureService_;
      SiteMapItemService = _SiteMapItemService_;
      SiteMapService = _SiteMapService_;
    });
  });

  function getItem(name) {
    return PageMenuService.menu.items.find(item => item.name === name);
  }

  function addAction(name, enabled = true) {
    if (!PageService.actions) {
      PageService.actions = {
        page: {
          items: {},
        },
      };
    }
    PageService.actions.page.items[name] = {
      enabled,
    };
  }

  describe('page menu', () => {
    it('should hide the menu button', () => {
      expect(PageMenuService.menu.isVisible()).toBe(false);
    });

    it('should show the menu button', () => {
      PageService.actions = {
        page: {},
      };

      expect(PageMenuService.menu.isVisible()).toBe(true);
    });

    it('loads the meta data of the current page when opening the page menu', () => {
      spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
      spyOn(ChannelService, 'loadPageModifiableChannels');
      const pageMeta = jasmine.createSpyObj('pageMeta', { getSiteMapItemId: 'siteMapItemId' });
      const page = jasmine.createSpyObj('page', { getMeta: pageMeta });
      spyOn(PageStructureService, 'getPage').and.returnValue(page);
      spyOn(SiteMapItemService, 'loadAndCache');

      const { menu } = PageMenuService;
      menu.onClick();

      expect(SiteMapItemService.loadAndCache).toHaveBeenCalledWith('siteMapId', 'siteMapItemId');
      expect(ChannelService.loadPageModifiableChannels).toHaveBeenCalled();
    });
  });

  describe('tools', () => {
    it('queries the PageToolsService for extensions to check if menu-item is visible', () => {
      getItem('tools').isVisible();

      expect(PageToolsService.hasExtensions).toHaveBeenCalled();
    });

    it('shows page tools when clicked', () => {
      getItem('tools').onClick();

      expect(PageToolsService.showPageTools).toHaveBeenCalled();
    });
  });

  describe('properties', () => {
    it('should hide the "properties" action', () => {
      expect(getItem('properties').isVisible()).toBe(false);
    });

    it('should show the disabled "properties" action', () => {
      addAction('properties', false);

      expect(getItem('properties').isVisible()).toBe(true);
      expect(getItem('properties').isEnabled()).toBe(false);
    });

    it('should show the enabled "properties" action', () => {
      addAction('properties');

      expect(getItem('properties').isVisible()).toBe(true);
      expect(getItem('properties').isEnabled()).toBe(true);
    });

    it('opens the page-properties subpage when "properties" option is clicked', () => {
      spyOn(PageMenuService, 'showSubPage');

      getItem('properties').onClick();
      expect(PageMenuService.showSubPage).toHaveBeenCalledWith('page-properties');
    });
  });

  describe('copy', () => {
    it('should hide the "copy" action', () => {
      expect(getItem('copy').isVisible()).toBe(false);
    });

    it('should show the disabled "copy" action', () => {
      addAction('copy', false);

      expect(getItem('copy').isVisible()).toBe(true);
      expect(getItem('copy').isEnabled()).toBe(false);
    });

    it('should show the enabled "copy" action', () => {
      addAction('copy');

      expect(getItem('copy').isVisible()).toBe(true);
      expect(getItem('copy').isEnabled()).toBe(true);
    });

    it('opens the page-copy subpage when "copy" option is clicked', () => {
      spyOn(PageMenuService, 'showSubPage');

      getItem('copy').onClick();
      expect(PageMenuService.showSubPage).toHaveBeenCalledWith('page-copy');
    });
  });

  describe('move', () => {
    it('should hide the "move" action', () => {
      expect(getItem('move').isVisible()).toBe(false);
    });

    it('should show the disabled "move" action', () => {
      addAction('move', false);

      expect(getItem('move').isVisible()).toBe(true);
      expect(getItem('move').isEnabled()).toBe(false);
    });

    it('should show the enabled "move" action', () => {
      addAction('move');

      expect(getItem('move').isVisible()).toBe(true);
      expect(getItem('move').isEnabled()).toBe(true);
    });

    it('opens the page-move subpage when "move" option is clicked', () => {
      spyOn(PageMenuService, 'showSubPage');

      getItem('move').onClick();
      expect(PageMenuService.showSubPage).toHaveBeenCalledWith('page-move');
    });
  });

  describe('delete', () => {
    beforeEach(() => {
      spyOn(DialogService, 'confirm').and.returnValue(confirmDialog);
      spyOn(DialogService, 'show').and.returnValue($q.when());
      spyOn(SiteMapItemService, 'get').and.returnValue({ name: 'name' });
      spyOn(SiteMapItemService, 'getNumberOfChildren').and.returnValue(0);
    });

    it('should hide the "delete" action', () => {
      expect(getItem('delete').isVisible()).toBe(false);
    });

    it('should show the disabled "delete" action', () => {
      addAction('delete', false);

      expect(getItem('delete').isVisible()).toBe(true);
      expect(getItem('delete').isEnabled()).toBe(false);
    });

    it('should show the enabled "delete" action', () => {
      addAction('delete');

      expect(getItem('delete').isVisible()).toBe(true);
      expect(getItem('delete').isEnabled()).toBe(true);
    });

    it('shows the confirm dialog', () => {
      getItem('delete').onClick();

      expect(confirmDialog.ok).toHaveBeenCalledWith('DELETE');
      expect(confirmDialog.cancel).toHaveBeenCalledWith('CANCEL');
      expect(DialogService.show).toHaveBeenCalledWith(confirmDialog);
    });

    it('shows the confirm delete single page message when the page has no subpages', () => {
      getItem('delete').onClick();

      expect(confirmDialog.textContent).toHaveBeenCalledWith('CONFIRM_DELETE_SINGLE_PAGE_MESSAGE');
    });

    it('shows the confirm delete multiple pages message when the page has subpages', () => {
      SiteMapItemService.getNumberOfChildren.and.returnValue(3);

      getItem('delete').onClick();

      expect(confirmDialog.textContent).toHaveBeenCalledWith('CONFIRM_DELETE_MULTIPLE_PAGE_MESSAGE');
    });

    it('does nothing when not confirming the deletion of a page', () => {
      DialogService.show.and.returnValue($q.reject());
      spyOn(SiteMapItemService, 'deleteItem');

      getItem('delete').onClick();

      $rootScope.$digest();
      expect(SiteMapItemService.deleteItem).not.toHaveBeenCalled();
    });

    it('navigates to the channel\'s homepage after successfully deleting the current page', () => {
      spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
      spyOn(ChannelService, 'checkChanges');
      spyOn(HippoIframeService, 'load');
      spyOn(SiteMapItemService, 'clear');
      spyOn(SiteMapItemService, 'deleteItem').and.returnValue($q.when());
      spyOn(SiteMapService, 'load');

      getItem('delete').onClick();
      $rootScope.$digest(); // process confirm action

      expect(HippoIframeService.load).toHaveBeenCalledWith('');
      expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
      expect(SiteMapItemService.clear).toHaveBeenCalled();
      expect(ChannelService.checkChanges).toHaveBeenCalled();
    });

    it('flashes a toast when failing to delete the current page', () => {
      spyOn(FeedbackService, 'showError');
      spyOn(SiteMapItemService, 'deleteItem').and.returnValue($q.reject());

      getItem('delete').onClick();
      $rootScope.$digest();

      expect(SiteMapItemService.deleteItem).toHaveBeenCalled();

      $rootScope.$digest();
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_PAGE');
    });
  });

  describe('new', () => {
    it('should hide the "new" action', () => {
      expect(getItem('new').isVisible()).toBe(false);
    });

    it('should show the disabled "new" action', () => {
      addAction('new', false);

      expect(getItem('new').isVisible()).toBe(true);
      expect(getItem('new').isEnabled()).toBe(false);
    });

    it('should show the enabled "new" action', () => {
      addAction('new');

      expect(getItem('new').isVisible()).toBe(true);
      expect(getItem('new').isEnabled()).toBe(true);
    });

    it('opens the page-new subpage when "new" option is clicked', () => {
      spyOn(PageMenuService, 'showSubPage');

      getItem('new').onClick();
      expect(PageMenuService.showSubPage).toHaveBeenCalledWith('page-new');
    });
  });
});
