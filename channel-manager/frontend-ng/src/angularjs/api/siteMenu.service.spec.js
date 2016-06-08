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
            linkType: 'SITEMAPITEM',
            link: 'child/1',
          },
        ],
      },
      {
        id: '3',
        title: 'Three',
      },
    ],
  };

  const newMenuItem = {
    linkType: 'NONE',
    title: 'NEW_MENU_ITEM_TITLE',
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

    spyOn(HstService, 'doPost');
    HstService.doPost.and.returnValue($q.when({ data: 'child1' }));

    spyOn(HstService, 'doDelete');
    HstService.doDelete.and.returnValue($q.when({ data: 'child1' }));

    spyOn(HstService, 'doPostWithParams');
    HstService.doPostWithParams.and.returnValue($q.when({ data: 'child1' }));

    spyOn(HstService, 'doPutWithHeaders');
    HstService.doPutWithHeaders.and.returnValue($q.when({ data: 'child1' }));

    // preload the default menu
    $rootScope.$apply(() => SiteMenuService.loadMenu('testUuid'));
    HstService.doGet.calls.reset();
  });

  it('successfully loads a menu', (done) => {
    SiteMenuService.loadMenu('localTestUuid')
      .then((menu) => {
        expect(menu.id).toBe(testMenu.id);
        expect(menu.items.length).toBe(3);
        done();
      })
      .catch(() => fail());
    expect(HstService.doGet).toHaveBeenCalledWith('localTestUuid');
    $rootScope.$digest();
  });

  it('relays a failure to load a menu', (done) => {
    const response = { };
    HstService.doGet.and.returnValue($q.reject(response));
    SiteMenuService.loadMenu('localTestUuid')
      .then(() => fail())
      .catch((error) => {
        expect(error).toBe(response);
        done();
      });
    expect(HstService.doGet).toHaveBeenCalledWith('localTestUuid');
    $rootScope.$digest();
  });

  it('retrieves a copy of a menu item', (done) => {
    HstService.doGet.calls.reset();
    SiteMenuService.getEditableMenuItem('2')
      .then((item) => {
        expect(item.id).toBe('2');
        expect(item.collapsed).toBe(true);
        expect(item.items.length).toBe(1);
        expect(item.sitemapLink).toBe('home');
        done();
      })
      .catch(() => fail());
    expect(HstService.doGet).not.toHaveBeenCalled();
    $rootScope.$digest();
  });

  it('waits for the menu to load when retrieving a menu item', (done) => {
    SiteMenuService.loadMenu('testUuid');

    // menu retrieval is in progress
    HstService.doGet.calls.reset();
    SiteMenuService.getEditableMenuItem('2')
      .then((item) => {
        expect(item.id).toBe('2');
        done();
      });

    expect(HstService.doGet).not.toHaveBeenCalled();
    $rootScope.$digest();

    // try again, with failed retrieval
    HstService.doGet.and.returnValue($q.reject());
    SiteMenuService.loadMenu('testUuid');

    SiteMenuService.getEditableMenuItem('2')
      .then(() => fail())
      .catch(() => done());
    $rootScope.$digest();
  });

  it('handles the retrieval of an unknown menu item', (done) => {
    SiteMenuService.getEditableMenuItem('unknown')
      .then((item) => {
        expect(item).toBe(null);
        done();
      })
      .catch(() => fail());
    $rootScope.$digest();
  });

  it('moves a menu item to a specific destination', (done) => {
    const response = { data: 'b' };
    HstService.doPutWithHeaders.and.returnValue($q.when(response));
    SiteMenuService.moveMenuItem('a', 'b', 1)
      .then((data) => {
        expect(data).toBe(response);
        done();
      })
      .catch(() => fail());
    expect(HstService.doPutWithHeaders).toHaveBeenCalledWith('testUuid', { 'Move-From': 'a' }, 'b', '1');
    $rootScope.$digest();
  });

  it('moves a menu item to the root', (done) => {
    const response = {};
    HstService.doPutWithHeaders.and.returnValue($q.reject(response));
    SiteMenuService.moveMenuItem('a', undefined, 1)
      .then(() => fail())
      .catch((data) => {
        expect(data).toBe(response);
        done();
      });
    expect(HstService.doPutWithHeaders).toHaveBeenCalledWith('testUuid', { 'Move-From': 'a' }, 'testUuid', '1');
    $rootScope.$digest();
  });

  it('reloads the menu upon successful deletion', (done) => {
    HstService.doGet.calls.reset();
    HstService.doDelete.and.returnValue($q.when());
    SiteMenuService.deleteMenuItem('2')
      .then(() => {
        expect(HstService.doGet).toHaveBeenCalledWith('testUuid');
        done();
      })
      .catch(() => fail());
    expect(HstService.doDelete).toHaveBeenCalledWith('testUuid', '2');
    $rootScope.$digest();
  });

  it('relays a failure to delete a menu item', (done) => {
    const error = { };
    HstService.doDelete.and.returnValue($q.reject(error));
    SiteMenuService.deleteMenuItem('2')
      .then(() => fail())
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });

  it('saves changes to a menu item', (done) => {
    const item = {
      id: '2',
      collapsed: false,
      title: 'Parent',
      linkType: 'SITEMAPITEM',
      sitemapLink: 'test',
      localParameters: {
        key: 'value',
      },
      items: [
        {
          id: 'child',
          collapsed: true,
          title: 'Child',
          linkType: 'EXTERNAL',
          externalLink: 'External Link',
        },
      ],
    };
    SiteMenuService.saveMenuItem(item)
      .then(() => {
        SiteMenuService.getEditableMenuItem('2')
          .then((editableItem) => {
            expect(editableItem.title).toBe('Parent');
            expect(editableItem.items[0].title).toBe('Child');
            done();
          });
      })
      .catch(() => fail());
    const savedItem = HstService.doPost.calls.mostRecent().args[0];
    expect(HstService.doPost.calls.mostRecent().args[1]).toBe('testUuid');
    expect(savedItem.id).toBe('2');
    expect(savedItem.collapsed).toBeUndefined();
    expect(savedItem.sitemapLink).toBeUndefined();
    expect(savedItem.link).toBe('test');
    expect(savedItem.localParameters.key).toBe('value');
    expect(savedItem.items.length).toBe(1);
    expect(savedItem.items[0].externalLink).toBeUndefined();
    expect(savedItem.items[0].link).toBe('External Link');
    $rootScope.$digest();
  });

  it('relays a failure to save a menu item', (done) => {
    const error = { };
    HstService.doPost.and.returnValue($q.reject(error));
    SiteMenuService.saveMenuItem({ })
      .then(() => fail())
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });

  it('creates a new menu item', (done) => {
    SiteMenuService.createEditableMenuItem()
      .then((item) => {
        expect(HstService.doPostWithParams).toHaveBeenCalled();
        const args = HstService.doPostWithParams.calls.mostRecent().args;
        expect(args[0].linkType).toBe('NONE');
        expect(args[0].title).toBeDefined();
        expect(args[0].localParameters).toBeUndefined();
        expect(args[1]).toBe('testUuid');
        expect(args[2].position).toBe('after');
        expect(args[3]).toBe('testUuid');

        expect(HstService.doGet).toHaveBeenCalled();
        expect(item.id).toBe('child1');
        expect(item.sitemapLink).toBe('child/1');
        done();
      })
      .catch(() => fail());
    HstService.doGet.calls.reset();
    $rootScope.$digest();
  });

  it('waits for the menu to load before creating a new menu item', (done) => {
    testMenu.prototypeItem = {
      localParameters: {
        key1: 'value1',
        key2: undefined,
      },
    };
    SiteMenuService.loadMenu('testUuid');

    SiteMenuService.createEditableMenuItem()
      .then((item) => {
        expect(HstService.doPostWithParams).toHaveBeenCalled();
        const args = HstService.doPostWithParams.calls.mostRecent().args;
        expect(args[0].localParameters).toEqual({ key1: 'value1', key2: undefined });
        delete testMenu.prototypeItem;

        expect(item.id).toBe('child1');
        done();
      })
      .catch(() => fail());
    expect(HstService.doPostWithParams).not.toHaveBeenCalled();
    $rootScope.$digest();
  });

  it('relays a failure to create a menu item', (done) => {
    const error = { };
    HstService.doPostWithParams.and.returnValue($q.reject(error));
    SiteMenuService.createEditableMenuItem()
      .then(() => fail())
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });

  it('should return a child menu item by id', (done) => {
    SiteMenuService.getEditableMenuItem('child1').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.id).toEqual('child1');
      done();
    });
    $rootScope.$digest();
  });

  it('should return a child menu item by id with parameters', (done) => {
    SiteMenuService.getEditableMenuItem('1').then((menuItem) => {
      expect(menuItem.localParameters.cssclass).toEqual('bike');
      done();
    });
    $rootScope.$digest();
  });

  it('should return externalLink split from the normal link', (done) => {
    SiteMenuService.getEditableMenuItem('1').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.externalLink).toBeDefined();
      expect(menuItem.externalLink).toEqual('http://onehippo.org');
      done();
    });
    $rootScope.$digest();
  });

  it('should return sitemapLink split from the normal link', (done) => {
    SiteMenuService.getEditableMenuItem('2').then((menuItem) => {
      expect(menuItem).toBeDefined();
      expect(menuItem.sitemapLink).toBeDefined();
      expect(menuItem.sitemapLink).toEqual('home');
      done();
    });
    $rootScope.$digest();
  });

  // Create item
  it('should create a menu item', (done) => {
    SiteMenuService.createEditableMenuItem().then(() => {
      expect(HstService.doPostWithParams)
        .toHaveBeenCalledWith(newMenuItem, 'testUuid', { position: 'after' }, 'testUuid');
      done();
    });
    $rootScope.$digest();
  });

  it('should create a menu item as next sibling of marker item if it has no children', (done) => {
    SiteMenuService.createEditableMenuItem({ id: 'child1' }).then(() => {
      expect(HstService.doPostWithParams)
        .toHaveBeenCalledWith(newMenuItem, 'testUuid', { position: 'after', sibling: 'child1' }, '2');
      done();
    });
    $rootScope.$digest();
  });

  it('should create a menu item as first child of marker item if it has children', (done) => {
    SiteMenuService.createEditableMenuItem({ id: '2' }).then(() => {
      expect(HstService.doPostWithParams)
        .toHaveBeenCalledWith(newMenuItem, 'testUuid', { position: 'first' }, '2');
      done();
    });
    $rootScope.$digest();
  });

  // Save item
  it('should save a menu item', (done) => {
    SiteMenuService.loadMenu('testUuid');
    $rootScope.$digest();

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

    SiteMenuService.saveMenuItem(menuItemToSave).then(() => {
      expect(HstService.doPost).toHaveBeenCalledWith(savedMenuItem, 'testUuid');
      done();
    });

    $rootScope.$digest();
  });

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
