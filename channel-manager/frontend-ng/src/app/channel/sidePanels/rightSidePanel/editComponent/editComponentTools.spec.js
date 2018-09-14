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

describe('EditComponentToolsCtrl', () => {
  let $rootScope;
  let EditComponentService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject(($controller, _$q_, _$rootScope_) => {
      $rootScope = _$rootScope_;

      EditComponentService = jasmine.createSpyObj('EditComponentService', ['stopEditing']);

      const $scope = $rootScope.$new();
      $ctrl = $controller('editComponentToolsCtrl', {
        $scope,
        EditComponentService,
      });
    });
  });

  it('stops editing when closed', () => {
    $ctrl.close();

    expect(EditComponentService.stopEditing).toHaveBeenCalled();
  });
});
