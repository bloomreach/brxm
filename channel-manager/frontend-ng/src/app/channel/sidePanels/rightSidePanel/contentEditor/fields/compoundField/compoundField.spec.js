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

describe('CompoundField', () => {
  let $componentController;

  let $ctrl;
  let onFieldFocus;
  let onFieldBlur;

  const dummyType = { };
  const dummyValues = [];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    onFieldFocus = jasmine.createSpy('onFieldFocus');
    onFieldBlur = jasmine.createSpy('onFieldBlur');

    $ctrl = $componentController('compoundField', {
    }, {
      fieldType: dummyType,
      fieldValues: dummyValues,
      name: 'test-name',
      onFieldFocus,
      onFieldBlur,
    });
  });

  it('initializes the component', () => {
    expect($ctrl.fieldType).toBe(dummyType);
    expect($ctrl.fieldValues).toBe(dummyValues);
    expect($ctrl.name).toBe('test-name');
    expect($ctrl.onFieldFocus).toBe(onFieldFocus);
    expect($ctrl.onFieldBlur).toBe(onFieldBlur);
  });

  it('keeps track of the focused state', () => {
    expect($ctrl.hasFocus).toBeFalsy();

    $ctrl.focusCompound();

    expect($ctrl.hasFocus).toBeTruthy();
    expect(onFieldFocus).toHaveBeenCalled();
    expect(onFieldBlur).not.toHaveBeenCalled();
    onFieldFocus.calls.reset();

    $ctrl.blurCompound();

    expect($ctrl.hasFocus).toBeFalsy();
    expect(onFieldFocus).not.toHaveBeenCalled();
    expect(onFieldBlur).toHaveBeenCalled();
  });
});
