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
        title: 'Three',
      },
    ],
  };

  const newMenuItem = { id: 'child1' };

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

    spyOn(HstService, 'doPost');
    HstService.doPost.and.returnValue($q.when({ data: newMenuItem.id }));
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

  it('caches a menu', (done) => {
    const menu = { id: 'testUuid' };
    HstService.doGet.and.returnValue($q.when({ data: menu }));
    SiteMenuService.getMenu('testUuid')
      .then(() => {
        HstService.doGet.calls.reset();
        SiteMenuService.getMenu('testUuid').then((response) => {
          expect(response).toEqual({ items: [], id: menu.id });
          expect(HstService.doGet).not.toHaveBeenCalled();
          done();
        });
      })
      .catch(() => fail());

    $rootScope.$digest();
  });

  it('should not return cached menu if menuId does not matches cached menuId', (done) => {
    SiteMenuService.getMenu('testUuid')
      .then(() => {
        SiteMenuService.getMenu('testUuid2').then(() => {
          expect(HstService.doGet).toHaveBeenCalledWith('testUuid2');
          done();
        });
      })
      .catch(() => fail());

    $rootScope.$digest();
  });

  it('should not return cached menu if forceUpdate is true', (done) => {
    const menu = { id: 'testUuid' };
    HstService.doGet.and.returnValue($q.when({ data: menu }));
    SiteMenuService.getMenu('testUuid')
      .then(() => {
        HstService.doGet.calls.reset();
        SiteMenuService.getMenu('testUuid', true).then((response) => {
          expect(response).toEqual({ items: [], id: menu.id });
          expect(HstService.doGet).toHaveBeenCalled();
          done();
        });
      })
      .catch(() => fail());

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

  // Create item
  it('should create a menu item', (done) => {
    SiteMenuService.createMenuItem('testUuid', newMenuItem, 'parentId').then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith(newMenuItem, 'testUuid', 'create', 'parentId', '');
      done();
    });
    $rootScope.$digest();
  });

  it('should create a root menu item if parentId is not specified', (done) => {
    SiteMenuService.createMenuItem('testUuid', newMenuItem).then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith(newMenuItem, 'testUuid', 'create', 'testUuid', '');
      done();
    });
    $rootScope.$digest();
  });

  it('should create a menu item at specified position and sibling', (done) => {
    const options = { position: '_pos', siblingId: '_sib' };
    SiteMenuService.createMenuItem('testUuid', newMenuItem, 'testUuid', options).then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith(newMenuItem, 'testUuid', 'create', 'testUuid', '?position=_pos&sibling=_sib');
      done();
    });
    $rootScope.$digest();
  });

  it('should create a menu item at specified sibling only if position is specified as well', (done) => {
    const options = { siblingId: '_sib' };
    SiteMenuService.createMenuItem('testUuid', newMenuItem, 'testUuid', options).then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith(newMenuItem, 'testUuid', 'create', 'testUuid', '');
      done();
    });
    $rootScope.$digest();
  });

  // Save item
  it('should save a menu item', (done) => {
    const menuItemToSave = {
      id: 'child1',
      items: [
        {
          id: 'child2',
          title: 'Child 2',
          sitemapLink: 'home',
          linkType: 'SITEMAPITEM',
          collapsed: false,
        },
        {
          id: 'child3',
          title: 'Child 3',
          link: 'child3link',
          linkType: 'NONE',
        },
      ],
      externalLink: 'http://onehippo.org',
      linkType: 'EXTERNAL',
      title: 'New title',
    };
    const savedMenuItem = {
      id: 'child1',
      items: [
        {
          id: 'child2',
          title: 'Child 2',
          link: 'home',
          linkType: 'SITEMAPITEM',
        },
        {
          id: 'child3',
          title: 'Child 3',
          linkType: 'NONE',
        },
      ],
      link: 'http://onehippo.org',
      linkType: 'EXTERNAL',
      title: 'New title',
    };

    SiteMenuService.saveMenuItem('testUuid', menuItemToSave).then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith(savedMenuItem, 'testUuid');
      done();
    });

    $rootScope.$digest();
  });

  // Move item
  it('should move an item', (done) => {
    SiteMenuService.moveMenuItem('testUuid', 'three', '1', 0).then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith({}, 'testUuid', 'move', 'three', '1', '0');
      done();
    });

    $rootScope.$digest();
  });

  // find path to menu item
  it('should find the path to a menu item', (done) => {
    SiteMenuService.getPathToMenuItem('testUuid', 'child1').then((paths) => {
      expect(paths).toBeDefined();
      expect(paths.length).toBe(3);
      expect(paths[0].id).toEqual('testUuid');
      expect(paths[1].id).toEqual('2');
      expect(paths[2].id).toEqual('child1');
      done();
    });

    $rootScope.$digest();
  });
});
