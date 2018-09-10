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

describe('pathLinkController', () => {
  let $componentController;
  let $ctrl;
  let $scope;
  let CmsService;
  let config;
  let ngModel;

  const $element = angular.element('<div></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    inject((_$componentController_, _$rootScope_, _CmsService_) => {
      $componentController = _$componentController_;
      CmsService = _CmsService_;
      $scope = _$rootScope_.$new();
    });

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setViewValue',
      '$modelValue',
    ]);

    ngModel.$modelValue = 'model-value';
    config = {
      linkpicker: 'link-picker-config',
    };

    $ctrl = $componentController('pathLink', {
      $scope,
      $element,
      CmsService,
    }, {
      config,
      displayName: 'TestDisplayName',
      name: 'TestField',
      ngModel,
    });
  });

  describe('$onInit', () => {
    it('initializes the component', () => {
      spyOn(CmsService, 'subscribe');
      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.config).toEqual(config);
      expect($ctrl.displayName).toEqual('TestDisplayName');
      expect($ctrl.ngModel.$modelValue).toEqual('model-value');
      expect(CmsService.subscribe).toHaveBeenCalledWith('path-picked', jasmine.any(Function), jasmine.any(Object));
      expect(CmsService.subscribe).toHaveBeenCalledWith('path-canceled', jasmine.any(Function), jasmine.any(Object));
    });
  });

  describe('openLinkPicker', () => {
    it('opens the picker by publishing the "show-path-picker" event', () => {
      spyOn(CmsService, 'publish');
      $ctrl.openLinkPicker();

      expect(CmsService.publish).toHaveBeenCalledWith('show-path-picker', $ctrl.name, ngModel.$modelValue, config.linkpicker);
    });
  });

  describe('_onPathPicked', () => {
    beforeEach(() => {
      spyOn($ctrl, '_focusSelectButton');
    });

    it('does not handle "path-picked" event', () => {
      $ctrl._onPathPicked('SomeField');
      expect($ctrl._focusSelectButton).not.toHaveBeenCalled();
    });

    it('handles "path-picked" event', () => {
      $ctrl._onPathPicked($ctrl.name, 'some/path', 'path pretty name');

      expect(ngModel.$setViewValue).toHaveBeenCalledWith('some/path');
      expect($ctrl.displayName).toEqual('path pretty name');
    });
  });

  describe('_onPathCanceled', () => {
    beforeEach(() => {
      spyOn($ctrl, '_focusSelectButton');
    });

    it('does not handle "path-canceled" event', () => {
      $ctrl._onPathCanceled('SomeField');
      expect($ctrl._focusSelectButton).not.toHaveBeenCalled();
    });

    it('handles "path-canceled" event', () => {
      $ctrl._onPathCanceled($ctrl.name);
      expect($ctrl._focusSelectButton).toHaveBeenCalled();
    });
  });
});
