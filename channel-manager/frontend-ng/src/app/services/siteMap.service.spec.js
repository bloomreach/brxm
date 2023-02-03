/*
 * Copyright 2016-2023 Bloomreach
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

describe('SiteMapService', () => {
  let $q;
  let $rootScope;
  let SiteMapService;
  let HstService;
  let FeedbackService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _SiteMapService_, _HstService_, _FeedbackService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      SiteMapService = _SiteMapService_;
      HstService = _HstService_;
      FeedbackService = _FeedbackService_;
    });

    spyOn(HstService, 'doPost');
    spyOn(HstService, 'doPostWithHeaders');
    spyOn(FeedbackService, 'showError');
  });

  it('delegates the creation of a new page to the HST service', (done) => {
    HstService.doPost.and.returnValue($q.when({ data: 'test' }));
    SiteMapService.create('siteMapId', 'parentSiteMapItemId', {})
      .then((result) => {
        expect(result).toBe('test');
        done();
      })
      .catch(() => {
        fail();
      });
    expect(HstService.doPost).toHaveBeenCalledWith({}, 'siteMapId', 'create', 'parentSiteMapItemId');
    $rootScope.$digest();
  });

  it('relays a failure to create a new page', (done) => {
    HstService.doPost.and.returnValue($q.reject());
    SiteMapService.create('siteMapId', undefined, {})
      .then(() => {
        fail();
      })
      .catch(() => {
        done();
      });
    expect(HstService.doPost).toHaveBeenCalledWith({}, 'siteMapId', 'create', undefined);
    $rootScope.$digest();
  });

  it('delegates copying a page to the HST service', (done) => {
    const headers = {
      foo: 'bar',
      key: 'value',
    };
    HstService.doPostWithHeaders.and.returnValue($q.when({ data: 'test' }));
    SiteMapService.copy('siteMapId', headers)
      .then((result) => {
        expect(result).toBe('test');
        done();
      })
      .catch(() => {
        fail();
      });
    expect(HstService.doPostWithHeaders).toHaveBeenCalledWith('siteMapId', headers, 'copy');
    $rootScope.$digest();
  });

  it('relays a failure to copy a page', (done) => {
    const headers = {
      foo: 'bar',
      key: 'value',
    };
    const response = { };
    HstService.doPostWithHeaders.and.returnValue($q.reject(response));
    SiteMapService.copy('siteMapId', headers)
      .then(() => {
        fail();
      })
      .catch((result) => {
        expect(result).toBe(response);
        done();
      });
    $rootScope.$digest();
  });
});
