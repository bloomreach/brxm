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

describe('ChoiceField', () => {
  let $componentController;

  let $ctrl;
  let onFieldFocus;
  let onFieldBlur;

  const choiceType = {
    displayName: 'Choice',
    required: true,
    hint: 'bla bla',
    choices: {
      choice1: {
        id: 'choice1',
        fields: [],
      },
      choice2: {
        id: 'choice2',
        fields: [],
      },
    },
  };

  const choiceValues = [
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

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    onFieldFocus = jasmine.createSpy('onFieldFocus');
    onFieldBlur = jasmine.createSpy('onFieldBlur');

    $ctrl = $componentController('choiceField', {
    }, {
      fieldType: choiceType,
      fieldValues: choiceValues,
      name: 'test-name',
      onFieldFocus,
      onFieldBlur,
    });
  });

  it('initializes the fields component', () => {
    expect($ctrl.fieldType).toBe(choiceType);
    expect($ctrl.fieldValues).toBe(choiceValues);
    expect($ctrl.name).toBe('test-name');
    expect($ctrl.onFieldFocus).toBe(onFieldFocus);
    expect($ctrl.onFieldBlur).toBe(onFieldBlur);
  });

  it('keeps track of the focused state', () => {
    expect($ctrl.hasFocus).toBeFalsy();

    $ctrl.focusChoice();

    expect($ctrl.hasFocus).toBeTruthy();
    expect(onFieldFocus).toHaveBeenCalled();
    expect(onFieldBlur).not.toHaveBeenCalled();
    onFieldFocus.calls.reset();

    $ctrl.blurChoice();

    expect($ctrl.hasFocus).toBeFalsy();
    expect(onFieldFocus).not.toHaveBeenCalled();
    expect(onFieldBlur).toHaveBeenCalled();
  });

  it('helps composing unique form field names', () => {
    expect($ctrl.getFieldName(0)).toBe('test-name');
    expect($ctrl.getFieldName(1)).toBe('test-name[2]');
    expect($ctrl.getFieldName(2)).toBe('test-name[3]');
  });
});
