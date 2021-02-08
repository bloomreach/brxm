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

describe('PrimitiveField', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $rootScope;
  let $q;
  let FieldService;
  let onFieldFocus;
  let onFieldBlur;

  let fieldType;
  let fieldValues;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject((_$componentController_, _$q_, _$rootScope_, _FieldService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      FieldService = _FieldService_;

      spyOn(FieldService, 'save').and.returnValue($q.resolve());
    });

    onFieldFocus = jasmine.createSpy('onFieldFocus');
    onFieldBlur = jasmine.createSpy('onFieldBlur');

    fieldType = {};
    fieldValues = [
      { value: 'Value 1' },
      { value: 'Value 2' },
      { value: 'Value 3' },
    ];

    $element = angular.element('<div>');
    $ctrl = $componentController('primitiveField', { $element }, {
      fieldType,
      fieldValues,
      name: 'test-name',
      onFieldFocus,
      onFieldBlur,
    });
  });

  it('initializes the component', () => {
    expect($ctrl.fieldType).toBe(fieldType);
    expect($ctrl.fieldValues).toBe(fieldValues);
    expect($ctrl.name).toBe('test-name');
    expect($ctrl.onFieldFocus).toBe(onFieldFocus);
    expect($ctrl.onFieldBlur).toBe(onFieldBlur);
  });

  it('helps composing unique form field names', () => {
    expect($ctrl.getFieldName(0)).toBe('test-name');
    expect($ctrl.getFieldName(1)).toBe('test-name[2]');
    expect($ctrl.getFieldName(2)).toBe('test-name[3]');
  });

  it('returns the form error object if a single field is invalid', () => {
    const error = {
      required: true,
    };
    $ctrl.form = {
      'test-name': {
        $error: error,
      },
    };
    expect($ctrl.getFieldError()).toEqual(error);
  });

  it('returns the combined form error object if multiple fields are invalid', () => {
    $ctrl.form = {
      'test-name': {
        $error: {
          required: true,
        },
      },
      'test-name[2]': {
        $error: {
          maxlength: true,
        },
      },
    };
    expect($ctrl.getFieldError()).toEqual({
      required: true,
      maxlength: true,
    });
  });

  it('returns null as form error object if the form has no corresponding field', () => {
    $ctrl.form = {};
    expect($ctrl.getFieldError()).toEqual(null);
  });

  it('returns null as form error object for a field without any values', () => {
    $ctrl = $componentController('primitiveField', { $element }, {
      fieldType,
      fieldValues: [],
      name: 'test-name',
    });

    expect($ctrl.getFieldError()).toEqual(null);
  });

  it('knowns whether a single field is valid', () => {
    $ctrl.form = {
      'test-name': {
        $invalid: false,
      },
    };
    expect($ctrl.isValid()).toBe(true);
  });

  it('knowns whether a single field is invalid', () => {
    $ctrl.form = {
      'test-name': {
        $invalid: true,
        $touched: true,
      },
    };
    expect($ctrl.isValid()).toBe(false);
  });

  it('knowns whether a multiple field is valid', () => {
    $ctrl.form = {
      'test-name': {
        $invalid: false,
        $touched: true,
      },
      'test-name[2]': {
        $invalid: true,
        $touched: false,
      },
    };
    expect($ctrl.isValid()).toBe(true);
  });

  it('knowns whether a multiple field is invalid', () => {
    $ctrl.form = {
      'test-name': {
        $invalid: false,
        $touched: false,
      },
      'test-name[2]': {
        $invalid: true,
        $touched: true,
      },
    };
    expect($ctrl.isValid()).toBe(false);
  });

  it('assumes that a field without any value is valid', () => {
    $ctrl.fieldValues = [];
    expect($ctrl.isValid()).toBe(true);
  });

  it('handles $event object if supplied upon focus', () => {
    const mockTargetElement = $('<input type="text">');
    const $event = {
      target: mockTargetElement,
      customFocus: null,
    };
    spyOn(FieldService, 'unsetFocusedInput');
    spyOn(FieldService, 'setFocusedInput');

    $ctrl.focusPrimitive($event);
    expect(FieldService.unsetFocusedInput).toHaveBeenCalledWith();
    expect(FieldService.setFocusedInput).toHaveBeenCalledWith($event.target, $event.customFocus);
  });

  it('handles $event object if supplied upon blur', () => {
    const $event = {
      relatedTarget: angular.element('<input type="text">'),
    };
    spyOn(FieldService, 'shouldPreserveFocus').and.returnValue(true);
    spyOn(FieldService, 'triggerInputFocus');

    $ctrl.blurPrimitive($event);
    expect(FieldService.shouldPreserveFocus).toHaveBeenCalledWith($event.target);
    expect(FieldService.triggerInputFocus).toHaveBeenCalled();

    // if should not preserve focus
    FieldService.shouldPreserveFocus.and.returnValue(false);
    $ctrl.blurPrimitive($event);
  });

  it('keeps track of the focused state', () => {
    expect($ctrl.hasFocus).toBeFalsy();

    $ctrl.focusPrimitive();

    expect($ctrl.hasFocus).toBeTruthy();
    expect(onFieldFocus).toHaveBeenCalled();
    expect(onFieldBlur).not.toHaveBeenCalled();
    onFieldFocus.calls.reset();

    $ctrl.blurPrimitive();

    expect($ctrl.hasFocus).toBeFalsy();
    expect(onFieldFocus).not.toHaveBeenCalled();
    expect(onFieldBlur).toHaveBeenCalled();
  });

  it('should move value', () => {
    const validatedValues = angular.copy(fieldValues);
    FieldService.save.and.returnValue($q.resolve(validatedValues));

    $ctrl.onMove(0, 2);
    $rootScope.$digest();

    expect(FieldService.save).toHaveBeenCalledWith({
      name: 'test-name',
      values: [
        { value: 'Value 2' },
        { value: 'Value 3' },
        { value: 'Value 1' },
      ],
    });
  });

  it('should remove value', () => {
    const validatedValues = angular.copy(fieldValues);
    FieldService.save.and.returnValue($q.resolve(validatedValues));

    $ctrl.onRemove(1);
    $rootScope.$digest();

    expect(FieldService.save).toHaveBeenCalledWith({
      name: 'test-name',
      values: [
        { value: 'Value 1' },
        { value: 'Value 3' },
      ],
    });
  });

  it('should add value', () => {
    $ctrl.form = { $setDirty: jasmine.createSpy('$setDirty') };
    $ctrl.onAdd();
    $rootScope.$digest();

    expect($ctrl.fieldValues).toEqual([
      { value: 'Value 1' },
      { value: 'Value 2' },
      { value: 'Value 3' },
      { value: '' },
    ]);
    expect($ctrl.form.$setDirty).toHaveBeenCalled();
  });

  describe('valueChanged', () => {
    let field;

    beforeEach(() => {
      field = {
        $invalid: false,
        $setValidity() {},
        $setTouched() {},
      };
      $ctrl.form = { 'test-name': field };
    });

    it('starts a save timer when the value changed', () => {
      $ctrl.valueChanged();
      expect(FieldService.save)
        .toHaveBeenCalledWith({ name: 'test-name', values: fieldValues, throttle: true });
    });

    it('sets server errors when the auto-saved value contains errorInfo objects', () => {
      const validatedValues = angular.copy(fieldValues);
      validatedValues[0].errorInfo = {
        message: 'First error',
      };
      validatedValues[2].errorInfo = {
        message: 'Second error',
      };
      FieldService.save.and.returnValue($q.resolve(validatedValues));
      spyOn(field, '$setTouched');
      spyOn(field, '$setValidity');

      $ctrl.valueChanged();
      $rootScope.$digest();

      expect(FieldService.save)
        .toHaveBeenCalledWith({ name: 'test-name', values: fieldValues, throttle: true });
      expect(field.$setTouched).toHaveBeenCalled();
      expect(field.$setValidity).toHaveBeenCalledWith('server', false);
      expect($ctrl.firstServerError).toBe('First error');
    });

    it('removes server errors when the auto-saved value does not contain errorInfo objects', () => {
      const validatedValues = angular.copy(fieldValues);
      FieldService.save.and.returnValue($q.resolve(validatedValues));
      spyOn(field, '$setValidity');

      fieldValues[1].errorInfo = { message: '"Error' };
      $ctrl.firstServerError = 'Error';
      $ctrl.valueChanged();
      $rootScope.$digest();

      expect(FieldService.save)
        .toHaveBeenCalledWith({ name: 'test-name', values: fieldValues, throttle: true });
      expect(field.$setValidity).toHaveBeenCalledWith('server', true);
      expect($ctrl.firstServerError).toBeUndefined();
    });

    it('marks the field as touched', () => {
      FieldService.save.and.returnValue($q.resolve(angular.copy(fieldValues)));
      spyOn(field, '$setTouched');

      $ctrl.valueChanged();
      $rootScope.$digest();

      expect(field.$setTouched).toHaveBeenCalled();
    });
  });

  it('saves the field on blur when the value has changed', () => {
    const validatedValues = angular.copy(fieldValues); // values without errorInfo objects
    FieldService.save.and.returnValue($q.resolve(validatedValues));

    $ctrl.focusPrimitive();

    fieldValues[1].value = 'Changed';
    const expectedFieldValues = angular.copy(fieldValues);

    $ctrl.blurPrimitive();
    $rootScope.$digest();

    expect(FieldService.save).toHaveBeenCalledWith({ name: 'test-name', values: fieldValues });
    expect($ctrl.fieldValues).toEqual(expectedFieldValues);
  });

  it('does not save the field on blur when the value has not changed', () => {
    $ctrl.focusPrimitive();
    $ctrl.blurPrimitive();

    expect(FieldService.save).not.toHaveBeenCalled();
  });

  it('broadcasts event "primitive-field:focus" when clicking on the field label', () => {
    spyOn($ctrl.$scope, '$broadcast');
    const $event = { preventDefault: angular.noop };
    $ctrl.onLabelClick($event);

    expect($ctrl.$scope.$broadcast).toHaveBeenCalledWith('primitive-field:focus', $event);
  });

  describe('$onInit', () => {
    beforeEach(() => {
      $ctrl.form = {
        field1: {
          $setTouched: jasmine.createSpy(),
          $setValidity: jasmine.createSpy(),
          $error: {
            server: false,
          },
        },
        field2: {
          $setTouched: jasmine.createSpy(),
          $setValidity: jasmine.createSpy(),
          $error: {
            server: true,
          },
        },
      };
    });

    it('makes form field invalid', () => {
      spyOn($ctrl, 'getFieldName').and.returnValue('field1');

      $ctrl.$onInit();
      $ctrl.fieldValues = [{
        errorInfo: { message: 'error message' },
      }];
      $rootScope.$digest();

      expect($ctrl.getFieldName).toHaveBeenCalled();
      expect($ctrl.form.field1.$setTouched).toHaveBeenCalled();
      expect($ctrl.form.field1.$setValidity).toHaveBeenCalledWith('server', false);
      expect($ctrl.firstServerError).toBe('error message');
    });

    it('makes form field valid', () => {
      spyOn($ctrl, 'getFieldName').and.returnValue('field2');

      $ctrl.$onInit();
      $ctrl.fieldValues = [{}];
      $rootScope.$digest();

      expect($ctrl.getFieldName).toHaveBeenCalled();
      expect($ctrl.form.field1.$setTouched).not.toHaveBeenCalled();
      expect($ctrl.form.field2.$setValidity).toHaveBeenCalledWith('server', true);
      expect($ctrl.firstServerError).toBeUndefined();
    });
  });
});
