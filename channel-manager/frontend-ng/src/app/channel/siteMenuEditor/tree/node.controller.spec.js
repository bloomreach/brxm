/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('TreeNodeCtrl', () => {
  let $element;
  let $scope;
  let outerScope;
  let $ctrl;
  let hippoTree;

  beforeEach(() => {
    angular.mock.module('hippo-cm.ui.tree');

    inject(($controller, _$rootScope_) => {
      hippoTree = {
        renderTreeTemplate: (scope, callback) => callback('dom'),
      };
      $element = angular.element('<div></div>');

      outerScope = _$rootScope_.$new();
      outerScope.something = 'data';
      const transcludedScope = outerScope.$new();
      const hippoTreeScope = transcludedScope.$new();
      hippoTreeScope.hippoTree = hippoTree;
      $scope = hippoTreeScope.$new();
      _$rootScope_.$digest();

      $ctrl = $controller('HippoTreeNodeCtrl', { $element, $scope });
    });
  });

  describe('$onInit', () => {
    it('should create new scope', () => {
      spyOn(outerScope, '$new');
      $ctrl._getOuterScope = () => outerScope;
      $ctrl.$onInit();

      expect($scope.$new).toHaveBeenCalled();
    });

    describe('watchers', () => {
      let scope;

      beforeEach(() => {
        scope = {};
        $ctrl._getOuterScope = () => ({
          $new: () => scope,
        });
        $ctrl.$onInit();
      });

      it('should watch for item', () => {
        $scope.$apply(() => {
          $ctrl.item = 'test';
        });

        expect(scope.item).toBe('test');
      });

      it('should watch for hippoTree', () => {
        spyOn($ctrl, '_render');

        $scope.$apply(() => {
          $ctrl.hippoTree = hippoTree;
        });

        expect(scope.hippoTree).toBe(hippoTree);
        expect($ctrl._render).toHaveBeenCalled();
      });

      it('should watch for uiTreeNode', () => {
        const toggle = () => {};
        $scope.$apply(() => {
          $ctrl.uiTreeNode = {
            scope: { toggle },
          };
        });

        expect(scope.toggle).toBe(toggle);
      });
    });
  });

  describe('_getOuterScope', () => {
    it('should return outer scope', () => {
      expect($ctrl._getOuterScope()).toBe(outerScope);
    });
  });

  describe('_render', () => {
    it('should not call rendering', () => {
      spyOn(hippoTree, 'renderTreeTemplate');

      $ctrl.scope = { };
      $ctrl._render();

      expect(hippoTree.renderTreeTemplate).not.toHaveBeenCalled();
    });

    it('should call rendering', () => {
      spyOn($element, 'replaceWith');

      $ctrl.scope = { hippoTree };
      $ctrl._render();

      expect($element.replaceWith).toHaveBeenCalledWith('dom');
    });
  });
});
