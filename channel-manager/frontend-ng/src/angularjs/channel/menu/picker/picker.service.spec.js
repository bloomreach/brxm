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

describe('PickerService', () => {
  'use strict';

  let $q;
  let $rootScope;
  let HstService;
  let DialogService;
  let PickerService;

  const testData = {
    items: [
      { id: 'item1' },
      { id: 'item2' },
    ],
  };

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _PickerService_, _DialogService_, _HstService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      PickerService = _PickerService_;
      HstService = _HstService_;
      DialogService = _DialogService_;
    });

    spyOn(HstService, 'doGet');
    HstService.doGet.and.returnValue($q.when({ data: testData }));
  });

  it('exists', () => {
    expect(PickerService).toBeDefined();
  });

  it('load initial data', (done) => {
    PickerService.getInitialData('root').then((initialData) => {
      expect(initialData).not.toBeNull();
      done();
    });

    expect(HstService.doGet).toHaveBeenCalledWith('root', 'picker', undefined);
    $rootScope.$digest();
  });

  it('load initial data for link', (done) => {
    PickerService.getInitialData('root', 'link').then((initialData) => {
      expect(initialData).not.toBeNull();
      expect(PickerService.getTree()).toEqual(initialData);
      done();
    });

    expect(HstService.doGet).toHaveBeenCalledWith('root', 'picker', 'link');
    $rootScope.$digest();
  });

  it('return a copy of remote initial data', (done) => {
    PickerService.getInitialData('root').then((initialData) => {
      expect(initialData).not.toBeNull();
      expect(initialData[0]).not.toBe(testData);
      expect(initialData[0]).toEqual(testData);
      done();
    });

    $rootScope.$digest();
  });

  it('load child data for specified item', (done) => {
    const item = {
      id: 'itemA',
      items: [],
    };
    PickerService.getData(item).then(() => {
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
    PickerService.show(dialogCfg);
    expect(DialogService.show).toHaveBeenCalledWith(jasmine.objectContaining(dialogCfg));
  });
});

