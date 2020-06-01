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

describe('MaskService', () => {
  let $rootScope;
  let MaskService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _MaskService_) => {
      $rootScope = _$rootScope_;
      MaskService = _MaskService_;
    });
  });

  it('should initialize mask class', () => {
    expect(MaskService.defaultMaskClass).toBe('masked');
  });

  describe('toggling the mask', () => {
    it('should enable the mask', () => {
      MaskService.mask();
      $rootScope.$digest();

      expect(MaskService.isMasked).toBe(true);
      expect(MaskService.maskClass).toBe('masked');
    });

    it('should set the optional mask class if provided', () => {
      MaskService.mask('myMaskClass');
      $rootScope.$digest();

      expect(MaskService.maskClass).toBe('masked myMaskClass');
    });

    it('should disable the mask', () => {
      MaskService.unmask();
      $rootScope.$digest();

      expect(MaskService.isMasked).toBe(false);
    });
  });
});
