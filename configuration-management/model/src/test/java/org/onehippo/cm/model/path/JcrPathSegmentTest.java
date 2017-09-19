/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.path;

import org.junit.Assert;
import org.junit.Test;

public class JcrPathSegmentTest {

    @Test
    public void test_canonical_sns() {
        Assert.assertEquals(JcrPaths.getSegment("sns", 1), JcrPaths.getSegment("sns[1]"));
    }

    @Test
    public void test_sns_without_index() {
        Assert.assertEquals(JcrPaths.getSegment("sns", 0), JcrPaths.getSegment("sns"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_negative_index() {
        JcrPaths.getSegment("sns[-1]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_zero_index() {
        JcrPaths.getSegment("sns[0]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_non_numeric_index() {
        JcrPaths.getSegment("sns[foo]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_with_trailing_chars() {
        JcrPaths.getSegment("sns[1]sns");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_sns_without_name() {
        JcrPaths.getSegment("[1]");
    }

}
