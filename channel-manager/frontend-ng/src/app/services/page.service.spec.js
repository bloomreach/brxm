/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

describe('PageService', () => {
  let $q;
  let $rootScope;
  let $state;
  let EditContentService;
  let HippoIframeService;
  let HstService;
  let PageService;
  let PageStructureService;
  let SiteMapService;

  let mockPage;
  let mockPageMeta;

  beforeEach(() => {
    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _EditContentService_,
      _HippoIframeService_,
      _HstService_,
      _PageService_,
      _PageStructureService_,
      _SiteMapService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      EditContentService = _EditContentService_;
      HippoIframeService = _HippoIframeService_;
      HstService = _HstService_;
      PageService = _PageService_;
      PageStructureService = _PageStructureService_;
      SiteMapService = _SiteMapService_;
    });

    mockPageMeta = jasmine.createSpyObj('PageMeta', {
      getPageId: 'pageId',
      getSiteMapItemId: 'siteMapItemId',
    });

    mockPage = jasmine.createSpyObj('Page', {
      getMeta: mockPageMeta,
    });

    spyOn(PageStructureService, 'getPage').and.returnValue(mockPage);
  });

  describe('on page:change', () => {
    it('should load', () => {
      spyOn(PageService, 'load');
      spyOn(PageService, 'syncPageEditor');

      $rootScope.$emit('page:change');
      $rootScope.$digest();

      expect(PageService.load).toHaveBeenCalled();
      expect(PageService.syncPageEditor).not.toHaveBeenCalled();
    });

    it('should sync the page editor if it is open', () => {
      spyOn(PageService, 'load');
      spyOn(PageService, 'syncPageEditor');
      spyOn(EditContentService, 'isEditingXPage').and.returnValue(true);

      $rootScope.$emit('page:change');
      $rootScope.$digest();

      expect(PageService.syncPageEditor).toHaveBeenCalled();
    });
  });

  describe('on page:check-changes', () => {
    it('should load', () => {
      spyOn(PageService, 'load');

      $rootScope.$emit('page:check-changes');
      $rootScope.$digest();

      expect(PageService.load).toHaveBeenCalled();
    });
  });

  describe('load', () => {
    it('should load the page actions and states', () => {
      const actions = {};
      const states = {};
      spyOn(HstService, 'doGet').and.returnValue($q.resolve({
        data: {
          actions,
          states,
        },
      }));

      PageService.load();
      $rootScope.$digest();

      expect(HstService.doGet).toHaveBeenCalledWith('pageId', 'item', 'siteMapItemId');
      expect(PageService.actions).toBe(actions);
      expect(PageService.states).toBe(states);
    });

    it('should reset the actions and states if there is no page', () => {
      PageStructureService.getPage.and.returnValue(null);
      PageService.actions = {};
      PageService.states = {};

      PageService.load();
      $rootScope.$digest();

      expect(PageService.actions).toBeNull();
      expect(PageService.states).toBeNull();
    });

    it('should reset the actions and states if the server returns and error', () => {
      spyOn(HstService, 'doGet').and.returnValue($q.reject());
      PageService.actions = {};
      PageService.states = {};

      PageService.load();
      $rootScope.$digest();

      expect(PageService.actions).toBeNull();
      expect(PageService.states).toBeNull();
    });
  });

  describe('actions', () => {
    beforeEach(() => {
      PageService.actions = {
        page: {
          items: {
            edit: { enabled: true },
            view: { enabled: false },
          },
        },
        site: {},
      };
    });

    describe('hasActions', () => {
      it('should return false', () => {
        expect(PageService.hasActions('unknown')).toBe(false);
      });

      it('should return true', () => {
        expect(PageService.hasActions('site')).toBe(true);
        expect(PageService.hasActions('page')).toBe(true);
      });
    });

    describe('hasAction', () => {
      it('should return false', () => {
        expect(PageService.hasAction('unknown', 'copy')).toBe(false);
        expect(PageService.hasAction('site', 'copy')).toBe(false);
        expect(PageService.hasAction('page', 'copy')).toBe(false);
      });

      it('should return true', () => {
        expect(PageService.hasAction('page', 'edit')).toBe(true);
      });
    });

    describe('getAction', () => {
      it('should return null', () => {
        expect(PageService.getAction('unknown', 'copy')).toBeNull();
        expect(PageService.getAction('site', 'copy')).toBeNull();
        expect(PageService.getAction('page', 'copy')).toBeNull();
      });

      it('should return an action', () => {
        expect(PageService.getAction('page', 'edit')).not.toBeNull();
      });
    });

    describe('isActionEnabled', () => {
      it('should return false', () => {
        expect(PageService.isActionEnabled('unknown', 'copy')).toBe(false);
        expect(PageService.isActionEnabled('site', 'copy')).toBe(false);
        expect(PageService.isActionEnabled('page', 'copy')).toBe(false);
        expect(PageService.isActionEnabled('page', 'view')).toBe(false);
      });

      it('should return true', () => {
        expect(PageService.isActionEnabled('page', 'edit')).toBe(true);
      });
    });

    describe('hasSomeAction', () => {
      it('should return false', () => {
        expect(PageService.hasSomeAction('unknown', 'copy', 'move')).toBe(false);
        expect(PageService.hasSomeAction('page', 'copy', 'move')).toBe(false);
      });

      it('should return true', () => {
        expect(PageService.hasSomeAction('page', 'copy', 'move', 'view')).toBe(true);
      });
    });
  });

  describe('states', () => {
    beforeEach(() => {
      PageService.states = { xpage: {} };
    });

    describe('hasState', () => {
      it('should return false', () => {
        expect(PageService.hasState('page')).toBe(false);

        PageService.states = null;
        expect(PageService.hasState('xpage')).toBe(false);
      });

      it('should return true', () => {
        expect(PageService.hasState('xpage')).toBe(true);
      });
    });

    describe('getState', () => {
      it('should return null', () => {
        expect(PageService.getState('page')).toBeNull();
      });

      it('should return an state', () => {
        expect(PageService.getState('xpage')).not.toBeNull();
      });
    });
  });

  describe('XPage related properties', () => {
    describe('isXPage', () => {
      it('should return true', () => {
        PageService.states = { xpage: {} };

        const actual = PageService.isXPage;

        expect(actual).toBeTruthy();
      });

      it('should return false', () => {
        PageService.states = {};

        const actual = PageService.isXPage;

        expect(actual).toBeFalsy();
      });
    });

    describe('xPageId', () => {
      it('should return undefined states are undefined', () => {
        PageService.states = undefined;

        const actual = PageService.xPageId;

        expect(actual).toBeUndefined();
      });

      it('should return undefined if xpage field is undefined', () => {
        PageService.states = {};

        const actual = PageService.xPageId;

        expect(actual).toBeUndefined();
      });

      it('should return id', () => {
        PageService.states = { xpage: { id: 123 } };

        const actual = PageService.xPageId;

        expect(actual).toBe(123);
      });
    });
  });

  describe('syncPageEditor', () => {
    it('should do nothing if we are already editing the current page', () => {
      PageService.states = { xpage: { id: 123 } };
      spyOn($state, 'go');
      spyOn(EditContentService, 'isEditing').and.returnValue(true);

      PageService.syncPageEditor();

      expect($state.go).not.toHaveBeenCalled();
    });

    it('should open the page editor if the current page is an experience page', () => {
      spyOn($state, 'go');
      PageService.states = { xpage: { id: 123 } };

      PageService.syncPageEditor();

      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.edit-page.content', { documentId: 123 });
    });

    it('should open edit-page-unavailable state if current page is not an experience page', () => {
      PageService.states = {};
      const sitemap = [
        {
          renderPathInfo: '/',
          children: [
            {
              pageTitle: 'My XPage',
              renderPathInfo: '/xpages/my-xpage',
            },
          ],
        },
      ];
      spyOn($state, 'go');
      spyOn(SiteMapService, 'get').and.returnValue(sitemap);
      spyOn(HippoIframeService, 'getCurrentRenderPathInfo').and.returnValue('/xpages/my-xpage');

      PageService.syncPageEditor();

      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.edit-page-unavailable', { title: 'My XPage' });
    });
  });
});
