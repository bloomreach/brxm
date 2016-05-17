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

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _SiteMenuService_, _HstService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      SiteMenuService = _SiteMenuService_;
      HstService = _HstService_;
    });

    spyOn(HstService, 'doGet');
  });

  it('successfully retrieves a menu', (done) => {
    const menu = { };
    HstService.doGet.and.returnValue($q.when({ data: menu }));
    SiteMenuService.loadMenu('testUuid')
      .then((response) => {
        expect(response).toBe(menu);
        done();
      })
      .catch(() => fail());
    expect(HstService.doGet).toHaveBeenCalledWith('testUuid');
    $rootScope.$digest();
  });

  it('relays the server\'s response in case of a failure', (done) => {
    const error = { };
    HstService.doGet.and.returnValue($q.reject(error));
    SiteMenuService.loadMenu('testUuid')
      .then(() => fail())
      .catch((response) => {
        expect(response).toBe(error);
        done();
      });
    $rootScope.$digest();
  });
});
