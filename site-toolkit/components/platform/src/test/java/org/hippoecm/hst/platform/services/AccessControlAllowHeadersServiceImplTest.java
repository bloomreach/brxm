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
package org.hippoecm.hst.platform.services;

import java.util.Collections;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccessControlAllowHeadersServiceImplTest {

    @Test
    public void test_allowed_headers_empty() {

        final AccessControlAllowHeadersServiceImpl service = new AccessControlAllowHeadersServiceImpl();

        assertThat(service.getAllAllowedHeaders()).isEmpty();

        assertThatThrownBy(() -> service.getAllAllowedHeaders().put("foo", Collections.singletonList("bar")))
                .as("AllowedHeaders should be immutable")
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(service.getAllowedHeaders("foo")).isNull();

        assertThat(service.getAllowedHeadersString()).isEmpty();
    }

    @Test
    public void test_allowed_headers_single_value() {

        final AccessControlAllowHeadersServiceImpl service = new AccessControlAllowHeadersServiceImpl();

        service.setAllowedHeaders("module1", Collections.singletonList("bar"));

        assertThat(service.getAllAllowedHeaders()).size().isEqualTo(1);

        assertThat(service.getAllowedHeaders("module1")).containsExactly("bar");

        assertThatThrownBy(() -> service.getAllowedHeaders("module1").add("test"))
                .as("AllowedHeaders for module should be immutable")
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(service.getAllowedHeadersString()).isEqualTo("bar");

        service.setAllowedHeaders("module1", Collections.singletonList("foo"));

        assertThat(service.getAllAllowedHeaders()).size()
                .as("Setting module1 again replaces earlier list and does not append the new value")
                .isEqualTo(1);

        service.setAllowedHeaders("module1", Collections.singletonList("foo"));

    }

    @Test
    public void test_allowed_headers_ordered_alphabetically() {
        final AccessControlAllowHeadersServiceImpl service = new AccessControlAllowHeadersServiceImpl();

        service.setAllowedHeaders("module1", ImmutableList.of("d", "c"));
        service.setAllowedHeaders("module2", ImmutableList.of("b", "a"));

        assertThat(service.getAllowedHeadersString()).isEqualTo("a, b, c, d");
    }

    @Test
    public void test_allowed_headers_removes_duplicates() {
        final AccessControlAllowHeadersServiceImpl service = new AccessControlAllowHeadersServiceImpl();

        service.setAllowedHeaders("module1", ImmutableList.of("a", "c"));
        service.setAllowedHeaders("module2", ImmutableList.of("a", "a"));

        assertThat(service.getAllowedHeadersString()).isEqualTo("a, c");
    }

    @Test
    public void test_allowed_headers_removes_blank_values() {
        final AccessControlAllowHeadersServiceImpl service = new AccessControlAllowHeadersServiceImpl();

        service.setAllowedHeaders("module1", ImmutableList.of("d", ""));
        service.setAllowedHeaders("module2", ImmutableList.of("   ", "a"));

        assertThat(service.getAllowedHeadersString()).isEqualTo("a, d");
    }
}
