/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

describe('CompoundField', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $q;
  let $rootScope;
  let $scope;
  let FeedbackService;
  let FieldService;

  const fieldType = { jcrType: 'jcrType' };
  const fieldValues = [];

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject((_$componentController_, _$q_, _$rootScope_, _FieldService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      FieldService = _FieldService_;
    });

    FeedbackService = { showError: jasmine.createSpy('showError') };
    $scope = $rootScope.$new();
    $element = angular.element('<div>');
    $ctrl = $componentController('compoundField', { $element, $scope, FeedbackService }, {
      fieldType,
      fieldValues,
      name: 'test-name',
    });
    $ctrl.form = { $setDirty: jasmine.createSpy('$setDirty') };
  });

  describe('onFocus', () => {
    it('focuses the component', () => {
      const focusHandler = jasmine.createSpy('onFocus');
      $element.on('focus', focusHandler);

      $ctrl.onFocus();

      expect(focusHandler).toHaveBeenCalled();
    });
  });

  describe('onBlur', () => {
    it('blurs the component', () => {
      const blurHandler = jasmine.createSpy('onBlur');
      $element.on('blur', blurHandler);

      $ctrl.onBlur();

      expect(blurHandler).toHaveBeenCalled();
    });
  });

  describe('getFieldName', () => {
    it('should compose a name for the multiple field', () => {
      expect($ctrl.getFieldName(0)).toBe('test-name');
      expect($ctrl.getFieldName(1)).toBe('test-name[2]');
      expect($ctrl.getFieldName(2)).toBe('test-name[3]');
    });
  });

  describe('isDraggable', () => {
    it('should be draggable', () => {
      $ctrl.fieldType.multiple = true;
      $ctrl.fieldType.orderable = true;
      $ctrl.fieldValues = ['a', 'b'];

      expect($ctrl.isDraggable()).toBe(true);
    });

    it('should not be draggable for a non-orderable field', () => {
      $ctrl.fieldType.multiple = true;
      $ctrl.fieldType.orderable = false;
      $ctrl.fieldValues = ['a', 'b'];

      expect($ctrl.isDraggable()).toBe(false);
    });

    it('should not be draggable for a non-multiple field type', () => {
      $ctrl.fieldType.multiple = false;
      $ctrl.fieldValues = ['a', 'b'];

      expect($ctrl.isDraggable()).toBe(false);
    });

    it('should not be draggable when there are less than 2 values', () => {
      $ctrl.fieldType.multiple = true;
      $ctrl.fieldValues = ['a'];

      expect($ctrl.isDraggable()).toBe(false);
    });
  });

  describe('isRemovable', () => {
    it('should be removable when there is more than one value', () => {
      $ctrl.fieldType.multiple = true;
      $ctrl.fieldType.required = true;
      $ctrl.fieldValues = ['a', 'b'];

      expect($ctrl.isRemovable()).toBe(true);
    });

    it('should be removable when the field type is not required', () => {
      $ctrl.fieldType.multiple = true;
      $ctrl.fieldType.required = false;
      $ctrl.fieldValues = ['a'];

      expect($ctrl.isRemovable()).toBe(true);
    });

    it('should not be removable when the field type is not multiple', () => {
      $ctrl.fieldType.multiple = false;
      $ctrl.fieldValues = ['a', 'b'];

      expect($ctrl.isRemovable()).toBe(false);
    });

    it('should not be removable when there are less than 2 values', () => {
      $ctrl.fieldType.multiple = true;
      $ctrl.fieldType.required = true;
      $ctrl.fieldValues = ['a'];

      expect($ctrl.isRemovable()).toBe(false);
    });
  });

  describe('onDrop', () => {
    let onDrop;

    beforeEach(() => {
      onDrop = jasmine.createSpy('onDrop');

      $scope.$on('field:drop', onDrop);
      $ctrl.fieldValues = ['a', 'b', 'c', 'd'];
    });

    it('should move a value', () => {
      spyOn(FieldService, 'reorder');

      $ctrl.onDrop({ oldIndex: 1, newIndex: 2 });
      $scope.$digest();

      expect(FieldService.reorder).toHaveBeenCalledWith({ name: 'test-name[2]', order: 3 });
      expect($ctrl.fieldValues).toEqual(['a', 'c', 'b', 'd']);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
      expect(onDrop).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'reorder').and.returnValue($q.reject());

      $ctrl.onDrop({ oldIndex: 1, newIndex: 2 });
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual(['a', 'b', 'c', 'd']);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
      expect(onDrop).toHaveBeenCalled();
    });
  });

  describe('onMove', () => {
    beforeEach(() => {
      $ctrl.fieldValues = ['a', 'b', 'c', 'd'];
    });

    it('should move a value', () => {
      spyOn(FieldService, 'reorder');

      $ctrl.onMove(1, 3);
      $scope.$digest();

      expect(FieldService.reorder).toHaveBeenCalledWith({ name: 'test-name[2]', order: 4 });
      expect($ctrl.fieldValues).toEqual(['a', 'c', 'd', 'b']);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'reorder').and.returnValue($q.reject());

      $ctrl.onMove(1, 3);
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual(['a', 'b', 'c', 'd']);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
    });
  });

  describe('onAdd', () => {
    beforeEach(() => {
      $ctrl.fieldValues = ['a', 'b', 'c'];
    });

    it('should add a value', () => {
      spyOn(FieldService, 'add').and.returnValue('d');

      $ctrl.onAdd(1);
      $scope.$digest();

      expect(FieldService.add).toHaveBeenCalledWith({ name: 'test-name[2]/jcrType' });
      expect($ctrl.fieldValues).toEqual(['a', { fields: 'd' }, 'b', 'c']);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'add').and.returnValue($q.reject());

      $ctrl.onAdd(1);
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual(['a', 'b', 'c']);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
    });
  });

  describe('onRemove', () => {
    beforeEach(() => {
      $ctrl.fieldValues = ['a', 'b', 'c'];
    });

    it('should remove a value', () => {
      spyOn(FieldService, 'remove');

      $ctrl.onRemove(1);
      $scope.$digest();

      expect(FieldService.remove).toHaveBeenCalledWith({ name: 'test-name[2]' });
      expect($ctrl.fieldValues).toEqual(['a', 'c']);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'remove').and.returnValue($q.reject());

      $ctrl.onRemove(1);
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual(['a', 'b', 'c']);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
    });
  });
});
