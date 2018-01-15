/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

fdescribe('overlayToggle component', () => {
  let $ctrl;
  let localStorageService;
  const testStorageKey = 'channelManager.overlays.testToggle'

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _localStorageService_,
    ) => {
      localStorageService = _localStorageService_;

      $ctrl = $componentController('overlayToggle', {}, {
        name: 'testToggle',
        state: false,
        iconName: 'md-icon-name',
        tooltip: 'Test tooltip',
      });

      spyOn(localStorageService, 'get');
      spyOn(localStorageService, 'set');
    });
  });

  describe('$onInit', () => {
    it('initializes component controller', () => {
      expect($ctrl.state).toEqual(false);
      expect($ctrl.iconName).toEqual('md-icon-name');
      expect($ctrl.tooltip).toEqual('Test tooltip');
    });

    it('sets storage key based on the toggle name and loads persistent toggle state', () => {
      $ctrl.$onInit();
      expect($ctrl.storageKey).toEqual(testStorageKey);
      expect(localStorageService.get).toHaveBeenCalledWith(testStorageKey);
    });
  });

  describe('toggleState', () => {
    beforeEach(() => {
      $ctrl.$onInit();
    });

    it('toggles overlay state and sets it to local storage', () => {
      $ctrl.state = false;

      $ctrl.toggleState();
      expect($ctrl.state).toEqual(true);
      expect(localStorageService.set).toHaveBeenCalledWith(testStorageKey, true);

      $ctrl.toggleState();
      expect($ctrl.state).toEqual(false);
      expect(localStorageService.set).toHaveBeenCalledWith(testStorageKey, false);
    });
  });

  describe('loadPersistentState', () => {
    it('sets the local state variable based on the state in local storage', () => {
      localStorageService.get.and.returnValue(true);
      $ctrl.loadPersistentState();
      expect($ctrl.state).toEqual(true);
    });
  });
});
