/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyParserTest {

    @Before
    public void setUp() {
        System.setProperty("foo", "fooVal");
        System.setProperty("bar", "barVal");
    }

    @After
    public void destroy() {
        System.clearProperty("foo");
        System.clearProperty("bar");
    }

    @Test
    public void default_property_parser_with_unresolvable_placeholders_behavior() throws Exception {
        final PropertyParser pp = new PropertyParser(System.getProperties());

        assertThat((String) pp.resolveProperty("name", "test-value"))
                .isEqualTo("test-value");


        assertThat((String) pp.resolveProperty("name", "test-value-${foo}"))
                .isEqualTo("test-value-fooVal");

        assertThat((String) pp.resolveProperty("name", "test-value-${foo}-${bar}"))
                .isEqualTo("test-value-fooVal-barVal");

        // unresolvable properties result in null
        assertThat((String) pp.resolveProperty("name", "test-value-${unresolvable}"))
                .as("unresolvable expected to result in null")
                .isNull();

        // default pp does not have default value support
        assertThat((String) pp.resolveProperty("name", "test-value-${unresolvable:someDefaultValue}"))
                .as("no default value support expected")
                .isNull();

        // default pp does not have default value support
        assertThat((String) pp.resolveProperty("name", "test-value-${foo:someDefaultValue}"))
                .as("no default value support expected")
                .isNull();

        final PropertyParser ppWithIgnore = new PropertyParser(System.getProperties(),
                "${", "}", null, true);


        assertThat((String) ppWithIgnore.resolveProperty("name", "test-value-${foo}-${bar}-${unresolvable}"))
                .isEqualTo("test-value-fooVal-barVal-${unresolvable}");

        assertThat((String) ppWithIgnore.resolveProperty("name", "test-value-${unresolvable:someDefaultValue}"))
                .as("no default value support expected")
                .isEqualTo("test-value-${unresolvable:someDefaultValue}");


    }

    @Test
    public void property_parser_with_value_separator_replaces_unresolvable_placeholders_with_default() throws Exception {
        final PropertyParser ppWithIgnore = new PropertyParser(System.getProperties(),
                "${", "}", ":", true);
        final PropertyParser ppNoIgnore = new PropertyParser(System.getProperties(),
                "${", "}", ":", false);

        assertThat((String) ppNoIgnore.resolveProperty("name", "test-value-${unresolvable}"))
                .isNull();

        assertThat((String) ppWithIgnore.resolveProperty("name", "test-value-${unresolvable}"))
                .isEqualTo("test-value-${unresolvable}");

        assertThat((String) ppNoIgnore.resolveProperty("name", "test-value-${unresolvable:defaultValue}"))
                .isEqualTo("test-value-defaultValue");

        assertThat((String) ppWithIgnore.resolveProperty("name", "test-value-${unresolvable:defaultValue}"))
                .isEqualTo("test-value-defaultValue");

        // default value with a : in it...
        assertThat((String) ppWithIgnore.resolveProperty("name", "test-value-${unresolvable:defaultValue:bla:flx}"))
                .isEqualTo("test-value-defaultValue:bla:flx");

        // default value with a url
        assertThat((String) ppWithIgnore.resolveProperty("name", "test-value-${unresolvable:http://locahost:8080/site}"))
                .isEqualTo("test-value-http://locahost:8080/site");

        // default value with a url
        assertThat((String) ppWithIgnore.resolveProperty("name", "${foo}-value-${unresolvable:http://locahost:8080/site}"))
                .isEqualTo("fooVal-value-http://locahost:8080/site");
    }
}
