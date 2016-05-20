/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

/* eslint-disable prefer-const */

describe('ToolbarCtrl', () => {
  'use strict';

  let $rootScope;
  let $controller;
  let ToolbarCtrl;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$controller_) => {
      $rootScope = _$rootScope_;
      $controller = _$controller_;
    });

    ToolbarCtrl = $controller('ToolbarCtrl', {
      $scope: $rootScope.$new(),
    });
  });

  describe('processMode', () => {
    it('should return an X if the mode is set to cancel', () => {
      spyOn(ToolbarCtrl, 'icon');

      ToolbarCtrl.processMode('cancel');
      $rootScope.$apply();

      expect(ToolbarCtrl.icon).toBe('clear');
    });
    it('should return a back arrow if the mode is not set', () => {
      spyOn(ToolbarCtrl, 'icon');

      ToolbarCtrl.processMode();
      $rootScope.$apply();

      expect(ToolbarCtrl.icon).toBe('keyboard_backspace');
    });
  });
});
