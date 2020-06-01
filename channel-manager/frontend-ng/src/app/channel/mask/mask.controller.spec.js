/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('$ctrl', () => {
  let $ctrl;
  let MaskService;
  let OverlayService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_MaskService_, $componentController, _OverlayService_) => {
      MaskService = _MaskService_;
      OverlayService = _OverlayService_;

      $ctrl = $componentController('mask', { MaskService });
    });
  });

  it('should get mask class', () => {
    MaskService.isMasked = false;

    expect($ctrl.getMaskClass()).toEqual('');

    MaskService.isMasked = true;
    MaskService.maskClass = 'masked';

    expect($ctrl.getMaskClass()).toEqual('masked');
  });

  it('should change toggle its own state', () => {
    OverlayService.toggleOverlayByComponent = true;
    $ctrl.state = true;
    $ctrl.onClick();

    expect($ctrl.state).toEqual(false);
  });

  it('should forward clickHandler', () => {
    spyOn(MaskService, 'clickHandler');

    $ctrl.onClick();

    expect(MaskService.clickHandler).toHaveBeenCalled();
  });
});
