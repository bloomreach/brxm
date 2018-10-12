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

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((_$componentController_, _$q_, $rootScope, _ComponentEditor_, _EditComponentService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $scope = $rootScope.$new();
      ComponentEditor = _ComponentEditor_;
      EditComponentService = _EditComponentService_;
    });

    component = $componentController('componentFields');
  });

  describe('valueChanged', () => {
    beforeEach(() => {
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('updates the preview when a value is changed', () => {
      spyOn(ComponentEditor, 'updatePreview').and.returnValue($q.resolve());

      component.valueChanged();
      component.valueChanged();
      expect(ComponentEditor.updatePreview.calls.count()).toBe(1);

      jasmine.clock().tick(500);
      expect(ComponentEditor.updatePreview.calls.count()).toBe(2);
    });

    it('closes the editor when the result of a value change cannot be processed', () => {
      spyOn(ComponentEditor, 'updatePreview').and.returnValue($q.reject());
      spyOn(EditComponentService, 'killEditor');

      component.valueChanged();
      $scope.$digest();

      expect(EditComponentService.killEditor).toHaveBeenCalled();
    });
  });

  describe('blur', () => {
    it('sets the default value of an empty property when the field blurs', () => {
      spyOn(ComponentEditor, 'setDefaultIfValueIsEmpty');

      const property = {};
      component.blur(property);

      expect(ComponentEditor.setDefaultIfValueIsEmpty).toHaveBeenCalledWith(property);
    });
  });
});
