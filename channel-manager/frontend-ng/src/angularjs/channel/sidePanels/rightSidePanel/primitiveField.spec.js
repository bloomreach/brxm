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

describe('CompoundField', () => {
  let $componentController;

  let $ctrl;
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

    inject((_$componentController_) => {
      $componentController = _$componentController_;
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
    expect($ctrl.getFieldName(1)).toBe('test-name/field:type[1]');
    expect($ctrl.getFieldName(2)).toBe('test-name/field:type[2]');

    const stubbed = $componentController('primitiveField', { }, {
      fieldType,
      fieldValues,
    });
    expect(stubbed.getFieldName(0)).toBe('field:type');
    expect(stubbed.getFieldName(1)).toBe('field:type[1]');
    expect(stubbed.getFieldName(2)).toBe('field:type[2]');
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
      'test-name/field:type[1]': {
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
});
