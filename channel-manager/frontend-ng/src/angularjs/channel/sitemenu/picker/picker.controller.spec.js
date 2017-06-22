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

describe('PickerCtrl', () => {
  let $rootScope;
  let $mdDialog;
  let PickerService;
  let getCtrl;

  const testData = {
    id: 'root',
    items: [
      { id: 'item1' },
      { id: 'item2',
        items: [
          { id: 'item2-1' },
        ],
      },
    ],
  };

  const testPickerTypes = [
    { id: 'pickerTypeA', name: 'Type A', type: 'typeA' },
    { id: 'pickerTypeB', name: 'Type B', type: 'typeB' },
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$q_, _$rootScope_, _$mdDialog_) => {
      $rootScope = _$rootScope_;
      $mdDialog = _$mdDialog_;

      PickerService = jasmine.createSpyObj('PickerService', [
        'loadDataForLink',
        'getData',
        'getTree',
      ]);

      getCtrl = (pickerTypes, initialLink, data = testData) => {
        PickerService.getTree.and.returnValue([data]);
        PickerService.loadDataForLink.and.returnValue(_$q_.when('typeB'));

        const ctrl = $controller('PickerCtrl', {
          $scope: $rootScope.$new(),
          PickerService,
        }, {
          pickerTypes,
          initialLink,
        });
        $rootScope.$digest();
        return ctrl;
      };
    });
  });

  it('should fail creation if no picker types are defined', () => {
    expect(getCtrl).toThrowError('No types defined for picker');
  });

  it('should load initial data of first pickerType when created', () => {
    getCtrl(testPickerTypes);
    expect(PickerService.loadDataForLink).toHaveBeenCalledWith('pickerTypeA', undefined);
  });

  it('should select the first root item if none is set with initialLink', () => {
    const PickerCtrl = getCtrl(testPickerTypes);
    expect(PickerCtrl.selectedItem).toBeDefined();
    expect(PickerCtrl.selectedItem).toEqual(testData);
    expect(PickerCtrl.selectedDocument).toBeNull();
  });

  it('should select the item referenced by initialLink', () => {
    const testDataWithSelected = angular.copy(testData, {});
    testDataWithSelected.items[1].items[0].selected = true;

    const PickerCtrl = getCtrl(testPickerTypes, 'item2-1', testDataWithSelected);
    expect(PickerService.loadDataForLink).toHaveBeenCalledWith('pickerTypeA', 'item2-1');
    expect(PickerCtrl.selectedItem).toBeDefined();
    expect(PickerCtrl.selectedItem).toEqual(testDataWithSelected.items[1]);
    expect(PickerCtrl.selectedDocument).toBeDefined();
    expect(PickerCtrl.selectedDocument).toEqual(testDataWithSelected.items[1].items[0]);
  });

  describe('changePickerType', () => {
    it('should (re)load initial data when switching picker types', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      PickerCtrl.changePickerType();
      expect(PickerService.loadDataForLink).toHaveBeenCalledWith('pickerTypeB');
      PickerService.loadDataForLink.calls.reset();

      PickerCtrl.pickerType = testPickerTypes[0];
      PickerCtrl.changePickerType();
      expect(PickerService.loadDataForLink).toHaveBeenCalledWith('pickerTypeA');
    });

    it('should reset the selected document when switching picker types', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      PickerCtrl.selectedItem = { type: 'typeA' };
      PickerCtrl.selectedDocument = {};
      PickerCtrl.pickerType = testPickerTypes[1];
      PickerCtrl.changePickerType();
      $rootScope.$digest();
      expect(PickerCtrl.selectedDocument).toBeNull();
    });

    it('should set the selected item to root when switching picker types', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      PickerCtrl.selectedItem = { type: 'typeA' };
      PickerCtrl.pickerType = testPickerTypes[1];
      PickerCtrl.changePickerType();
      $rootScope.$digest();
      expect(PickerCtrl.selectedItem).toEqual(testData);
    });
  });

  describe('ok', () => {
    it('should close the dialog with the selected document return value', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      const selectedDoc = {};
      PickerCtrl.selectedDocument = {};
      spyOn($mdDialog, 'hide');
      PickerCtrl.ok();
      expect($mdDialog.hide).toHaveBeenCalledWith(selectedDoc);
    });
  });

  describe('cancel', () => {
    it('should cancel the dialog', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      const selectedDoc = {};
      PickerCtrl.selectedDocument = {};
      spyOn($mdDialog, 'cancel');
      PickerCtrl.cancel();
      expect($mdDialog.cancel).not.toHaveBeenCalledWith(selectedDoc);
      expect($mdDialog.cancel).toHaveBeenCalled();
    });
  });

  describe('treeOptions', () => {
    it('should only display items of type folder and page', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      expect(PickerCtrl.treeOptions.displayItem({ type: 'folder' })).toBe(true);
      expect(PickerCtrl.treeOptions.displayItem({ type: 'page' })).toBe(true);
    });

    it('should load child items when item is expanded', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      const item = { collapsed: true, items: [] };

      PickerCtrl.treeOptions.toggleItem(item);

      expect(PickerService.getData).toHaveBeenCalledWith(item);
    });

    it('should load child items when item is selected', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      const item = { leaf: true, items: [] };

      PickerCtrl.treeOptions.selectItem(item);

      expect(PickerService.getData).toHaveBeenCalledWith(item);
    });

    it('should only select item if it is selectable', () => {
      const PickerCtrl = getCtrl(testPickerTypes);
      const item = { selectable: false, items: [] };

      PickerCtrl.treeOptions.selectItem(item);
      expect(PickerCtrl.selectedDocument).toBeNull();

      item.selectable = true;
      PickerCtrl.treeOptions.selectItem(item);
      expect(PickerCtrl.selectedDocument).toBe(item);
    });
  });
});
