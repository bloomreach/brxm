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

describe('ViewportToggleCtrl', () => {
  let $componentController;
  let $translate;
  let ChannelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$translate_, _ChannelService_) => {
      $componentController = _$componentController_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
    });

    spyOn($translate, 'instant');
  });

  function createController(viewportMap) {
    spyOn(ChannelService, 'getChannel').and.returnValue({
      viewportMap,
    });

    return $componentController('viewportToggle', {
      ChannelService,
    });
  }

  describe('setViewports', () => {
    it('should set the viewport widths from the backend', () => {
      const $ctrl = createController({
        desktop: 1167,
        tablet: 678,
        phone: 256,
      });

      $ctrl.setViewports();

      expect($ctrl.viewports[0].id).toBe('ANY_DEVICE');
      expect($ctrl.viewports[0].width).toBe(0);
      expect($ctrl.viewports[1].id).toBe('DESKTOP');
      expect($ctrl.viewports[1].width).toBe(1167);
      expect($ctrl.viewports[2].id).toBe('TABLET');
      expect($ctrl.viewports[2].width).toBe(678);
      expect($ctrl.viewports[3].id).toBe('PHONE');
      expect($ctrl.viewports[3].width).toBe(256);
    });

    it('should use the default viewport width values when the backend does not return any', () => {
      const $ctrl = createController({});
      $ctrl.setViewports();

      expect($ctrl.viewports[0].id).toBe('ANY_DEVICE');
      expect($ctrl.viewports[0].width).toBe(0);
      expect($ctrl.viewports[1].id).toBe('DESKTOP');
      expect($ctrl.viewports[1].width).toBe(1280);
      expect($ctrl.viewports[2].id).toBe('TABLET');
      expect($ctrl.viewports[2].width).toBe(720);
      expect($ctrl.viewports[3].id).toBe('PHONE');
      expect($ctrl.viewports[3].width).toBe(320);
    });
  });

  describe('getDisplayName', () => {
    it('should return the display name', () => {
      const $ctrl = createController({});
      $ctrl.getDisplayName({
        id: 'TEST',
      });

      expect($translate.instant).toHaveBeenCalledWith('VIEWPORT_TEST');
    });
  });
});
