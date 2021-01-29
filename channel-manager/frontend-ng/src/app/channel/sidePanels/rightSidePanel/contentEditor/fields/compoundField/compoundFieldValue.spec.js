/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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

describe('CompoundFieldValue', () => {
  let $compile;
  let $ctrl;
  let $element;
  let $rootScope;
  let $scope;
  let collapse;
  let context;
  let fieldValue;
  let parent;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject((_$rootScope_, _$compile_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
    });

    $scope = $rootScope.$new();
    collapse = jasmine.createSpyObj('CollapseCtrl', ['open', 'collapse']);
    context = { expanded: new Set() };
    fieldValue = {};
    parent = {
      children: new Set(),
      setError: jasmine.createSpy('setError'),
    };
    const $parent = angular.element('<div><div compound-field-value="fieldValue"></div></div>');
    $element = $parent.find('[compound-field-value]');

    $element.data('$collapseController', collapse);
    $parent.data('$compoundFieldController', context);
    $parent.data('$compoundFieldValueController', parent);
    $scope.fieldValue = fieldValue;

    spyOn(context.expanded, 'add').and.callThrough();
    spyOn(context.expanded, 'delete').and.callThrough();
    spyOn(context.expanded, 'has').and.callThrough();
    spyOn(parent.children, 'add').and.callThrough();
    spyOn(parent.children, 'delete').and.callThrough();

    $compile($element)($scope);
    $scope.$digest();

    $ctrl = $element.controller('compoundFieldValue');
  });

  describe('$onInit', () => {
    it('initializes the component', () => {
      expect(parent.children.add).toHaveBeenCalledWith($ctrl);
    });
  });

  describe('$onDestroy', () => {
    it('destroys the component', () => {
      $ctrl.$onDestroy();

      expect(parent.children.delete).toHaveBeenCalledWith($ctrl);
    });
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

      expect(collapse.open).toHaveBeenCalled();
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

  describe('onDrag', () => {
    it('should collapse a field', () => {
      $scope.$broadcast('field:drag', context);

      expect(collapse.collapse).toHaveBeenCalled();
    });

    it('should store the expanded state', () => {
      $scope.$broadcast('field:drag', context);

      expect(context.expanded.add).toHaveBeenCalledWith(fieldValue);
    });

    it('should not store the expanded state if a field is already collapsed', () => {
      collapse.isCollapsed = true;
      $scope.$broadcast('field:drag', context);

      expect(context.expanded.add).not.toHaveBeenCalledWith(fieldValue);
    });
  });

  describe('onDrop', () => {
    it('should not expand a field', () => {
      collapse.isCollapsed = true;
      $scope.$broadcast('field:drag', context);
      $scope.$broadcast('field:drop', context);

      expect(collapse.open).not.toHaveBeenCalled();
    });

    it('should expand a field', () => {
      $scope.$broadcast('field:drag', context);
      $scope.$broadcast('field:drop', context);

      expect(collapse.open).toHaveBeenCalled();
    });
  });
});
