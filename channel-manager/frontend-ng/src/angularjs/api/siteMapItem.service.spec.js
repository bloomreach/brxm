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

/* eslint-disable prefer-const */

import angular from 'angular';
import 'angular-mocks';

describe('SiteMapItemService', () => {
  let $q;
  let $rootScope;
  let SiteMapItemService;
  let HstService;
  let FeedbackService;
  let ConfigService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _SiteMapItemService_, _ConfigService_, _HstService_, _FeedbackService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      SiteMapItemService = _SiteMapItemService_;
      ConfigService = _ConfigService_;
      HstService = _HstService_;
      FeedbackService = _FeedbackService_;
    });

    spyOn(HstService, 'doGet');
    spyOn(HstService, 'doPost');
    spyOn(FeedbackService, 'showError');
    ConfigService.cmsUser = 'tester';
  });

  it('retrieves the sitemap item from the HST service', () => {
    const siteMapItem = {
      lockedBy: 'dumbo',
    };
    HstService.doGet.and.returnValue($q.when({ data: siteMapItem }));
    SiteMapItemService.loadAndCache('siteMapId', 'siteMapItemId');
    expect(HstService.doGet).toHaveBeenCalledWith('siteMapId', 'item', 'siteMapItemId');
    expect(SiteMapItemService.hasItem()).toBe(false);

    $rootScope.$digest();
    expect(SiteMapItemService.hasItem()).toBe(true);
    expect(SiteMapItemService.get()).toBe(siteMapItem);

    SiteMapItemService.clear();
    expect(SiteMapItemService.hasItem()).toBe(false);
    expect(SiteMapItemService.get()).toBeUndefined();
  });

  it('flashes a toast when the retrieval of a sitemap item fails', () => {
    HstService.doGet.and.returnValue($q.reject());
    SiteMapItemService.loadAndCache('siteMapId', 'siteMapItemId');
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_SITEMAP_ITEM_RETRIEVAL_FAILED');
  });

  it('derives if the current page is locked by another user', () => {
    expect(SiteMapItemService.isLocked()).toBe(false);

    const siteMapItem = { };
    HstService.doGet.and.returnValue($q.when({ data: siteMapItem }));
    SiteMapItemService.loadAndCache('siteMapId', 'siteMapItemId');
    $rootScope.$digest();
    expect(SiteMapItemService.isLocked()).toBe(false);

    siteMapItem.lockedBy = 'tester';
    expect(SiteMapItemService.isLocked()).toBe(false);

    siteMapItem.lockedBy = 'dumbo';
    expect(SiteMapItemService.isLocked()).toBe(true);
  });

  it('derives if the current page is editable', () => {
    expect(SiteMapItemService.isEditable()).toBe(false);

    const siteMapItem = {
      isHomePage: false,
      inherited: false,
      workspaceConfiguration: true,
    };
    HstService.doGet.and.returnValue($q.when({ data: siteMapItem }));
    SiteMapItemService.loadAndCache('siteMapId', 'siteMapItemId');
    $rootScope.$digest();
    expect(SiteMapItemService.isEditable()).toBe(true);

    siteMapItem.isHomePage = true;
    expect(SiteMapItemService.isEditable()).toBe(false);

    siteMapItem.isHomePage = false;
    siteMapItem.inherited = true;
    expect(SiteMapItemService.isEditable()).toBe(false);

    siteMapItem.inherited = false;
    siteMapItem.workspaceConfiguration = false;
    expect(SiteMapItemService.isEditable()).toBe(false);

    siteMapItem.workspaceConfiguration = true;
    siteMapItem.lockedBy = 'dumbo';
    expect(SiteMapItemService.isEditable()).toBe(false);
  });

  it('fails to delete the item if it has none', (done) => {
    SiteMapItemService.deleteItem()
      .then(() => fail())
      .catch(() => done());
    $rootScope.$digest();
  });

  it('relays HST service\'s result when deleting an item successfully', (done) => {
    const siteMapItem = { id: 'siteMapItemIdInSiteMapItem' };
    HstService.doGet.and.returnValue($q.when({ data: siteMapItem }));
    SiteMapItemService.loadAndCache('siteMapId', 'siteMapItemId');
    $rootScope.$digest();

    HstService.doPost.and.returnValue($q.when());
    SiteMapItemService.deleteItem()
      .then(() => done());
    expect(HstService.doPost).toHaveBeenCalledWith(null, 'siteMapId', 'delete', 'siteMapItemIdInSiteMapItem');
    $rootScope.$digest();
  });

  it('relays HST service\'s result when failing to delete an item', (done) => {
    const siteMapItem = { id: 'siteMapItemIdInSiteMapItem' };
    HstService.doGet.and.returnValue($q.when({ data: siteMapItem }));
    SiteMapItemService.loadAndCache('siteMapId', 'siteMapItemId');
    $rootScope.$digest();

    HstService.doPost.and.returnValue($q.reject());
    SiteMapItemService.deleteItem()
      .then(() => fail())
      .catch(() => done());
    $rootScope.$digest();
  });

  it('asks HST service to update a sitemap item and returns its response', (done) => {
    const siteMapItem = { };
    const resultData = { renderPathInfo: '/test' };
    HstService.doPost.and.returnValue($q.when({ data: resultData }));
    SiteMapItemService.updateItem(siteMapItem, 'siteMapId')
      .then((data) => {
        expect(data).toBe(resultData);
        done();
      });
    expect(HstService.doPost).toHaveBeenCalledWith(siteMapItem, 'siteMapId', 'update');
    $rootScope.$digest();
  });
});
