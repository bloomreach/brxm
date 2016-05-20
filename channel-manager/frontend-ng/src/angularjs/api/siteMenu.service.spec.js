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

describe('SiteMenuService', () => {
  'use strict';

  let $q;
  let $rootScope;
  let SiteMenuService;
  let HstService;

  const testMenu = {
    id: 'testUuid',
    items: [
      {
        id: '1',
        link: 'http://onehippo.org',
        linkType: 'EXTERNAL',
        localParameters: {
          cssclass: 'bike',
          'call-to-action': 'use it',
        },
        title: 'One',
      },
      {
        id: '2',
        link: 'home',
        linkType: 'SITEMAPITEM',
        title: 'Two',
        items: [
          {
            id: 'child1',
            title: 'Child 1',
          },
        ],
      },
      {
        id: '3',
        title: 'One',
      },
    ],
  };

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _SiteMenuService_, _HstService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      SiteMenuService = _SiteMenuService_;
      HstService = _HstService_;
    });

    spyOn(HstService, 'doGet');
    HstService.doGet.and.returnValue($q.when({ data: testMenu }));
  });

  it('successfully retrieves a menu', (done) => {
    const menu = { id: 'testUuid' };
    HstService.doGet.and.returnValue($q.when({ data: menu }));
    SiteMenuService.getMenu('testUuid')
      .then((response) => {
        expect(response).toEqual({ items: [], id: menu.id });
        done();
      })
      .catch(() => fail());
    expect(HstService.doGet).toHaveBeenCalledWith('testUuid');
    $rootScope.$digest();
  });

  it('relays the server\'s response in case of a failure', (done) => {
    const error = { };
    HstService.doGet.and.returnValue($q.reject(error));
    SiteMenuService.getMenu('testUuid')
      .then(() => fail())
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });

  it('should collapse node with childNodes on first load', (done) => {
    SiteMenuService.getMenu('testUuid')
      .then((menu) => {
        expect(menu.items[1].collapsed).toBe(true);
        done();
      });
    $rootScope.$digest();
  });

  // getMenuItem
  it('should return a main menu item by id', (done) => {
    SiteMenuService.getMenuItem('testUuid', '2').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.id).toEqual('2');
      done();
    });
    $rootScope.$digest();
  });

  it('should return a child menu item by id', (done) => {
    SiteMenuService.getMenuItem('testUuid', 'child1').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.id).toEqual('child1');
      done();
    });
    $rootScope.$digest();
  });

  it('should return a child menu item by id with parameters', (done) => {
    SiteMenuService.getMenuItem('testUuid', '1').then((menuItem) => {
      expect(menuItem.localParameters.cssclass).toEqual('bike');
      done();
    });
    $rootScope.$digest();
  });

  it('should return null when getting an unknown menu item', (done) => {
    SiteMenuService.getMenuItem('testUuid', 'nosuchitem').then((menuItem) => {
      expect(menuItem).toBeNull();
      done();
    });
    $rootScope.$digest();
  });

  it('should update the returned menu data when the title of a menu item changes', (done) => {
    SiteMenuService.getMenu('testUuid').then((menu) => {
      SiteMenuService.getMenuItem('testUuid', 'child1').then((child1) => {
        child1.title = 'New title';
        expect(menu.items[1].items[0].title).toEqual('New title');
        done();
      });
    });
    $rootScope.$digest();
  });

  it('should return externalLink split from the normal link', (done) => {
    SiteMenuService.getMenuItem('testUuid', '1').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.externalLink).toBeDefined();
      expect(menuItem.externalLink).toEqual('http://onehippo.org');
      done();
    });
    $rootScope.$digest();
  });

  it('should return sitemapLink split from the normal link', (done) => {
    SiteMenuService.getMenuItem('testUuid', '2').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.sitemapLink).toBeDefined();
      expect(menuItem.sitemapLink).toEqual('home');
      done();
    });
    $rootScope.$digest();
  });
});
