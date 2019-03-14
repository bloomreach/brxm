/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

describe('OpenuiStringField', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $log;
  let $q;
  let $rootScope;
  let mdInputContainer;
  let ExtensionService;
  let OpenUIService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$log_, _$q_, _$rootScope_, _ExtensionService_, _OpenUIService_) => {
      $componentController = _$componentController_;
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ExtensionService = _ExtensionService_;
      OpenUIService = _OpenUIService_;
    });

    mdInputContainer = jasmine.createSpyObj('mdInputContainer', ['setHasValue']);
    $element = angular.element('<div/>');
    $ctrl = $componentController('openuiStringField', { $element }, { mdInputContainer });
  });

  it('initializes the component', () => {
    $ctrl.$onInit();

    expect(mdInputContainer.setHasValue).toHaveBeenCalledWith(true);
  });

  describe('$onChanges', () => {
    let extension;
    beforeEach(() => {
      extension = { displayName: 'test-extension' };
      spyOn(ExtensionService, 'getExtension').and.returnValue(extension);
      spyOn(ExtensionService, 'getExtensionUrl').and.returnValue('test-url');
    });

    it('connects to the child', () => {
      spyOn(OpenUIService, 'connect').and.returnValue($q.resolve());
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id' } });

      expect(ExtensionService.getExtension).toHaveBeenCalledWith('test-id');
      expect(ExtensionService.getExtensionUrl).toHaveBeenCalledWith(extension);
      expect(OpenUIService.connect).toHaveBeenCalledWith(jasmine.objectContaining({
        url: 'test-url',
        appendTo: $element[0],
      }));
    });

    it('logs error message', () => {
      const error = new Error('something went wrong');
      spyOn(OpenUIService, 'connect').and.returnValue($q.reject(error));
      spyOn($log, 'warn');
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id' } });

      $rootScope.$digest();

      expect($log.warn).toHaveBeenCalledWith(
        "Extension 'test-extension' failed to connect with the client library.",
        error,
      );
    });
  });
});
