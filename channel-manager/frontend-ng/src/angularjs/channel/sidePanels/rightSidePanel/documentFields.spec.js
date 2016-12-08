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

describe('ChannelFields', () => {
  let $componentController;
  let $rootScope;

  let $ctrl;
  let onFieldFocus;
  let onFieldBlur;

  const stringField = {
    id: 'ns:string',
    type: 'STRING',
  };
  const multipleStringField = {
    id: 'ns:multiplestring',
    type: 'STRING',
  };
  const emptyMultipleStringField = {
    id: 'ns:emptymultiplestring',
    type: 'STRING',
  };
  const compoundField = {
    id: 'ns:compound',
    type: 'COMPOUND',
    fields: [
      stringField,
    ],
  };
  const testDocumentType = {
    id: 'ns:testdocument',
    fields: [
      stringField,
      multipleStringField,
      emptyMultipleStringField,
      compoundField,
    ],
  };
  const testDocument = {
    id: 'test',
    info: {
      type: {
        id: 'ns:testdocument',
      },
      editing: {
        state: 'AVAILABLE',
      },
    },
    fields: {
      'ns:string': ['String value'],
      'ns:multiplestring': ['One', 'Two'],
      'ns:emptymultiplestring': [],
      'ns:compound': [
        {
          'ns:string': 'String value in compound',
        },
      ],
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$rootScope_) => {
      $componentController = _$componentController_;
      $rootScope = _$rootScope_;
    });

    onFieldFocus = jasmine.createSpy('onFieldFocus');
    onFieldBlur = jasmine.createSpy('onFieldBlur');

    $ctrl = $componentController('documentFields', {
    }, {
      fieldTypes: testDocumentType,
      fieldValues: testDocument.fields,
      onFieldFocus,
      onFieldBlur,
    });
    $rootScope.$apply();
  });

  it('initializes the fields component', () => {
    expect($ctrl.fieldTypes).toBe(testDocumentType);
    expect($ctrl.fieldValues).toBe(testDocument.fields);
  });

  it('recognizes an empty multiple field', () => {
    expect($ctrl.hasValue(stringField)).toBe(true);
    expect($ctrl.hasValue(multipleStringField)).toBe(true);
    expect($ctrl.hasValue(emptyMultipleStringField)).toBe(false);

    testDocument.fields.invalid = 'not an array';
    expect($ctrl.hasValue({ id: 'invalid' })).toBe(false);
  });

  it('keeps track of the compound with the focused field', () => {
    const someField = { test: 'bla' };
    $ctrl.focusCompound(someField);

    expect(onFieldFocus).toHaveBeenCalled();

    expect($ctrl.hasFocusedField(someField)).toBe(true);
    expect($ctrl.hasFocusedField({ test: 'bla' })).toBe(false); // different object
    expect($ctrl.hasFocusedField()).toBe(false);

    $ctrl.blurCompound();
    expect(onFieldBlur).toHaveBeenCalled();
    expect($ctrl.hasFocusedField(someField)).toBe(false);
  });

  it('keeps track of the field type with the focused field', () => {
    const someFieldType = { test: 'bla' };
    $ctrl.focusFieldType(someFieldType);

    expect(onFieldFocus).toHaveBeenCalled();

    expect($ctrl.hasFocusedFieldType(someFieldType)).toBe(true);
    expect($ctrl.hasFocusedFieldType({ test: 'bla' })).toBe(false); // different object
    expect($ctrl.hasFocusedFieldType()).toBe(false);

    $ctrl.blurFieldType();
    expect(onFieldBlur).toHaveBeenCalled();
    expect($ctrl.hasFocusedFieldType(someFieldType)).toBe(false);
  });

  it('ignores the onFieldFocus and onFieldBlur callbacks when they are not defined', () => {
    expect(() => {
      $ctrl = $componentController('documentFields', {}, {
        fieldTypes: testDocumentType,
        fieldValues: testDocument.fields,
      });
      $ctrl.$onInit();
      $ctrl.onFieldFocus();
      $ctrl.onFieldBlur();
    }).not.toThrow();
  });
});
