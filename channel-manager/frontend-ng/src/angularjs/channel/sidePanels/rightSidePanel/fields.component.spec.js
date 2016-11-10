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

    jasmine.getFixtures().load('channel/sidePanels/rightSidePanel/fields.component.fixture.html');

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

  const parentComponentGetsClassAdded = (focusedParents, unFocusedParents, field) => {
    for (const element of focusedParents) {
      expect(element).not.toHaveClass('has-focused-element');
    }
    $ctrl.onFieldFocus({
      target: field,
    });
    for (const element of focusedParents) {
      expect(element).toHaveClass('has-focused-element');
      element.removeClass('has-focused-element');
    }
    for (const element of unFocusedParents) {
      expect(element).not.toHaveClass('has-focused-element');
    }
  };
  const parentComponentGetsClassRemoved = (focusedParents, unFocusedParents, field) => {
    for (const element of unFocusedParents) {
      element.addClass('has-focused-element');
    }
    $ctrl.onFieldBlur({
      target: field,
    });
    for (const element of focusedParents) {
      expect(element).not.toHaveClass('has-focused-element');
    }
    for (const element of unFocusedParents) {
      expect(element).not.toHaveClass('has-focused-element');
    }
  };

  // standard compound field
  it('can find a parent compound and add the appropriate class', () => {
    const stringCompound = $j('#string_compound');

    parentComponentGetsClassAdded([stringCompound], [], $j('#input_17'));
    parentComponentGetsClassAdded([stringCompound], [], $j('#input_18'));
    parentComponentGetsClassAdded([stringCompound], [], $j('#input_19'));
    parentComponentGetsClassAdded([stringCompound], [], $j('#input_20'));
    parentComponentGetsClassAdded([stringCompound], [], $j('#input_21'));
  });
  it('can find a parent compound and remove the appropriate class', () => {
    const stringCompound = $j('#string_compound');

    parentComponentGetsClassRemoved([], [stringCompound], $j('#input_17'));
    parentComponentGetsClassRemoved([], [stringCompound], $j('#input_18'));
    parentComponentGetsClassRemoved([], [stringCompound], $j('#input_19'));
    parentComponentGetsClassRemoved([], [stringCompound], $j('#input_20'));
    parentComponentGetsClassRemoved([], [stringCompound], $j('#input_21'));
  });

  // multiple compound field
  it('can find a parent compound when using a multiple compound and add the appropriate class', () => {
    const multipleStringCompoundOne = $j('#multiple_string_compound_1');
    const multipleStringCompoundTwo = $j('#multiple_string_compound_2');

    parentComponentGetsClassAdded([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_27'));
    parentComponentGetsClassAdded([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_28'));
    parentComponentGetsClassAdded([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_29'));
    parentComponentGetsClassAdded([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_30'));
    parentComponentGetsClassAdded([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_31'));

    parentComponentGetsClassAdded([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_32'));
    parentComponentGetsClassAdded([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_33'));
    parentComponentGetsClassAdded([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_34'));
    parentComponentGetsClassAdded([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_35'));
    parentComponentGetsClassAdded([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_36'));
  });
  it('can find a parent compound when using a multiple compound and remove the appropriate class', () => {
    const multipleStringCompoundOne = $j('#multiple_string_compound_1');
    const multipleStringCompoundTwo = $j('#multiple_string_compound_2');

    parentComponentGetsClassRemoved([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_27'));
    parentComponentGetsClassRemoved([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_28'));
    parentComponentGetsClassRemoved([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_29'));
    parentComponentGetsClassRemoved([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_30'));
    parentComponentGetsClassRemoved([multipleStringCompoundTwo], [multipleStringCompoundOne], $j('#input_31'));

    parentComponentGetsClassRemoved([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_32'));
    parentComponentGetsClassRemoved([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_33'));
    parentComponentGetsClassRemoved([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_34'));
    parentComponentGetsClassRemoved([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_35'));
    parentComponentGetsClassRemoved([multipleStringCompoundOne], [multipleStringCompoundTwo], $j('#input_36'));
  });

  // nested compound field
  it('can find a nested parent compound and add the appropriate class', () => {
    const nestedCompound = $j('#nested_compound');
    const nestedTextCompound = $j('#nested_text_compound');
    const optionalRecursiveCompound = $j('#optional_recursive_compound');
    const nestedSecondTextCompound = $j('#nested_second_text_compound');

    parentComponentGetsClassAdded([nestedCompound], [nestedTextCompound, optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_37'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_38'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_39'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_40'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_41'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_42'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_43'));
    parentComponentGetsClassAdded([nestedCompound, nestedTextCompound], [optionalRecursiveCompound, nestedSecondTextCompound], $j('#input_44'));

    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound], [nestedTextCompound, nestedSecondTextCompound], $j('#input_45'));
    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound, nestedSecondTextCompound], [nestedTextCompound], $j('#input_46'));
    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound, nestedSecondTextCompound], [nestedTextCompound], $j('#input_47'));
    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound, nestedSecondTextCompound], [nestedTextCompound], $j('#input_48'));
    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound, nestedSecondTextCompound], [nestedTextCompound], $j('#input_49'));
    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound, nestedSecondTextCompound], [nestedTextCompound], $j('#input_50'));
    parentComponentGetsClassAdded([nestedCompound, optionalRecursiveCompound], [nestedTextCompound, nestedSecondTextCompound], $j('#input_51'));
  });
  it('can find a nested parent compound and remove the appropriate class', () => {
    const nestedCompound = $j('#nested_compound');
    const nestedTextCompound = $j('#nested_text_compound');
    const optionalRecursiveCompound = $j('#optional_recursive_compound');
    const nestedSecondTextCompound = $j('#nested_second_text_compound');

    parentComponentGetsClassRemoved([nestedTextCompound, optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound], $j('#input_37'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_38'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_39'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_40'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_41'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_42'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_43'));
    parentComponentGetsClassRemoved([optionalRecursiveCompound, nestedSecondTextCompound], [nestedCompound, nestedTextCompound], $j('#input_44'));

    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_45'));
    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_46'));
    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_47'));
    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_48'));
    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_49'));
    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_50'));
    parentComponentGetsClassRemoved([nestedTextCompound, nestedSecondTextCompound], [nestedCompound, optionalRecursiveCompound], $j('#input_51'));
  });
});
