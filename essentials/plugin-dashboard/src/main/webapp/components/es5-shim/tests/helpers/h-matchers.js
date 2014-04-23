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

beforeEach(function() {
    this.addMatchers({
        toExactlyMatch: function(expected) {
            var a1, a2,
                l, i,
                key,
                actual = this.actual;
            
            var getKeys = function(o) {
                var a = [];
                for(key in o) {
                    if(o.hasOwnProperty(key)) {
                        a.push(key);
                    }
                }
                return a;
            }
            a1 = getKeys(actual);
            a2 = getKeys(expected);
            
            l = a1.length;
            if(l !== a2.length) {
                return false;
            }
            for(i = 0; i < l; i++) {
                key = a1[i];
                expect(key).toEqual(a2[i]);
                expect(actual[key]).toEqual(expected[key]);
            }
            
            return true;
        }
    })
});
