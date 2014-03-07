/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UriValidatorTest {

    private final UriValidator validator = new UriValidator();

    @Test
    public void test_valid_urls() {
        final String[] validUrls = {
                "", "&", "~", "?", "!",
                "abc", "@home", "#", "Test-#",
                "a/b/c", "a%20b",
                "schema:host", "schema:/host", "schema://host",
                "s:////1./2.3.4", "s://h:123", "s://h1.2.h3/p1",
                "$xyz/a/b/c"
        };
        for (String each : validUrls) {
            assertThat("url = " + each, validator.apply(each), is(true));
        }
    }


    @Test
    public void test_invalid_urls() {
        final String[] validUrls = {
                ":", "{", "}", "\r\n", "\\",
                "!@#$%^&*()", ":x:x:x:",
                ":// . . /$",
                "s: //h", "s:// h", "s://h :p", "s://h:port/ path", "s://h/path/ path2",
                "${baseUrl}/a/b/c"
        };
        for (String each : validUrls) {
            assertThat("url = " + each, validator.apply(each), is(false));
        }
    }
}
