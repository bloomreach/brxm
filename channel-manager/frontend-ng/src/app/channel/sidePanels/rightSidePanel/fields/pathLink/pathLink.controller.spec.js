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
  let $q;
  let $scope;
  let $timeout;
  let PickerService;
  let config;
  let ngModel;

  const $element = angular.element('<div></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.fields');

    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);

    angular.mock.module(($provide) => {
      $provide.value('PickerService', PickerService);
    });

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $timeout = _$timeout_;
      $scope = _$rootScope_.$new();
    });

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setTouched',
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
    }, {
      config,
      displayName: 'TestDisplayName',
      name: 'TestField',
      ngModel,
    });
  });

  describe('$onInit', () => {
    it('initializes the component', () => {
      $ctrl.$onInit();
      $scope.$apply();

      expect($ctrl.config).toEqual(config);
      expect($ctrl.displayName).toEqual('TestDisplayName');
      expect($ctrl.ngModel.$modelValue).toEqual('model-value');
    });

    it('registers a listener for the edit-component:select-document event', () => {
      spyOn($scope, '$on');
      $ctrl.$onInit();
      expect($scope.$on).toHaveBeenCalledWith('edit-component:select-document', jasmine.any(Function));
    });
  });

  describe('edit-component:select-document listener', () => {
    let onSelectDocument;

    beforeEach(() => {
      spyOn($scope, '$on');
      spyOn($ctrl, 'openLinkPicker');

      $ctrl.$onInit();
      [, onSelectDocument] = $scope.$on.calls.mostRecent().args;
    });

    it('opens the link picker when the parameter name matches', () => {
      onSelectDocument('event', 'TestField');
      expect(ngModel.$setTouched).toHaveBeenCalled();
      expect($ctrl.openLinkPicker).toHaveBeenCalled();
    });

    it('does not open the link picker when the parameter name does not match', () => {
      onSelectDocument('event', 'AnotherField');
      expect($ctrl.openLinkPicker).not.toHaveBeenCalled();
    });
  });

  describe('openLinkPicker', () => {
    it('picks a path', () => {
      PickerService.pickPath.and.returnValue($q.resolve());

      $ctrl.openLinkPicker();

      expect(PickerService.pickPath).toHaveBeenCalledWith(config.linkpicker, ngModel.$modelValue);
    });

    it('updates the view when a path has been picked', (done) => {
      PickerService.pickPath.and.returnValue($q.resolve({ path: 'some/path', displayName: 'path pretty name' }));
      spyOn($ctrl, '_focusSelectButton');

      $ctrl.openLinkPicker().then(() => {
        expect(ngModel.$setViewValue).toHaveBeenCalledWith('some/path');
        expect($ctrl.displayName).toEqual('path pretty name');
        done();
      });
      $scope.$digest();
    });

    it('focuses the select button when picked a path has been canceled', (done) => {
      PickerService.pickPath.and.returnValue($q.reject());
      spyOn($ctrl, '_focusSelectButton');

      $ctrl.openLinkPicker().finally(() => {
        expect($ctrl._focusSelectButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });
  });

  describe('focus', () => {
    beforeEach(() => {
      $ctrl.onFocus = jasmine.createSpy();
    });

    it('should forward focus event', () => {
      $ctrl.focus('event');
      expect($ctrl.onFocus).toHaveBeenCalledWith('event');
    });

    it('should focus container', () => {
      $ctrl.mdInputContainer = { setFocused: jasmine.createSpy() };
      $ctrl.focus('event');
      expect($ctrl.mdInputContainer.setFocused).toHaveBeenCalledWith(true);
    });
  });

  describe('blur', () => {
    beforeEach(() => {
      $ctrl.onBlur = jasmine.createSpy();
    });

    it('should forward blur event', () => {
      $ctrl.blur('event');
      $timeout.flush();
      expect($ctrl.onBlur).toHaveBeenCalledWith('event');
    });

    it('should blur container', () => {
      $ctrl.mdInputContainer = { setFocused: jasmine.createSpy() };
      $ctrl.blur('event');
      expect($ctrl.mdInputContainer.setFocused).toHaveBeenCalledWith(false);
    });
  });
});
