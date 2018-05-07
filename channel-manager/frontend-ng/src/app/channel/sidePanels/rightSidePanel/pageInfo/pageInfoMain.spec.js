/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('pageInfoMainCtrl', () => {
  let ExtensionService;
  let PageInfoService;
  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.pageInfo');

    inject(($controller, $rootScope) => {
      ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtensions']);
      PageInfoService = jasmine.createSpyObj('PageInfoService', ['closePageInfo']);

      const $scope = $rootScope.$new();
      $ctrl = $controller('pageInfoMainCtrl', {
        $scope,
        ExtensionService,
        PageInfoService,
      });
    });
  });

  it('initializes the page extensions', () => {
    const extensions = [{ id: 'a' }, { id: 'b' }];
    ExtensionService.getExtensions.and.returnValue(extensions);

    $ctrl.$onInit();

    expect(ExtensionService.getExtensions).toHaveBeenCalledWith('page');
    expect($ctrl.extensions).toEqual(extensions);
  });

  it('gets the selected extension ID from the PageInfoService', () => {
    PageInfoService.selectedExtensionId = 'a';
    expect($ctrl.selectedExtensionId).toEqual('a');
  });

  it('sets the selected extension ID in the PageInfoService', () => {
    $ctrl.selectedExtensionId = 'a';
    expect(PageInfoService.selectedExtensionId).toEqual('a');
  });

  it('closes page info', () => {
    $ctrl.close();
    expect(PageInfoService.closePageInfo).toHaveBeenCalled();
  });
});
