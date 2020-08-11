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
package org.onehippo.cm.model.util;

import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.fail;

public class OverrideResidualMatchersTest {

    @Test
    public void correct_patterns() {
        Stream.of(
                "path: content"
        ).forEach(this::expectNoError);
    }

    @Test
    public void incorrect_patterns() {
        Stream.of(
                "path",
                "path:content",
                "path: foo",
                ":content",
                " :content"
        ).forEach(this::expectError);
    }

    private void expectNoError(final String string) {
        new OverrideResidualMatchers(string);
    }

    private void expectError(final String string) {
        try {
            new OverrideResidualMatchers(string);
            fail("Expected error parsing " + string);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

}
