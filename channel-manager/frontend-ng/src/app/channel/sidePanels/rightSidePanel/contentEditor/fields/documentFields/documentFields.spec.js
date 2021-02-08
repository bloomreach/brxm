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

describe('DocumentFields', () => {
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
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

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
    $rootScope.$digest();
  });

  it('initializes the fields component', () => {
    expect($ctrl.fieldTypes).toBe(testDocumentType);
    expect($ctrl.fieldValues).toBe(testDocument.fields);
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

  it('generates names for root-level fields', () => {
    expect($ctrl.getFieldName(stringField)).toEqual('ns:string');
    expect($ctrl.getFieldName(multipleStringField)).toEqual('ns:multiplestring');
    expect($ctrl.getFieldName(compoundField)).toEqual('ns:compound');
  });

  it('generates names for nested fields', () => {
    $ctrl = $componentController('documentFields', {
    }, {
      name: 'ns:compound',
      fieldTypes: compoundField,
      fieldValues: [stringField],
    });
    expect($ctrl.getFieldName(stringField)).toEqual('ns:compound/ns:string');
  });

  it('generates a unique hash per field type based on the ID and validators of a field', () => {
    expect($ctrl.getFieldTypeHash({ id: 'hap:title' })).toEqual('hap:title:undefined');
    expect($ctrl.getFieldTypeHash({ id: 'hap:title', validators: ['REQUIRED'] })).toEqual('hap:title:REQUIRED');
    expect($ctrl.getFieldTypeHash({ id: 'hap:title', validators: ['REQUIRED', 'OTHER'] }))
      .toEqual('hap:title:REQUIRED,OTHER');
  });
});
