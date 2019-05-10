/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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
  let $element;
  let $scope;
  let parent;

  const fieldType = {};
  const fieldValue = [];

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    parent = {
      children: new Set(),
      setError: jasmine.createSpy('setError'),
    };
    $scope = { collapse: jasmine.createSpyObj('CollapseCtrl', ['open']) };
    $element = angular.element('<div>');
    $ctrl = $componentController('compoundField', { $element, $scope }, {
      fieldType,
      fieldValue,
      parent,
      name: 'test-name',
    });
  });

  it('initializes the component', () => {
    spyOn(parent.children, 'add');
    $ctrl.$onInit();

    expect($ctrl.fieldType).toBe(fieldType);
    expect($ctrl.fieldValue).toBe(fieldValue);
    expect($ctrl.name).toBe('test-name');
    expect(parent.children.add).toHaveBeenCalledWith($ctrl);
  });

  it('destroys the component', () => {
    spyOn(parent.children, 'delete');
    $ctrl.$onDestroy();

    expect(parent.children.delete).toHaveBeenCalledWith($ctrl);
  });

  it('focuses the component', () => {
    const focusHandler = jasmine.createSpy('onFocus');
    $element.on('focus', focusHandler);

    $ctrl.onFocus();

    expect($ctrl.hasFocus).toBeTruthy();
    expect(focusHandler).toHaveBeenCalled();
  });

  it('blurs the component', () => {
    const blurHandler = jasmine.createSpy('onBlur');
    $element.on('blur', blurHandler);

    $ctrl.onBlur();

    expect($ctrl.hasFocus).toBeFalsy();
    expect(blurHandler).toHaveBeenCalled();
  });

  describe('$onChanges', () => {
    beforeEach(() => {
      spyOn($ctrl, 'setError');
    });

    it('sets the error state', () => {
      $ctrl.$onChanges({ fieldValue: { currentValue: { errorInfo: {} } } });

      expect($ctrl.setError).toHaveBeenCalledWith(true);
    });

    it('unsets the error state', () => {
      $ctrl.$onChanges({ fieldValue: { currentValue: { } } });

      expect($ctrl.setError).toHaveBeenCalledWith(false);
    });
  });

  describe('setError', () => {
    it('sets the error flag', () => {
      $ctrl.setError(true);
      expect($ctrl.hasError).toBe(true);

      $ctrl.setError(false);
      expect($ctrl.hasError).toBe(false);
    });

    it('opens the collapsible block', () => {
      $ctrl.setError(true);

      expect($scope.collapse.open).toHaveBeenCalled();
    });

    it('keeps parent error state', () => {
      parent.children.add({ hasError: true }, $ctrl);
      $ctrl.setError(false);

      expect(parent.setError).toHaveBeenCalledWith(true);
    });

    it('updates parent error state', () => {
      parent.children.add({ hasError: false }, $ctrl);
      $ctrl.setError(false);

      expect(parent.setError).toHaveBeenCalledWith(false);
    });
  });
});
