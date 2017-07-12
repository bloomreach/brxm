/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
  let FieldService;
  let onFieldFocus;
  let onFieldBlur;

  const fieldType = { id: 'field:type' };
  const fieldValues = [
    { value: 'Value 1' },
    { value: 'Value 2' },
    { value: 'Value 3' },
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$rootScope_, _FieldService_) => {
      $componentController = _$componentController_;
      FieldService = _FieldService_;
    });

    onFieldFocus = jasmine.createSpy('onFieldFocus');
    onFieldBlur = jasmine.createSpy('onFieldBlur');

    $ctrl = $componentController('primitiveField', {
    }, {
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
    expect($ctrl.getFieldName(0)).toBe('test-name/field:type');
    expect($ctrl.getFieldName(1)).toBe('test-name/field:type[2]');
    expect($ctrl.getFieldName(2)).toBe('test-name/field:type[3]');

    const stubbed = $componentController('primitiveField', { }, {
      fieldType,
      fieldValues,
    });
    expect(stubbed.getFieldName(0)).toBe('field:type');
    expect(stubbed.getFieldName(1)).toBe('field:type[2]');
    expect(stubbed.getFieldName(2)).toBe('field:type[3]');
  });

  it('returns the form error object if a single field is invalid', () => {
    const error = {
      required: true,
    };
    $ctrl.form = {
      'test-name/field:type': {
        $error: error,
      },
    };
    expect($ctrl.getFieldError()).toEqual(error);
  });

  it('returns the combined form error object if multiple fields are invalid', () => {
    $ctrl.form = {
      'test-name/field:type': {
        $error: {
          required: true,
        },
      },
      'test-name/field:type[2]': {
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
    $ctrl = $componentController('primitiveField', {
    }, {
      fieldType,
      fieldValues: [],
      name: 'test-name',
    });

    expect($ctrl.getFieldError()).toEqual(null);
  });

  it('knowns whether a single field is valid', () => {
    $ctrl.form = {
      'test-name/field:type': {
        $invalid: false,
      },
    };
    expect($ctrl.isValid()).toBe(true);
  });

  it('knowns whether a single field is invalid', () => {
    $ctrl.form = {
      'test-name/field:type': {
        $invalid: true,
      },
    };
    expect($ctrl.isValid()).toBe(false);
  });

  it('knowns whether a multiple field is valid', () => {
    $ctrl.form = {
      'test-name/field:type': {
        $invalid: false,
      },
      'test-name/field:type[2]': {
        $invalid: false,
      },
    };
    expect($ctrl.isValid()).toBe(true);
  });

  it('knowns whether a multiple field is invalid', () => {
    $ctrl.form = {
      'test-name/field:type': {
        $invalid: false,
      },
      'test-name/field:type[2]': {
        $invalid: true,
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

    spyOn(FieldService, 'setFocusedInput');
    spyOn(FieldService, 'shouldUnsetFocus').and.returnValue(false);
    spyOn(FieldService, 'unsetFocusedInput');

    $ctrl.focusPrimitive($event);

    $event.target.triggerHandler('blur.focusedInputBlurHandler');
    expect(FieldService.setFocusedInput).toHaveBeenCalledWith($event.target, $event.customFocus);
    expect(FieldService.shouldUnsetFocus).toHaveBeenCalled();
    expect(FieldService.unsetFocusedInput).toHaveBeenCalled();
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

  it('starts a draft timer when the value changed', () => {
    spyOn(FieldService, 'startDraftTimer');

    $ctrl.valueChanged();

    expect(FieldService.startDraftTimer).toHaveBeenCalledWith('test-name/field:type', fieldValues);
  });

  it('drafts the field on blur when the value has changed', () => {
    spyOn(FieldService, 'draftField');

    $ctrl.focusPrimitive();
    fieldValues[1].value = 'Changed';
    $ctrl.blurPrimitive();

    expect(FieldService.draftField).toHaveBeenCalledWith('test-name/field:type', fieldValues);
  });

  it('does not draft the field on blur when the value has not changed', () => {
    spyOn(FieldService, 'draftField');

    $ctrl.focusPrimitive();
    $ctrl.blurPrimitive();

    expect(FieldService.draftField).not.toHaveBeenCalled();
  });
});
