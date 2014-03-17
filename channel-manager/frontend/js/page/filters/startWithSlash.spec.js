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

    var startWithSlash;

    beforeEach(module('hippo.channel.page'));
    beforeEach(inject(function (_startWithSlashFilter_) {
        startWithSlash = _startWithSlashFilter_;
    }));

    describe('startWithSlash', function () {
        it('should add an initial slash when the input does not start with a slash', function () {
            expect(startWithSlash('test')).toBe('/test');
        });
        it('should not add an initial slash when the input already starts with a slash', function () {
            expect(startWithSlash('/test')).toBe('/test');
        });
        it('should not add an initial slash when the input is a slash', function () {
            expect(startWithSlash('/')).toBe('/');
        });
        it('should return a slash with the input is empty', function () {
            expect(startWithSlash('')).toBe('/');
        });
    });

});