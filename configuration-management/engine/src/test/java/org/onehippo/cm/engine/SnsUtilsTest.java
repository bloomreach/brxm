/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cm.engine;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SnsUtilsTest {

    @Test
    public void test_canonical_sns() {
        assertEquals(Pair.of("sns", 1), SnsUtils.splitIndexedName("sns[1]"));
    }

    @Test
    public void test_sns_with_relative_index() {
        assertEquals(Pair.of("sns", -1), SnsUtils.splitIndexedName("sns[+1]"));
    }

    @Test
    public void test_sns_without_index() {
        assertEquals(Pair.of("sns", 0), SnsUtils.splitIndexedName("sns"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_negative_index() {
        SnsUtils.splitIndexedName("sns[-1]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_zero_index() {
        SnsUtils.splitIndexedName("sns[0]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_non_numeric_index() {
        SnsUtils.splitIndexedName("sns[foo]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_trailing_chars() {
        SnsUtils.splitIndexedName("sns[1]sns");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_without_name() {
        SnsUtils.splitIndexedName("[0]");
    }

}
