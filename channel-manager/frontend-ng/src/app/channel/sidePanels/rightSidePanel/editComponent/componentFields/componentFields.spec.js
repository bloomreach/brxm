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

describe('ComponentFields', () => {
  let $componentController;
  let $q;
  let $scope;
  let ComponentEditor;
  let EditComponentService;

  let component;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((_$componentController_, _$q_, $rootScope, _ComponentEditor_, _EditComponentService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $scope = $rootScope.$new();
      ComponentEditor = _ComponentEditor_;
      EditComponentService = _EditComponentService_;
    });

    form = {};

    component = $componentController('componentFields',
      {},
      { form },
    );
  });

  describe('valueChanged', () => {
    it('triggers valueChanged() on the ComponentEditor when a value is changed and valid', () => {
      spyOn(ComponentEditor, 'valueChanged');
      form.$valid = true;
      ComponentEditor.valueChanged.and.returnValue($q.resolve());

      component.valueChanged();

      expect(ComponentEditor.valueChanged).toHaveBeenCalled();
    });

    it('does not trigger valueChanged() on the ComponentEditor when a value is changed to something invalid', () => {
      spyOn(ComponentEditor, 'valueChanged');
      form.$valid = false;

      component.valueChanged();

      expect(ComponentEditor.valueChanged).not.toHaveBeenCalled();
    });

    it('closes the editor when result of a value change cannot be processed', () => {
      spyOn(ComponentEditor, 'valueChanged').and.returnValue($q.reject());
      spyOn(ComponentEditor, 'killEditor');
      spyOn(EditComponentService, 'stopEditing');
      form.$valid = true;

      component.valueChanged();
      $scope.$digest();

      expect(ComponentEditor.killEditor).toHaveBeenCalled();
      expect(EditComponentService.stopEditing).toHaveBeenCalled();
    });
  });
});
