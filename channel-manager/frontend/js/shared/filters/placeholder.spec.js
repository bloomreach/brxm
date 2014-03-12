/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

describe('filter', function () {
    'use strict';

    var placeholderFilter;

    beforeEach(module('hippo.channel.menu'));
    beforeEach(inject(function (_placeholderFilter_) {
        placeholderFilter = _placeholderFilter_;
    }));

    describe('placeholder', function () {
        it('should not insert the placeholder when the input is not empty', function () {
            expect(placeholderFilter('test', 'test')).toBe('test');
        });

        it('should insert the placeholder when the input is empty', function () {
            expect(placeholderFilter('', 'test')).toBe('test');
        });

        it('should insert the placeholder when the input is null', function () {
            expect(placeholderFilter(null, 'abc')).toBe('abc');
        });
    });

});