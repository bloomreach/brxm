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

describe('imageLinkController', () => {
  let $componentController;
  let $ctrl;
  let $scope;
  let CmsService;
  let ngModel;
  let config;

  const $element = angular.element('<div></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.fieldsModule');

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
      imagepicker: 'image-picker-config',
    };

    $ctrl = $componentController('imageLink', {
      $scope,
      $element,
      CmsService,
    }, {
      ngModel,
      name: 'TestField',
      ariaLabel: 'TestAriaLabel',
      config,
      hint: 'TestHint',
      index: 0,
      url: 'TestUrl',
    });
  });

  function init() {
    $ctrl.$onInit();
    $scope.$apply();
  }

  describe('$onInit', () => {
    it('initializes the component', () => {
      init();

      expect($ctrl.ngModel.$modelValue).toEqual('model-value');
      expect($ctrl.name).toEqual('TestField');
      expect($ctrl.ariaLabel).toEqual('TestAriaLabel');
      expect($ctrl.hiddenLabel).toEqual('TestAriaLabel');
      expect($ctrl.config).toEqual(config);
      expect($ctrl.hint).toEqual('TestHint');
      expect($ctrl.url).toEqual('TestUrl');
      expect($ctrl.selectElement).toBeDefined();
      expect($ctrl.imagePicked).toBeFalsy();
    });

    it('adds an asterisk to the hiddenLabel for required image links', () => {
      $ctrl.isRequired = true;
      init();

      expect($ctrl.hiddenLabel).toEqual('TestAriaLabel *');
    });

    it('only renders a hidden label for the first image', () => {
      $ctrl.index = 0;
      init();

      expect($ctrl.hiddenLabel).not.toEqual('');

      $ctrl.index = 1;
      init();

      expect($ctrl.hiddenLabel).toEqual('');
    });
  });

  describe('openImagePicker', () => {
    beforeEach(() => {
      init();
      spyOn(CmsService, 'publish');
      spyOn($ctrl.selectElement, 'focus');
      $ctrl.openImagePicker();
    });

    it('opens the picker by publishing the "show-image-picker" event', () => {
      expect(CmsService.publish).toHaveBeenCalledWith('show-image-picker', 'image-picker-config', { uuid: 'model-value' },
        jasmine.any(Function), jasmine.any(Function));
    });

    it('stores the URL and the UUID of the picked image', () => {
      const okCallback = CmsService.publish.calls.mostRecent().args[3];
      okCallback({
        url: 'new-url',
        uuid: 'new-uuid',
      });
      $scope.$apply();

      expect($ctrl.imagePicked).toBe(true);
      expect($ctrl.url).toEqual('new-url');
      expect(ngModel.$setViewValue).toHaveBeenCalledWith('new-uuid');
      expect($ctrl.selectElement.focus).toHaveBeenCalled();
    });

    it('sets focus on the selectElement when the picker is cancelled', () => {
      const cancelCallback = CmsService.publish.calls.mostRecent().args[4];
      cancelCallback();

      expect($ctrl.selectElement.focus).toHaveBeenCalled();
    });
  });

  describe('clearPickedImage', () => {
    it('resets the value of the url and ngModel.$viewValue', () => {
      init();
      $ctrl.clearPickedImage();

      expect($ctrl.url).toEqual('');
      expect($ctrl.imagePicked).toBe(false);
      expect(ngModel.$setViewValue).toHaveBeenCalledWith('');
    });
  });
});
