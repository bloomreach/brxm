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
package org.onehippo.cm.engine.autoexport;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class InjectResidualMatchersTest {

    @Test
    public void correct_patterns() {
        Stream.of(
                "/path: content",
                "/path[type]: content",
                "/path[some:type]: content",
                "/path[1][some:type]: content",
                "/path[1]/text: content"
        ).forEach(this::expectNoError);
    }

    @Test
    public void incorrect_patterns() {
        Stream.of(
                "/path",
                "/path:content",
                "/path: foo",
                "/path[]: content",
                ": content",
                " : content",
                "[some:type]: content",
                " [some:type]: content",
                "/path]some:type[: content",
                "/path[some:type]text: content"
        ).forEach(this::expectError);
    }

    @Test
    public void test_sns() {
        final InjectResidualMatchers.Matcher matcher = new InjectResidualMatchers.Matcher("/path[1]: content");
        assertEquals("/path[1]", matcher.patternString);
        assertNull(matcher.nodeType);
    }

    private void expectNoError(final String string) {
        new InjectResidualMatchers(Stream.of(string).collect(Collectors.toList()));
    }

    private void expectError(final String string) {
        try {
            new InjectResidualMatchers(Stream.of(string).collect(Collectors.toList()));
            fail("Expected error parsing " + string);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

}
