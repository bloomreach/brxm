/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
  let $rootScope;
  let MaskService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _MaskService_, $componentController) => {
      MaskService = _MaskService_;

      $rootScope = _$rootScope_;
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

  it('should forward clickHandler', () => {
    spyOn($rootScope, '$emit');

    $ctrl.onClick();

    expect($rootScope.$emit).toHaveBeenCalledWith('mask:click');
  });
});
