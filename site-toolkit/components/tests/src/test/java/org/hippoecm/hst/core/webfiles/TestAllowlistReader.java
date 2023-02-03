/*
 *  Copyright 2015-2023 Bloomreach
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
package org.hippoecm.hst.core.webfiles;

import java.io.InputStream;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestAllowlistReader {

    @Test
    public void test_reading_allowlist_file() throws Exception {

        InputStream input = null;
        try {
            input = TestAllowlistReader.class.getResourceAsStream("allowlistTest.txt");
            final AllowlistReader allowlistReader = new AllowlistReader(input);

            // see "allowlistTest.txt"
            final Set<String> expectedAllowlist = new ImmutableSet.Builder<String>().add("/css/test",
                    "foo",
                    "js/",
                    "test.txt",
                    "font/",
                    "foo/123/b_ar",
                    "/lux/test/",
                    "/lux/test",
                    "lux/ /foo").build();

            final Set<String> allowlist = allowlistReader.getAllowlist();
            assertEquals(expectedAllowlist, allowlist);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }
}
