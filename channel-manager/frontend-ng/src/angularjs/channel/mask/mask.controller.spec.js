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

describe('MaskController', () => {
  let MaskController;
  let MaskService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_MaskService_, $componentController) => {
      MaskService = _MaskService_;

      MaskController = $componentController('mask', { MaskService });
    });

    spyOn(MaskService, 'initialize').and.callFake(() => {
      MaskService.clickHandler = () => 'clicked';
    });

    MaskController.$onInit();
  });

  it('should initialize', () => {
    expect(MaskService.initialize).toHaveBeenCalled();
  });

  it('should get masked status', () => {
    MaskService.isMasked = true;

    expect(MaskController.isMasked()).toBe(true);
  });

  it('should get mask class', () => {
    MaskService.isMasked = false;

    expect(MaskController.getMaskClass()).toEqual('');

    MaskService.isMasked = true;
    MaskService.maskClass = 'masked';

    expect(MaskController.getMaskClass()).toEqual('masked');
  });

  it('should forward clickHandler', () => {
    spyOn(MaskService, 'clickHandler');

    MaskController.onClick();

    expect(MaskService.clickHandler).toHaveBeenCalled();
  });
});

