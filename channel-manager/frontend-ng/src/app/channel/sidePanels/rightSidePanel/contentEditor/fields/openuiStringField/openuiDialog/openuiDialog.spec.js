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

describe('OpenuiDialogCtrl', () => {
  let $ctrl;
  let $element;
  let $log;
  let $rootScope;
  let $scope;
  let ExtensionService;
  let OpenUiService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$log_, _$rootScope_) => {
      OpenUiService = jasmine.createSpyObj('OpenUiService', ['connect', 'initialize']);
      ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtension', 'getExtensionRelativeUrl']);
      $element = angular.element('<md-dialog><md-dialog-content/></md-dialog>');
      $log = _$log_;
      $rootScope = _$rootScope_;
      $scope = $rootScope.$new();

      const locals = {
        extensionId: 'test-id',
        dialogOptions: { title: 'Test title', url: './dialog' },
      };

      $ctrl = $controller('OpenuiDialogCtrl', {
        $element,
        $log,
        $scope,
        ExtensionService,
        locals,
        OpenUiService,
      });
    });
  });

  describe('$onInit', () => {
    it('connects to the child', () => {
      ExtensionService.getExtension.and.returnValue({});
      ExtensionService.getExtensionRelativeUrl.and.returnValue('http://test-url/dialog?a=b');
      spyOn($scope, '$on');

      $ctrl.$onInit();

      expect(OpenUiService.initialize).toHaveBeenCalledWith('test-id', jasmine.objectContaining({
        url: 'http://test-url/dialog?a=b',
        appendTo: $element.find('md-dialog-content')[0],
      }));
      expect($scope.$on.calls.mostRecent().args[0]).toBe('$destroy');
    });

    it('logs a warning when the connection fails', () => {
      const error = new Error('Connection destroyed');
      spyOn($log, 'warn');

      OpenUiService.initialize.and.throwError(error);
      ExtensionService.getExtension.and.returnValue({});
      ExtensionService.getExtensionRelativeUrl.and.returnValue('http://test-url?a=b');

      expect(() => $ctrl.$onInit()).toThrow(error);

      expect($log.warn).toHaveBeenCalledWith('Dialog \'Test title\' failed to connect with the client library.', error);
    });
  });
});
