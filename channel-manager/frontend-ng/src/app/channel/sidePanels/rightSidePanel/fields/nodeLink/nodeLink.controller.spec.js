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

describe('nodeLinkController', () => {
  let $componentController;
  let $ctrl;
  let $q;
  let $scope;
  let $timeout;
  let PickerService;
  let config;
  let mdInputContainer;
  let ngModel;

  const $element = angular.element('<div></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.fields');

    PickerService = jasmine.createSpyObj('PickerService', ['pickLink']);

    angular.mock.module(($provide) => {
      $provide.value('PickerService', PickerService);
    });

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $scope = _$rootScope_.$new();
      $timeout = _$timeout_;
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

    mdInputContainer = jasmine.createSpyObj('mdInputContainer', [
      'setFocused',
      'setInvalid',
    ]);

    $ctrl = $componentController('nodeLink', {
      $scope,
      $element,
    }, {
      ariaLabel: 'TestAriaLabel',
      config,
      displayName: 'TestDisplayName',
      hint: 'TestHint',
      index: 0,
      mdInputContainer,
      name: 'TestField',
      ngModel,
    });
  });

  function init() {
    $ctrl.$onInit();
    $scope.$apply();
  }

  describe('$onInit', () => {
    it('initializes the component', () => {
      init();

      expect($ctrl.ariaLabel).toEqual('TestAriaLabel');
      expect($ctrl.config).toEqual(config);
      expect($ctrl.displayName).toEqual('TestDisplayName');
      expect($ctrl.hint).toEqual('TestHint');
      expect($ctrl.index).toEqual(0);
      expect($ctrl.linkPicked).toBeFalsy();
      expect($ctrl.name).toEqual('TestField');
      expect($ctrl.ngModel.$modelValue).toEqual('model-value');
    });

    it('reacts on model validity changes', () => {
      init();

      ngModel.$invalid = true;
      ngModel.$touched = true;
      $scope.$digest();

      expect(mdInputContainer.setInvalid).toHaveBeenCalledWith(true);
    });
  });

  describe('focus handling', () => {
    let preventDefault;
    beforeEach(() => {
      preventDefault = jasmine.createSpy('preventDefault');
    });

    describe('when parent broadcasts event "primitive-field:focus"', () => {
      it('calls onFocusFromParent when index is zero', () => {
        spyOn($ctrl, 'onFocusFromParent');
        $ctrl.index = 0;
        init();

        $scope.$parent.$broadcast('primitive-field:focus');

        expect($ctrl.onFocusFromParent).toHaveBeenCalled();
      });

      it('does not call onFocusFromParent when index is not zero', () => {
        spyOn($ctrl, 'onFocusFromParent');
        $ctrl.index = 1;
        init();

        $scope.$parent.$broadcast('primitive-field:focus');

        expect($ctrl.onFocusFromParent).not.toHaveBeenCalled();
      });
    });


    it('prevents default event behavior when receiving focus from parent', () => {
      $ctrl.onFocusFromParent({ preventDefault });
      expect(preventDefault).toHaveBeenCalled();
    });

    it('opens the picker if model is empty', () => {
      spyOn($ctrl, 'open');
      ngModel.$modelValue = '';
      init();

      $ctrl.onFocusFromParent({ preventDefault });

      expect($ctrl.open).toHaveBeenCalled();
    });

    it('puts focus on the "clear" button if model is not empty', () => {
      const clearBtn = jasmine.createSpyObj('clearButton', ['focus']);
      spyOn($element, 'find').and.returnValues(clearBtn);
      init();

      $ctrl.onFocusFromParent({ preventDefault });

      expect(clearBtn.focus).toHaveBeenCalled();
    });

    it('sets focus on parent container', () => {
      $ctrl.onFocus();
      expect($ctrl.mdInputContainer.setFocused).toHaveBeenCalledWith(true);
    });

    it('emits focus event and set hasFocus to true', () => {
      spyOn($element, 'triggerHandler');
      const event = {};
      $ctrl.onFocus(event);

      expect($ctrl.hasFocus).toBe(true);
      expect($element.triggerHandler).toHaveBeenCalledWith(event);
    });

    it('blurs parent container', () => {
      $ctrl.onBlur();
      $timeout.flush();

      expect($ctrl.mdInputContainer.setFocused).toHaveBeenCalledWith(false);
    });

    it('emits blur event and set hasFocus to false after timeout', () => {
      spyOn($element, 'triggerHandler');
      $ctrl.hasFocus = true;
      const event = {};
      $ctrl.onBlur(event);

      expect($ctrl.hasFocus).toBe(true);

      $timeout.flush();
      expect($element.triggerHandler).toHaveBeenCalledWith(event);
      expect($ctrl.hasFocus).toBe(false);
    });

    it('prevents blur event if the picker is open', () => {
      PickerService.pickLink.and.returnValue($q.defer().promise);

      $ctrl.open();
      $ctrl.onBlur();
      $timeout.flush();

      expect($ctrl.hasFocus).toBe(true);
    });

    it('cancels the timeout if a focus event is fired right after the blur event', () => {
      spyOn($timeout, 'cancel').and.callThrough();
      $ctrl.hasFocus = true;
      $ctrl.onBlur();
      $ctrl.onFocus();
      $timeout.flush();

      expect($timeout.cancel).toHaveBeenCalled();
      expect($ctrl.hasFocus).toBe(true);
    });
  });

  describe('open', () => {
    beforeEach(() => {
      init();
      spyOn($ctrl, '_focusSelectButton');
    });

    it('picks a link', (done) => {
      PickerService.pickLink.and.returnValue($q.resolve());
      $ctrl.open().then(() => {
        expect(PickerService.pickLink).toHaveBeenCalledWith('link-picker-config', { uuid: 'model-value' });
        done();
      });
      $scope.$digest();
    });

    it('stores the UUID and displayName of the picked link', (done) => {
      PickerService.pickLink.and.returnValue($q.resolve({
        displayName: 'new-display-name',
        uuid: 'new-uuid',
      }));

      $ctrl.open().then(() => {
        expect($ctrl.isPicked).toBe(true);
        expect($ctrl._focusSelectButton).not.toHaveBeenCalled();
        expect($ctrl.displayName).toEqual('new-display-name');
        expect(ngModel.$setViewValue).toHaveBeenCalledWith('new-uuid');
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the select-button if a link was previously picked', (done) => {
      $ctrl.isPicked = true;

      PickerService.pickLink.and.returnValue($q.resolve({
        displayName: 'new-display-name',
        uuid: 'new-uuid',
      }));

      $ctrl.open().then(() => {
        expect($ctrl._focusSelectButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the select button when the picker is cancelled and a link was previously picked', (done) => {
      PickerService.pickLink.and.returnValue($q.reject());

      $ctrl.open().finally(() => {
        expect($ctrl._focusSelectButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('never opens more than one picker at a time', () => {
      const deferred = $q.defer();
      PickerService.pickLink.and.returnValue(deferred.promise);

      $ctrl.open();
      $ctrl.open();
      expect(PickerService.pickLink.calls.count()).toBe(1);

      deferred.resolve();
      $scope.$digest();

      $ctrl.open();
      expect(PickerService.pickLink.calls.count()).toBe(2);
    });
  });

  describe('clear link', () => {
    it('resets the value of the displayName and ngModel.$viewValue', () => {
      init();
      $ctrl.clear();

      expect($ctrl.displayName).toEqual('');
      expect($ctrl.isPicked).toBe(false);
      expect(ngModel.$setTouched).toHaveBeenCalled();
      expect(ngModel.$setViewValue).toHaveBeenCalledWith('');
    });
  });
});
