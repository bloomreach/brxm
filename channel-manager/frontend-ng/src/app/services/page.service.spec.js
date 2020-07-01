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
  let HstService;
  let PageService;
  let PageStructureService;

  let mockPage;
  let mockPageMeta;

  beforeEach(() => {
    inject((_$q_, _$rootScope_, _HstService_, _PageService_, _PageStructureService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      HstService = _HstService_;
      PageService = _PageService_;
      PageStructureService = _PageStructureService_;
    });

    mockPageMeta = jasmine.createSpyObj('PageMeta', {
      getPageId: 'pageId',
    });

    mockPage = jasmine.createSpyObj('Page', {
      getMeta: mockPageMeta,
    });

    spyOn(PageStructureService, 'getPage').and.returnValue(mockPage);
  });

  describe('on page change', () => {
    it('should load the page actions and states', () => {
      const actions = {};
      const states = {};
      spyOn(HstService, 'doGet').and.returnValue($q.resolve({
        data: {
          actions,
          states,
        },
      }));

      $rootScope.$emit('page:change');
      $rootScope.$digest();

      expect(HstService.doGet).toHaveBeenCalledWith('pageId');
      expect(PageService.actions).toBe(actions);
      expect(PageService.states).toBe(states);
    });

    it('should reset the actions and states if there is no page', () => {
      PageStructureService.getPage.and.returnValue(null);
      PageService.actions = {};
      PageService.states = {};

      $rootScope.$emit('page:change');
      $rootScope.$digest();

      expect(PageService.actions).toBeNull();
      expect(PageService.states).toBeNull();
    });

    it('should reset the actions and states if the server returns and error', () => {
      spyOn(HstService, 'doGet').and.returnValue($q.reject());
      PageService.actions = {};
      PageService.states = {};

      $rootScope.$emit('page:change');
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
  });
});
