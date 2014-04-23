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

describe('Number', function () {
    'use strict';
    
    describe('toFixed', function () {
        it('should convert numbers correctly', function () {
            expect((0.00008).toFixed(3)).toBe('0.000');
            expect((0.9).toFixed(0)).toBe('1');
            expect((1.255).toFixed(2)).toBe('1.25');
            expect((1843654265.0774949).toFixed(5)).toBe('1843654265.07749');
            expect((1000000000000000128).toFixed(0)).toBe('1000000000000000128');
        });
    });

});
