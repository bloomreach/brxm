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

describe('nodeLinkController', () => {
  let $componentController;
  let $ctrl;
  let $q;
  let $scope;
  let $timeout;
  let PickerService;
  let config;
  let ngModel;
  let onBlur;
  let onFocus;

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
      '$setViewValue',
      '$modelValue',
    ]);

    ngModel.$modelValue = 'model-value';
    config = {
      linkpicker: 'link-picker-config',
    };

    onBlur = jasmine.createSpy('onBlur');
    onFocus = jasmine.createSpy('onFocus');

    $ctrl = $componentController('nodeLink', {
      $scope,
      $element,
    }, {
      ariaLabel: 'TestAriaLabel',
      config,
      displayName: 'TestDisplayName',
      hint: 'TestHint',
      index: 0,
      name: 'TestField',
      ngModel,
      onBlur,
      onFocus,
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

    it('opens the linkPicker if model is empty', () => {
      spyOn($ctrl, 'openLinkPicker');
      ngModel.$modelValue = '';
      init();

      $ctrl.onFocusFromParent({ preventDefault });

      expect($ctrl.openLinkPicker).toHaveBeenCalled();
    });

    it('puts focus on the "clear" button if model is not empty', () => {
      const clearBtn = jasmine.createSpyObj('clearButton', ['focus']);
      spyOn($element, 'find').and.returnValues(clearBtn);
      init();

      $ctrl.onFocusFromParent({ preventDefault });

      expect(clearBtn.focus).toHaveBeenCalled();
    });

    it('emits focus event and set hasFocus to true', () => {
      const event = {};
      $ctrl.focus(event);

      expect($ctrl.hasFocus).toBe(true);
      expect(onFocus).toHaveBeenCalledWith(event);
    });

    it('emits blur event and set hasFocus to false after timeout', () => {
      $ctrl.hasFocus = true;
      const event = {};
      $ctrl.blur(event);

      expect($ctrl.hasFocus).toBe(true);

      $timeout.flush();
      expect(onBlur).toHaveBeenCalledWith(event);
      expect($ctrl.hasFocus).toBe(false);
    });

    it('cancels the timeout if a focus event is fired right after the blur event', () => {
      spyOn($timeout, 'cancel').and.callThrough();
      $ctrl.hasFocus = true;
      $ctrl.blur();
      $ctrl.focus();
      $timeout.flush();

      expect($timeout.cancel).toHaveBeenCalled();
      expect($ctrl.hasFocus).toBe(true);
    });
  });

  describe('openLinkPicker', () => {
    beforeEach(() => {
      init();
      spyOn($ctrl, '_focusSelectButton');
    });

    it('picks a link', (done) => {
      PickerService.pickLink.and.returnValue($q.resolve());
      $ctrl.openLinkPicker().then(() => {
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

      $ctrl.openLinkPicker().then(() => {
        expect($ctrl.linkPicked).toBe(true);
        expect($ctrl._focusSelectButton).not.toHaveBeenCalled();
        expect($ctrl.displayName).toEqual('new-display-name');
        expect(ngModel.$setViewValue).toHaveBeenCalledWith('new-uuid');
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the select-button if a link was previously picked', (done) => {
      $ctrl.linkPicked = true;

      PickerService.pickLink.and.returnValue($q.resolve({
        displayName: 'new-display-name',
        uuid: 'new-uuid',
      }));

      $ctrl.openLinkPicker().then(() => {
        expect($ctrl._focusSelectButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('sets focus on the select button when the picker is cancelled and a link was previously picked', (done) => {
      PickerService.pickLink.and.returnValue($q.reject());

      $ctrl.openLinkPicker().finally(() => {
        expect($ctrl._focusSelectButton).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });
  });

  describe('clear link', () => {
    it('resets the value of the displayName and ngModel.$viewValue', () => {
      init();
      $ctrl.clear();

      expect($ctrl.displayName).toEqual('');
      expect($ctrl.linkPicked).toBe(false);
      expect(ngModel.$setViewValue).toHaveBeenCalledWith('');
    });
  });
});
