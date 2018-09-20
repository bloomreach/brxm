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

describe('ComponentEditorCtrl', () => {
  let $componentController;
  let $rootScope;
  let CmsService;
  let ComponentEditor;

  let $ctrl;
  let $scope;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$rootScope_, _CmsService_) => {
      $componentController = _$componentController_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
    });

    spyOn(CmsService, 'reportUsageStatistic');

    ComponentEditor = jasmine.createSpyObj('ComponentEditor', [
      'getPropertyGroups',
    ]);

    $scope = $rootScope.$new();

    form = jasmine.createSpyObj('form', ['$setPristine']);

    $ctrl = $componentController('componentEditor',
      { $scope, ComponentEditor },
      { form },
    );
    $scope.$digest();
  });

  describe('dirty state sync', () => {
    beforeEach(() => {
      $ctrl.$onInit();
    });

    it('marks the component editor dirty when the form becomes dirty', () => {
      $ctrl.$onInit();

      form.$dirty = false;
      ComponentEditor.dirty = false;

      $scope.$digest();
      expect(ComponentEditor.dirty).toBe(false);

      form.$dirty = true;
      $scope.$digest();
      expect(ComponentEditor.dirty).toBe(true);
    });

    it('marks the form pristine when the component editor becomes pristine', () => {
      $ctrl.$onInit();

      form.$dirty = true;
      ComponentEditor.dirty = true;

      $scope.$digest();
      expect(ComponentEditor.dirty).toBe(true);

      ComponentEditor.dirty = false;
      $scope.$digest();
      expect(form.$setPristine).toHaveBeenCalled();
    });
  });

  it('gets the property groups', () => {
    const propertyGroups = [];
    ComponentEditor.getPropertyGroups.and.returnValue(propertyGroups);
    expect($ctrl.getPropertyGroups()).toBe(propertyGroups);
  });
});
