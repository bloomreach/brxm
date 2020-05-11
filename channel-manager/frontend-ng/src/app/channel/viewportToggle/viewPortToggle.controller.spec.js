/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
  let $ctrl;
  let $translate;
  let channel;
  let devices;
  let ngModel;
  let viewportMap;
  let ChannelService;
  let ViewportService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$translate_, _ChannelService_, _ViewportService_) => {
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      ViewportService = _ViewportService_;

      ngModel = jasmine.createSpyObj('NgModelCtrl', ['$setViewValue']);
      devices = [];
      viewportMap = {};
      channel = { defaultDevice: 'default', devices, viewportMap };
      spyOn(ChannelService, 'getChannel').and.returnValue(channel);
      spyOn(ViewportService, 'setWidth');
      spyOn($translate, 'instant').and.callFake(key => key);

      $ctrl = _$componentController_('viewportToggle', {}, { ngModel });
    });
  });

  describe('$onInit', () => {
    it('selects the "any_device" viewport and uses it to set the viewport', () => {
      $ctrl.$onInit();

      expect($ctrl.value).toBe('any_device');
      expect(ViewportService.setWidth).toHaveBeenCalledWith(0);
    });

    it("selects a channel's default device", () => {
      channel.defaultDevice = 'tablet';
      $ctrl.$onInit();

      expect($ctrl.value).toBe('tablet');
    });

    it('allows for a custom viewport', () => {
      devices.push('{ "id": "Custom-id", "icon": "custom-icon", "width": 320 }');
      $ctrl.$onInit();

      expect($ctrl.values).toHaveLength(5);
      expect($ctrl.values[4]).toEqual({
        id: 'custom-id',
        icon: 'custom-icon',
        label: 'Custom-id',
        width: 320,
      });
    });

    it('ensures a custom viewport has a default icon and lowercase id', () => {
      devices.push('{ "id": "CUSTOM-ID", "width": 320 }');
      $ctrl.$onInit();

      expect($ctrl.values[4].id).toEqual('custom-id');
      expect($ctrl.values[4].icon).not.toBeEmpty();
    });

    it('allows the default viewports to be customized', () => {
      devices.push('{ "id": "tablet", "width": 4000, "icon": "ipad" }');
      $ctrl.$onInit();

      expect($ctrl.values[2].id).toBe('tablet');
      expect($ctrl.values[2].width).toBe(4000);
      expect($ctrl.values[2].icon).toBe('ipad');
    });

    it('should not choke on improper configuration values for custom viewports', () => {
      devices.push(null);
      devices.push(undefined);
      devices.push('');
      devices.push(' ');
      devices.push('tablet:786px');
      devices.push('{ id: "id-with-missing-double-quotes" }');

      $ctrl.$onInit();

      expect($ctrl.values).toHaveLength(4);
    });

    it('should set the viewport widths from the backend', () => {
      viewportMap.desktop = 1167;
      viewportMap.tablet = 678;
      viewportMap.phone = 256;
      $ctrl.$onInit();

      expect($ctrl.values[0].id).toBe('any_device');
      expect($ctrl.values[0].width).toBe(0);
      expect($ctrl.values[1].id).toBe('desktop');
      expect($ctrl.values[1].width).toBe(1167);
      expect($ctrl.values[2].id).toBe('tablet');
      expect($ctrl.values[2].width).toBe(678);
      expect($ctrl.values[3].id).toBe('phone');
      expect($ctrl.values[3].width).toBe(256);
    });

    it('should use the default viewport width values when the backend does not return any', () => {
      $ctrl.$onInit();

      expect($ctrl.values[0].id).toBe('any_device');
      expect($ctrl.values[0].label).toBe('any_device');
      expect($ctrl.values[0].width).toBe(0);
      expect($ctrl.values[1].id).toBe('desktop');
      expect($ctrl.values[1].label).toBe('desktop');
      expect($ctrl.values[1].width).toBe(1280);
      expect($ctrl.values[2].id).toBe('tablet');
      expect($ctrl.values[2].label).toBe('tablet');
      expect($ctrl.values[2].width).toBe(720);
      expect($ctrl.values[3].id).toBe('phone');
      expect($ctrl.values[3].label).toBe('phone');
      expect($ctrl.values[3].width).toBe(320);
    });
  });

  describe('onChange', () => {
    beforeEach(() => {
      $ctrl.$onInit();
      $ctrl.value = 'tablet';
      $ctrl.onChange();
    });

    it('should update the model', () => {
      expect(ngModel.$setViewValue).toHaveBeenCalledWith('tablet');
    });

    it('should update the viewport width', () => {
      expect(ViewportService.setWidth).toHaveBeenCalledWith(720);
    });
  });
});
