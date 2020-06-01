/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
  let $q;
  let $rootScope;
  let $scope;
  let $timeout;
  let PickerService;
  let ngModel;
  let config;

  const $element = angular.element('<div></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields');

    PickerService = jasmine.createSpyObj('PickerService', ['pickImage']);

    angular.mock.module(($provide) => {
      $provide.value('PickerService', PickerService);
    });

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $scope = _$rootScope_.$new();
      $timeout = _$timeout_;
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
    }, {
      ariaLabel: 'TestAriaLabel',
      config,
      hint: 'TestHint',
      index: 0,
      name: 'TestField',
      ngModel,
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
      expect($ctrl.imagePicked).toBeFalsy();
    });

    it('adds an asterisk to the hiddenLabel for required image links', () => {
      $ctrl.ngRequired = true;
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

  describe('onFocusFromParent', () => {
    let preventDefault;

    beforeEach(() => {
      preventDefault = jasmine.createSpy('preventDefault');
      spyOn($ctrl, 'onFocusFromParent').and.callThrough();
    });

    describe('when parent broadcasts event "primitive-field:focus"', () => {
      it('calls "onFocusFromParent" when index is 0', () => {
        $ctrl.index = 0;
        init();

        $rootScope.$broadcast('primitive-field:focus', { preventDefault });
        expect($ctrl.onFocusFromParent).toHaveBeenCalled();
      });

      it('does not call "onFocusFromParent" when index is not 0', () => {
        $ctrl.index = 1;
        init();

        $rootScope.$broadcast('primitive-field:focus');
        expect($ctrl.onFocusFromParent).not.toHaveBeenCalled();
      });
    });

    it('prevents default event behavior when receiving focus from parent', () => {
      $ctrl.onFocusFromParent({ preventDefault });
      expect(preventDefault).toHaveBeenCalled();
    });

    it('opens the imagePicker if model is empty', () => {
      spyOn($ctrl, 'openImagePicker');
      ngModel.$modelValue = '';
      init();

      $ctrl.onFocusFromParent({ preventDefault });

      expect($ctrl.openImagePicker).toHaveBeenCalled();
    });

    it('puts focus on the "select" button if model is not empty', () => {
      const selectBtn = jasmine.createSpyObj('selectBtn', ['focus']);
      spyOn($element, 'find').and.returnValues(selectBtn);
      init();

      $ctrl.onFocusFromParent({ preventDefault });

      expect(selectBtn.focus).toHaveBeenCalled();
    });
  });

  describe('focus handling', () => {
    it('sets focus on the "select" button if no image is yet selected', () => {
      const imgEl = [];
      const selectEl = jasmine.createSpyObj('selectElement', ['focus']);
      // find() is called twice, first to check if there are any images, then to find the select button
      spyOn($element, 'find').and.returnValues(imgEl, selectEl);

      $ctrl.setFocus();

      expect($element.find).toHaveBeenCalledWith('img');
      expect($element.find).toHaveBeenCalledWith('.hippo-imagelink-select');
      expect(selectEl.focus).toHaveBeenCalled();
    });

    it('sets focus on the "clear" button if an image has been selected', () => {
      const imgEl = ['img'];
      const clearEl = jasmine.createSpyObj('clearElement', ['focus']);
      // find() is called twice, first to check if there are any images, then to find the clear button
      spyOn($element, 'find').and.returnValues(imgEl, clearEl);

      $ctrl.setFocus();

      expect($element.find).toHaveBeenCalledWith('img');
      expect($element.find).toHaveBeenCalledWith('.hippo-imagelink-clear');
      expect(clearEl.focus).toHaveBeenCalled();
    });

    it('emits button focus event and set buttonHasFocus to true', () => {
      spyOn($element, 'triggerHandler');
      const event = {};
      $ctrl.onFocus(event);

      expect($ctrl.buttonHasFocus).toBe(true);
      expect($element.triggerHandler).toHaveBeenCalledWith(event);
    });

    it('emits button blur event and set buttonHasFocus to false after timeout', () => {
      spyOn($element, 'triggerHandler');
      $ctrl.buttonHasFocus = true;
      const event = {};
      $ctrl.onBlur(event);

      expect($ctrl.buttonHasFocus).toBe(true);
      expect($element.triggerHandler).toHaveBeenCalledWith(event);

      $timeout.flush();
      expect($ctrl.buttonHasFocus).toBe(false);
    });

    it('cancels the timeout if a focus event is fired right after the blur event', () => {
      spyOn($timeout, 'cancel').and.callThrough();
      $ctrl.buttonHasFocus = true;
      $ctrl.onBlur();
      $ctrl.onFocus();
      $timeout.flush();

      expect($timeout.cancel).toHaveBeenCalled();
      expect($ctrl.buttonHasFocus).toBe(true);
    });
  });

  describe('openImagePicker', () => {
    beforeEach(() => {
      init();
      spyOn($ctrl, '_focusClearButton');
      spyOn($ctrl, '_focusSelectButton');
    });

    it('picks an image', () => {
      PickerService.pickImage.and.returnValue($q.resolve());
      $ctrl.openImagePicker();
      expect(PickerService.pickImage).toHaveBeenCalledWith(
        'image-picker-config',
        { uuid: 'model-value' },
      );
    });

    it('stores the URL and the UUID of the picked image', (done) => {
      PickerService.pickImage.and.returnValue($q.resolve({
        url: 'new-url',
        uuid: 'new-uuid',
      }));
      $ctrl.openImagePicker().then(() => {
        expect($ctrl.imagePicked).toBe(true);
        // the image will be focused by the focus-if directive
        expect($ctrl._focusClearButton).not.toHaveBeenCalled();
        expect($ctrl.url).toEqual('new-url');
        expect(ngModel.$setViewValue).toHaveBeenCalledWith('new-uuid');
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the clear button if an image was previously picked', (done) => {
      $ctrl.imagePicked = true;

      PickerService.pickImage.and.returnValue($q.resolve({
        url: 'new-url',
        uuid: 'new-uuid',
      }));

      $ctrl.openImagePicker().then(() => {
        expect($ctrl._focusClearButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the clear button when the picker is cancelled and an image was previously picked', (done) => {
      spyOn($ctrl, '_hasImage').and.returnValue(true);
      PickerService.pickImage.and.returnValue($q.reject());

      $ctrl.openImagePicker().finally(() => {
        expect($ctrl._focusClearButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the select button when the picker is cancelled and no image has been picked yet', (done) => {
      PickerService.pickImage.and.returnValue($q.reject());

      $ctrl.openImagePicker().finally(() => {
        expect($ctrl._focusSelectButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('never opens more than one picker at a time', () => {
      const deferred = $q.defer();
      PickerService.pickImage.and.returnValue(deferred.promise);

      $ctrl.openImagePicker();
      $ctrl.openImagePicker();
      expect(PickerService.pickImage.calls.count()).toBe(1);

      deferred.resolve();
      $scope.$digest();

      $ctrl.openImagePicker();
      expect(PickerService.pickImage.calls.count()).toBe(2);
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
