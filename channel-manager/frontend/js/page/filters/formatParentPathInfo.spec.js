/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

describe('formatParentPathInfo test cases', function () {
  'use strict';

  var formatParentPathInfo;

  beforeEach(module('hippo.channel.page'));
  beforeEach(inject(function (_formatParentPathInfoFilter_) {
    formatParentPathInfo = _formatParentPathInfoFilter_;
  }));

  describe('formatParentPathInfo', function () {
    it('should add a slash when the input does not end with a slash', function () {
      expect(formatParentPathInfo('test')).toBe('test/');
    });
    it('should not add an additional slash when the input already ends with a slash', function () {
      expect(formatParentPathInfo('test')).toBe('test/');
    });
    it('null input must return an empty string', function () {
      expect(formatParentPathInfo(null)).toBe('');
    });
    it('empty string input must return an empty string', function () {
      expect(formatParentPathInfo('')).toBe('');
    });
  });

});