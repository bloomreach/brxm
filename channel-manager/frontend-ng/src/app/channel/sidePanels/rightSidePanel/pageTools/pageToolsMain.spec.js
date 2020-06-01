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

describe('pageToolsMainCtrl', () => {
  let PageToolsService;
  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.pageTools');

    inject(($controller, $rootScope) => {
      PageToolsService = jasmine.createSpyObj('PageToolsService', ['getExtensions']);

      const $scope = $rootScope.$new();
      $ctrl = $controller('pageToolsMainCtrl', {
        $scope,
        PageToolsService,
      });
    });
  });

  it('initializes the page extensions', () => {
    const extensions = [{ id: 'a' }, { id: 'b' }];
    PageToolsService.getExtensions.and.returnValue(extensions);

    $ctrl.$onInit();

    expect(PageToolsService.getExtensions).toHaveBeenCalled();
    expect($ctrl.extensions).toEqual(extensions);
  });

  it('changes the app state when a tab is selected', () => {
    const extensions = [{ id: 'a' }, { id: 'b' }];
    PageToolsService.getExtensions.and.returnValue(extensions);

    $ctrl.$onInit();

    $ctrl.selectedTab = 0;
    expect(PageToolsService.selectedExtensionId).toEqual('a');

    $ctrl.selectedTab = 1;
    expect(PageToolsService.selectedExtensionId).toEqual('b');
  });

  it('sets the selected tab to the selected extension', () => {
    const extensions = [{ id: 'a' }, { id: 'b' }];
    PageToolsService.getExtensions.and.returnValue(extensions);

    $ctrl.$onInit();

    PageToolsService.selectedExtensionId = 'a';
    expect($ctrl.selectedTab).toBe(0);

    PageToolsService.selectedExtensionId = 'b';
    expect($ctrl.selectedTab).toBe(1);
  });
});
