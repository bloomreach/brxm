/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCorsSupportValve {

    @Test
    public void test_merge_headers_sorts_the_headers() {
        CorsSupportValve valve = new CorsSupportValve();

        final String merge = valve.merge(ImmutableList.of("foo", "bar"), "lux1", "lux2");

        assertThat(merge).isEqualTo("bar, foo, lux1, lux2");
    }

    @Test
    public void test_merge_headers_ignores_null_values() {
        CorsSupportValve valve = new CorsSupportValve();

        final String merge = valve.merge(ImmutableList.of("foo"), "lux1", null);

        assertThat(merge).isEqualTo("foo, lux1");
    }

    @Test
    public void test_merge_headers_ignores_blank_values() {
        CorsSupportValve valve = new CorsSupportValve();

        final String merge = valve.merge(ImmutableList.of("foo", ""), "lux1", "   ");

        assertThat(merge).isEqualTo("foo, lux1");
    }

    @Test
    public void test_merge_headers_sorts_the_headers_multiple_headers_in_single_string() {
        CorsSupportValve valve = new CorsSupportValve();

        final String merge = valve.merge(ImmutableList.of("foo1, foo2  , foo3, , ,  ", "bar"), "lux1", "lux2, lux3");

        assertThat(merge).isEqualTo("bar, foo1, foo2, foo3, lux1, lux2, lux3");
    }
}
