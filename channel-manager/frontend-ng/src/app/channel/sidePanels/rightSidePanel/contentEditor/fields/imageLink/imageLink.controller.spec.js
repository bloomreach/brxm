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
  let $scope;
  let PickerService;
  let config;
  let ngModel;

  const $element = angular.element('<div></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.fields');

    PickerService = jasmine.createSpyObj('PickerService', ['pickImage']);

    angular.mock.module(($provide) => {
      $provide.value('PickerService', PickerService);
    });

    inject((_$componentController_, _$q_, _$rootScope_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $scope = _$rootScope_.$new();
    });

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setTouched',
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
      config,
      displayName: 'TestDisplayName',
      name: 'TestField',
      ngModel,
    });
  });

  describe('open', () => {
    it('picks an image', () => {
      PickerService.pickImage.and.returnValue($q.resolve());

      $ctrl.open();

      expect(PickerService.pickImage).toHaveBeenCalledWith(config.imagepicker, { uuid: ngModel.$modelValue });
    });

    it('updates the preview when an image has been picked', (done) => {
      PickerService.pickImage.and.returnValue($q.resolve({ uuid: 'something', url: 'image-url' }));

      $ctrl.open().then(() => {
        expect(ngModel.$setViewValue).toHaveBeenCalledWith('something');
        expect($ctrl.displayName).toEqual('image-url');
        done();
      });
      $scope.$digest();
    });
  });
});
