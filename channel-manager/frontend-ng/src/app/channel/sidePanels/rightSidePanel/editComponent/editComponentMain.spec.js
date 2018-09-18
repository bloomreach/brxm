/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('EditComponentMainCtrl', () => {
  let $q;
  let $rootScope;
  let $scope;
  let ComponentEditor;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject(($controller, _$q_, _$rootScope_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;

      ComponentEditor = jasmine.createSpyObj('ComponentEditor', [
        'confirmDeleteComponent',
        'deleteComponent',
      ]);

      $scope = $rootScope.$new();

      $ctrl = $controller('editComponentMainCtrl', {
        $scope,
        ComponentEditor,
      });
    });
  });

  describe('delete component', () => {
    it('shows a confirm delete dialog', () => {
      ComponentEditor.confirmDeleteComponent.and.returnValue($q.resolve());

      $ctrl.deleteComponent();
      $scope.$digest();

      expect(ComponentEditor.confirmDeleteComponent).toHaveBeenCalled();
    });

    it('does not delete the component if the action is cancelled', () => {
      ComponentEditor.confirmDeleteComponent.and.returnValue($q.reject());

      $ctrl.deleteComponent();
      $scope.$digest();

      expect(ComponentEditor.deleteComponent).not.toHaveBeenCalled();
    });

    it('deletes the component if the action is confirmed', () => {
      ComponentEditor.confirmDeleteComponent.and.returnValue($q.resolve());

      $ctrl.deleteComponent();
      $scope.$digest();

      expect(ComponentEditor.deleteComponent).toHaveBeenCalled();
    });
  });
});
