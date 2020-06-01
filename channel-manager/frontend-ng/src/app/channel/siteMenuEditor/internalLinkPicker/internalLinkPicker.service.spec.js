/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('InternalLinkPickerService', () => {
  let $q;
  let $rootScope;
  let HstService;
  let DialogService;
  let InternalLinkPickerService;

  const testData = {
    items: [
      { id: 'item1' },
      { id: 'item2' },
    ],
    pickerType: 'testPickerType',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _DialogService_, _HstService_, _InternalLinkPickerService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      DialogService = _DialogService_;
      HstService = _HstService_;
      InternalLinkPickerService = _InternalLinkPickerService_;
    });

    spyOn(HstService, 'doGet');
    HstService.doGet.and.returnValue($q.when({ data: testData }));
  });

  it('load initial data', (done) => {
    InternalLinkPickerService.loadDataForLink('root').then((pickerType) => {
      expect(pickerType).toBe('testPickerType');
      done();
    });

    expect(HstService.doGet).toHaveBeenCalledWith('root', 'picker', undefined);
    $rootScope.$digest();
  });

  it('load initial data for link', (done) => {
    InternalLinkPickerService.loadDataForLink('root', 'link').then(() => {
      expect(InternalLinkPickerService.getTree()[0]).toEqual(testData);
      done();
    });

    expect(HstService.doGet).toHaveBeenCalledWith('root', 'picker', 'link');
    $rootScope.$digest();
  });

  it('load child data for specified item', (done) => {
    const item = {
      id: 'itemA',
      items: [],
    };
    InternalLinkPickerService.getData(item).then(() => {
      expect(item.items).toEqual(testData.items);
      done();
    });

    expect(HstService.doGet).toHaveBeenCalledWith('itemA', 'picker');
    $rootScope.$digest();
  });

  it('should open the picker with the dialog service', () => {
    const dialogCfg = { customProperty: true };
    spyOn(DialogService, 'show');
    DialogService.show.and.returnValue($q.when(dialogCfg));
    InternalLinkPickerService.show(dialogCfg);
    expect(DialogService.show).toHaveBeenCalledWith(jasmine.objectContaining(dialogCfg));
  });
});
