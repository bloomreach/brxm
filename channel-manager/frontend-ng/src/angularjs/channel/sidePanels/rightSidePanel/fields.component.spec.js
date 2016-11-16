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

    $ctrl = $componentController('channelFields', {
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

  it('enumerates multiple compound fields', () => {
    const field = {
      id: 'multiple-compounds',
      displayName: 'Compound Name',
    };

    expect($ctrl.getDisplayNameForCompound(field, 3)).toBe('Compound Name'); // no value
    testDocument.fields['multiple-compounds'] = ['bla'];
    expect($ctrl.getDisplayNameForCompound(field, 3)).toBe('Compound Name'); // single value
    testDocument.fields['multiple-compounds'] = ['bla', 'bli'];
    expect($ctrl.getDisplayNameForCompound(field, 0)).toBe('Compound Name (1)'); // multiple values, enumerate
    expect($ctrl.getDisplayNameForCompound(field, 3)).toBe('Compound Name (4)'); // multiple values, enumerate
  });

  it('keeps track of the compound with the focused field', () => {
    $ctrl.focusCompound('ns:compound', 0);

    expect(onFieldFocus).toHaveBeenCalled();

    // include the index of the compound to distinguish 'multiple' compounds
    expect($ctrl.hasFocusedField('ns:compound', 0)).toBe(true);
    expect($ctrl.hasFocusedField('ns:compound', 1)).toBe(false);
    expect($ctrl.hasFocusedField('ns:string', 0)).toBe(false);

    $ctrl.blurCompound();
    expect(onFieldBlur).toHaveBeenCalled();
    expect($ctrl.hasFocusedField('ns:compound', 0)).toBe(false);
  });

  it('ignores the onFieldFocus and onFieldBlur callbacks when they are not defined', () => {
    expect(() => {
      $ctrl = $componentController('channelFields', {}, {
        fieldTypes: testDocumentType,
        fieldValues: testDocument.fields,
      });
      $ctrl.$onInit();
      $ctrl.onFieldFocus();
      $ctrl.onFieldBlur();
    }).not.toThrow();
  });
});
