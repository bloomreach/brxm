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
  const textField = {
    id: 'ns:text',
    type: 'MULTILINE_STRING',
  };
  const requiredTextField = {
    id: 'ns:requiredtext',
    type: 'MULTILINE_STRING',
    validators: [
      'REQUIRED',
    ],
  };
  const nestedString = {
    id: 'ns:nestedstring',
    type: 'STRING',
  };
  const nestedTextCompound = {
    id: 'ns:nestedtextcompound',
    type: 'COMPOUND',
    fields: [textField, requiredTextField],
  };
  const nestedCompound = {
    id: 'ns:nestedcompound',
    type: 'COMPOUND',
    fields: [nestedString, nestedTextCompound],
  };
  const testDocumentType = {
    id: 'ns:testdocument',
    fields: [
      stringField,
      multipleStringField,
      emptyMultipleStringField,
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
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$rootScope_) => {
      $componentController = _$componentController_;
      $rootScope = _$rootScope_;
    });

    $ctrl = $componentController('channelFields', {
    }, {
      fieldTypes: testDocumentType,
      fieldValues: testDocument.fields,
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

  it('can check if a field without child fields does not execute hasFocusedField', () => {
    expect($ctrl.hasFocusedField(stringField)).toBeFalsy();
    expect($ctrl.hasFocusedField(multipleStringField)).toBeFalsy();
  });

  it('can check if a field has a focused element', () => {
    expect($ctrl.hasFocusedField(nestedCompound)).toBeFalsy();
    $ctrl.onFieldFocus(nestedString);
    expect($ctrl.hasFocusedField(nestedCompound)).toBeTruthy();
    nestedString.focused = false; // reset state
  });

  it('can check if a field has a focused child element', () => {
    expect($ctrl.hasFocusedField(nestedCompound)).toBeFalsy();
    $ctrl.onFieldFocus(textField);
    expect($ctrl.hasFocusedField(nestedCompound)).toBeTruthy();
    textField.focused = false; // reset state
  });

  it('can set a field to focus', () => {
    expect(stringField.focused).toBeFalsy();
    $ctrl.onFieldFocus(stringField);
    expect(stringField.focused).toBeTruthy();
    stringField.focused = false; // reset state
  });

  it('can set a field to unfocus', () => {
    stringField.focused = true;
    $ctrl.onFieldBlur(stringField);
    expect(stringField.focused).toBeFalsy();
    stringField.focused = false; // reset state
  });
});
