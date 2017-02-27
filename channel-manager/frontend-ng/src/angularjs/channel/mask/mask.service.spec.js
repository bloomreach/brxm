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

describe('MaskService', () => {
  let MaskService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_MaskService_) => {
      MaskService = _MaskService_;
    });
  });

  it('should initialize mask class', () => {
    expect(MaskService.defaultMaskClass).toBe('masked');
  });

  it('should set default click handler', () => {
    expect(MaskService.clickHandler).toBeDefined();
  });

  describe('setting and resetting the click handler', () => {
    beforeEach(() => {
      function test() {
        return 'clickHandler';
      }

      MaskService.onClick(test);
    });

    it('should be able to change the click handler', () => {
      expect(MaskService.clickHandler()).toEqual('clickHandler');
    });

    it('should be able to reset the click handler', () => {
      MaskService.removeClickHandler();
      expect(MaskService.clickHandler).toBeDefined();
      expect(MaskService.clickHandler()).toEqual(undefined);
    });
  });

  describe('toggling the mask', () => {
    it('should enable the mask', () => {
      MaskService.mask();
      expect(MaskService.isMasked).toBe(true);
      expect(MaskService.maskClass).toBe('masked');
    });

    it('should set the optional mask class if provided', () => {
      MaskService.mask('myMaskClass');
      expect(MaskService.maskClass).toBe('masked myMaskClass');
    });

    it('should disable the mask', () => {
      MaskService.unmask();
      expect(MaskService.isMasked).toBe(false);
    });
  });
});
