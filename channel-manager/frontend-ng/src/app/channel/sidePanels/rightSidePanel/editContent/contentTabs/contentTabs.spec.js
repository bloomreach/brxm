/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

describe('ContentTabsCtrl', () => {
  let $ctrl;
  let $state;
  let $rootScope;

  const navItem = 'test';
  const mockState = `hippo-cm.channel.edit-page.${navItem}`;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($componentController, _$rootScope_, _$state_, $uiRouterGlobals, $transitions) => {
      $rootScope = _$rootScope_;
      $state = _$state_;

      $uiRouterGlobals.current = {
        name: mockState,
      };

      $ctrl = $componentController('contentTabs', {
        $uiRouterGlobals,
        $transitions,
      });
    });
  });

  describe('onInit', () => {
    it('should set the initial selected nav item', () => {
      $ctrl.$onInit();

      expect($ctrl.selectedNavItem).toEqual(navItem);
    });

    it('should select the content tab on transition to content state', () => {
      const newNavItem = 'content';
      $ctrl.$onInit();
      $state.go(`hippo-cm.channel.edit-page.${newNavItem}`, { documentId: 'test1' });
      $rootScope.$digest();
      expect($ctrl.selectedNavItem).toEqual(newNavItem);
    });

    it('should select the content tab on transition to content state', () => {
      const newNavItem = 'versions';
      $ctrl.$onInit();
      $state.go(`hippo-cm.channel.edit-page.${newNavItem}`, { documentId: 'test1' });
      $rootScope.$digest();
      expect($ctrl.selectedNavItem).toEqual(newNavItem);
    });
  });
});
