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

describe('the incrementProperty filter', function () {
    'use strict';

    var incrementPropertyFilter;

    beforeEach(module('hippo.channel.menu'));
    beforeEach(inject(function (_incrementPropertyFilter_) {
        incrementPropertyFilter = _incrementPropertyFilter_;
    }));

    var items = [
            {
                name: 'Test'
            }
        ],
        items1 = [
            {
                name: 'Untitled'
            },
        ],
        items2 = [
            {
                name: 'Untitled'
            },
            {
                name: 'Untitled (1)'
            },
        ],
        items3 = [
            {
                name: 'Untitled'
            },
            {
                name: 'Untitled (1)'
            },
            {
                name: 'Untitled (5)'
            },
        ],
        items4 = [
            {
                name: 'Untitled',
                items: [
                    {
                        name: 'Untitled (12)',
                        items: [
                            {
                                name: 'Untitled (17)'
                            }
                        ]
                    }
                ]
            },
            {
                name: 'Untitled (5)'
            }
        ];

    it('should add a number to an item without any increments', function () {
        expect(incrementPropertyFilter(items, 'name', 'Untitled')).toEqual('Untitled');
    });

    it('should add an increment if one doesnt exist', function () {
        expect(incrementPropertyFilter(items1, 'name', 'Untitled')).toEqual('Untitled (1)');
    });

    it('should increment linearly', function () {
        expect(incrementPropertyFilter(items2, 'name', 'Untitled')).toEqual('Untitled (2)');
    });

    it('should increment after the highest value', function () {
        expect(incrementPropertyFilter(items3, 'name', 'Untitled')).toEqual('Untitled (6)');
    });

    it('should search sub collections', function () {
        expect(incrementPropertyFilter(items4, 'name', 'Untitled', 'items')).toEqual('Untitled (18)');
    });

});