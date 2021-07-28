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

describe('ChoiceField', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $q;
  let $rootScope;
  let $scope;
  let FeedbackService;
  let FieldService;
  let choiceValues;

  const choiceType = {
    displayName: 'Choice',
    required: true,
    optional: false,
    multiple: true,
    hint: 'bla bla',
    minValues: 1,
    maxValues: 3,
    choices: {
      choice1: {
        id: 'choice1',
        fields: [],
        type: 'PRIMITIVE',
      },
      choice2: {
        id: 'choice2',
        fields: [],
        type: 'COMPOUND',
      },
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject((_$componentController_, _$q_, _$rootScope_, _FieldService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      FieldService = _FieldService_;
    });

    choiceValues = [
      {
        chosenId: 'choice2',
        chosenValue: { fields: [] },
      },
      {
        chosenId: 'choice1',
        chosenValue: { fields: [] },
      },
      {
        chosenId: 'choice2',
        chosenValue: { fields: [] },
      },
    ];
    FeedbackService = { showError: jasmine.createSpy('showError') };
    $scope = $rootScope.$new();
    $element = angular.element('<div>');
    $ctrl = $componentController('choiceField', { $element, $scope, FeedbackService }, {
      fieldType: { ...choiceType },
      fieldValues: choiceValues.slice(),
      name: 'test-name',
    });
    $ctrl.form = { $setDirty: jasmine.createSpy('$setDirty') };
  });

  describe('onFocus', () => {
    it('should keep track of the focused state', () => {
      const focusHandler = jasmine.createSpy('onFocus');
      const blurHandler = jasmine.createSpy('onBlur');
      $element.on('focus', focusHandler);
      $element.on('blur', blurHandler);

      expect($ctrl.hasFocus).toBeFalsy();

      $ctrl.onFocus();

      expect($ctrl.hasFocus).toBeTruthy();
      expect(focusHandler).toHaveBeenCalled();
      expect(blurHandler).not.toHaveBeenCalled();
      focusHandler.calls.reset();

      $ctrl.onBlur();

      expect($ctrl.hasFocus).toBeFalsy();
      expect(focusHandler).not.toHaveBeenCalled();
      expect(blurHandler).toHaveBeenCalled();
    });
  });

  describe('getFieldName', () => {
    it('should help composing unique form field names', () => {
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
      $ctrl.fieldType.orderable = true;
      $ctrl.fieldValues = ['a'];

      expect($ctrl.isDraggable()).toBe(false);
    });
  });

  describe('isAddable', () => {
    it('should be addable while the maximum amount of values has not been reached', () => {
      expect($ctrl.isAddable()).toBe(false);

      $ctrl.fieldType.maxValues += 1;

      expect($ctrl.isAddable()).toBe(true);
    });
  });

  describe('isRemovable', () => {
    it('should be removable when the fieldType is optional', () => {
      $ctrl.fieldType.optional = true;

      expect($ctrl.isRemovable()).toBe(true);
    });

    it('should not be removable when the amount of choices is one and the minimum amount of values is reached', () => {
      delete $ctrl.fieldType.choices.choice2;
      $ctrl.fieldType.minValues = 3;

      expect($ctrl.isRemovable()).toBe(false);
    });

    it('should be removable when the number of values is greater than zero', () => {
      expect($ctrl.isRemovable()).toBe(true);
    });

    it('should not be removable when the number of values is zero', () => {
      $ctrl.fieldValues = [];

      expect($ctrl.isRemovable()).toBe(false);
    });
  });

  describe('onDrop', () => {
    let onDrop;

    beforeEach(() => {
      onDrop = jasmine.createSpy('onDrop');

      $scope.$on('field:drop', onDrop);
    });

    it('should move a value', () => {
      spyOn(FieldService, 'reorder');
      spyOn($ctrl, '_focus');

      $ctrl.onDrop({ oldIndex: 1, newIndex: 2 });
      $scope.$digest();

      expect(FieldService.reorder).toHaveBeenCalledWith({ name: 'test-name[2]', order: 3 });
      expect($ctrl._focus).toHaveBeenCalledWith(2, false, $ctrl._isCKEditorField(2));
      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
      ]);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
      expect(onDrop).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'reorder').and.returnValue($q.reject());

      $ctrl.onDrop({ oldIndex: 1, newIndex: 2 });
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
      ]);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
      expect(onDrop).toHaveBeenCalled();
    });
  });

  describe('onMove', () => {
    it('should move a value', () => {
      spyOn(FieldService, 'reorder');

      $ctrl.onMove(1, 2);
      $scope.$digest();

      expect(FieldService.reorder).toHaveBeenCalledWith({ name: 'test-name[2]', order: 3 });
      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
      ]);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'reorder').and.returnValue($q.reject());

      $ctrl.onMove(1, 2);
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
      ]);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
    });
  });

  describe('onAdd', () => {
    it('should add a value', () => {
      spyOn(FieldService, 'add').and.returnValue({ choice1: [{ value: 'something' }] });

      $ctrl.onAdd('choice1', 1);
      $scope.$digest();

      expect(FieldService.add).toHaveBeenCalledWith({ name: 'test-name[2]/choice1' });
      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1', chosenValue: { value: 'something' } }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
      ]);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'add').and.returnValue($q.reject());

      $ctrl.onAdd('choice1', 1);
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
      ]);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
    });
  });

  describe('onRemove', () => {
    it('should remove a value', () => {
      spyOn(FieldService, 'remove');

      $ctrl.onRemove(1);
      $scope.$digest();

      expect(FieldService.remove).toHaveBeenCalledWith({ name: 'test-name[2]' });
      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
      ]);
      expect($ctrl.form.$setDirty).toHaveBeenCalled();
    });

    it('should handle an error', () => {
      spyOn(FieldService, 'remove').and.returnValue($q.reject());

      $ctrl.onRemove(1);
      $scope.$digest();

      expect($ctrl.fieldValues).toEqual([
        jasmine.objectContaining({ chosenId: 'choice2' }),
        jasmine.objectContaining({ chosenId: 'choice1' }),
        jasmine.objectContaining({ chosenId: 'choice2' }),
      ]);
      expect($ctrl.form.$setDirty).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalled();
    });
  });
});
