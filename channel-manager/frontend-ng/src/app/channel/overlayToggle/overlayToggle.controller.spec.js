/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('overlayToggle component', () => {
  let $ctrl;
  let localStorageService;
  let ProjectService;

  const testStorageKey = 'channelManager.overlays.testToggle';
  const onStateCallback = jasmine.createSpy('stateChangeCallback');

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _localStorageService_,
      _ProjectService_,
    ) => {
      localStorageService = _localStorageService_;
      ProjectService = _ProjectService_;

      $ctrl = $componentController('overlayToggle', {}, {
        name: 'testToggle',
        state: false,
        defaultState: true,
        onStateChange: onStateCallback,
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
  });

  describe('$onChanges', () => {
    it('initiates overlay again when inputs change', () => {
      spyOn($ctrl, 'initiateOverlay');
      $ctrl.$onChanges();
      expect($ctrl.initiateOverlay).toHaveBeenCalled();
    });
  });

  describe('initOverlay', () => {
    it('loads persisted state when selected project is master', () => {
      spyOn($ctrl, 'loadPersistentState');

      $ctrl.initiateOverlay();

      expect($ctrl.disabled).toBe(false);
      expect($ctrl.loadPersistentState).toHaveBeenCalled();
    });

    it('loads persisted state when a branch is selected and editing is allowed', () => {
      spyOn(ProjectService, 'isBranch').and.returnValue(true);
      spyOn(ProjectService, 'isEditingAllowed').and.returnValue(true);
      spyOn($ctrl, 'loadPersistentState');

      $ctrl.initiateOverlay();

      expect($ctrl.disabled).toBe(false);
      expect($ctrl.loadPersistentState).toHaveBeenCalled();
    });

    it('disables buttons and overlay when a branch is selected and editing is not allowed', () => {
      spyOn(ProjectService, 'isBranch').and.returnValue(true);
      spyOn(ProjectService, 'isEditingAllowed').and.returnValue(false);
      spyOn($ctrl, 'loadPersistentState');

      $ctrl.initiateOverlay();

      expect($ctrl.disabled).toBe(true);
      expect($ctrl.state).toBe(false);
      expect($ctrl.loadPersistentState).not.toHaveBeenCalled();
      expect(onStateCallback).toHaveBeenCalledWith({ state: $ctrl.state });
    });
  });

  describe('setState', () => {
    it('sets the visibility state of an overlay and stores it for persistence', () => {
      $ctrl.$onInit();
      $ctrl.state = false;
      $ctrl.setState(true);
      expect($ctrl.state).toEqual(true);
      expect(localStorageService.set).toHaveBeenCalledWith(testStorageKey, true);
    });
  });

  describe('loadPersistentState', () => {
    it('sets the local state variable based on the state in local storage, if not null (not first visit)', () => {
      localStorageService.get.and.returnValue(true);
      $ctrl.loadPersistentState();
      expect($ctrl.state).toEqual(true);
    });

    it('uses default state value if local storage value is null (first visit)', () => {
      localStorageService.get.and.returnValue(null);
      $ctrl.loadPersistentState();
      expect($ctrl.state).toEqual(true);
    });
  });
});
