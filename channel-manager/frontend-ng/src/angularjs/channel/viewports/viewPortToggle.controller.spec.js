/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

describe('ViewportToggleCtrl', () => {
  let $rootScope;
  let $controller;
  let $translate;
  let ChannelService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$controller_, _$translate_, _ChannelService_) => {
      $rootScope = _$rootScope_;
      $controller = _$controller_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
    });
    spyOn($translate, 'instant');
  });

  describe('setViewPorts', () => {
    it('should set the viewport widths from the backend if it exists or default to default values', () => {
      spyOn(ChannelService, 'getChannel').and.returnValue({
        viewportMap: {
          desktop: 1167,
          tablet: 678,
          phone: 256,
        },
      });
      $ctrl = $controller('ViewportToggleCtrl', {
        $scope: $rootScope.$new(),
        ChannelService,
      });

      $ctrl.setViewPorts();
      $rootScope.$apply();

      expect($ctrl.viewPorts[1].id).toBe('DESKTOP');
      expect($ctrl.viewPorts[1].maxWidth).toBe(1167);
      expect($ctrl.viewPorts[2].id).toBe('TABLET');
      expect($ctrl.viewPorts[2].maxWidth).toBe(678);
      expect($ctrl.viewPorts[3].id).toBe('PHONE');
      expect($ctrl.viewPorts[3].maxWidth).toBe(256);
    });
    it('should set the viewport widths from the backend if it exists or default to default values', () => {
      spyOn(ChannelService, 'getChannel').and.returnValue({
        viewportMap: {},
      });
      $ctrl = $controller('ViewportToggleCtrl', {
        $scope: $rootScope.$new(),
        ChannelService,
      });

      $ctrl.setViewPorts();
      $rootScope.$apply();

      expect($ctrl.viewPorts[1].id).toBe('DESKTOP');
      expect($ctrl.viewPorts[1].maxWidth).toBe(1280);
      expect($ctrl.viewPorts[2].id).toBe('TABLET');
      expect($ctrl.viewPorts[2].maxWidth).toBe(720);
      expect($ctrl.viewPorts[3].id).toBe('PHONE');
      expect($ctrl.viewPorts[3].maxWidth).toBe(320);
    });
  });

  describe('getDisplayName', () => {
    beforeEach(() => {
      spyOn(ChannelService, 'getChannel').and.returnValue({
        viewportMap: {},
      });
      $ctrl = $controller('ViewportToggleCtrl', {
        $scope: $rootScope.$new(),
        ChannelService,
      });
    });
    it('should return the display name', () => {
      $ctrl.getDisplayName({
        id: 'TEST',
      });
      $rootScope.$apply();

      expect($translate.instant).toHaveBeenCalledWith('VIEWPORT_TEST');
    });
  });
});
