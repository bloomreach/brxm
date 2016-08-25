/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms.l10n.IncludeLocalesMojo.findPreviousMicroVersion;

public class IncludeLocalesMojoTest {

    @Test
    public void find_previous_micro_version_string() {

        assertEquals("5", findPreviousMicroVersion("6"));

        assertEquals("6", findPreviousMicroVersion("6-SNAPSHOT"));
        assertEquals("6-SNAPSHOT", findPreviousMicroVersion("6-cmng-psp1-SNAPSHOT"));
        // we do not expect micro version to be prepended with '0' version. If that happens, most likely
        // fallback locales won't work because the fallback from '06' will be '5' and not '05'.
        // we could support that '06' would fallback to '05' instead of '5' but then what would be the
        // fallback of version '10'? Would it be '9' or '09'. We can't know. Hence assume
        // MAJOR.MINOR.MICRO scheme to not have versions prepended with a '0'
        assertEquals("6-SNAPSHOT", findPreviousMicroVersion("06-cmng-psp1-SNAPSHOT"));
        assertEquals("10-SNAPSHOT", findPreviousMicroVersion("10-cmng-psp1-SNAPSHOT"));
        assertEquals("11-SNAPSHOT", findPreviousMicroVersion("11-cmng-psp1-SNAPSHOT"));

        assertEquals("0", findPreviousMicroVersion("0-rc-1"));
        assertEquals("1", findPreviousMicroVersion("1-rc-1"));

        // strange micro version number
        assertEquals("1-SNAPSHOT", findPreviousMicroVersion("1abc-cmng-psp1-SNAPSHOT"));


        assertEquals("1-SNAPSHOT", findPreviousMicroVersion("1-xyz-psp1-SNAPSHOT"));
        // 4.1.0-xyz-psp1-SNAPSHOT should result that first '4.1.0-SNAPSHOT' is attempted
        assertEquals("0-SNAPSHOT", findPreviousMicroVersion("0-xyz-psp1-SNAPSHOT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegal_micro_version_string() {
        findPreviousMicroVersion("x-SNAPSHOT");
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegal_micro_version_string_2() {
        findPreviousMicroVersion("x34-SNAPSHOT");
    }

    @Test(expected = IllegalArgumentException.class)
    public void null_micro_version_string() {
        findPreviousMicroVersion(null);
    }


    @Test
    public void test_mapping(){
        final IncludeLocalesMojo includeLocalesMojo = new IncludeLocalesMojo();
        List<String> artifactPrefixes = new ArrayList<>();
        // valid
        artifactPrefixes.add("org.onehippo.cms7:hippo-cms-engine,hippo-cms-l10n");
        // invalid
        artifactPrefixes.add("hippo-cms-engine,hippo-cms-l10n");
        // invalid
        artifactPrefixes.add("org.onehippo.cms7:hippo-cms-engine,org.onehippo.cms7:hippo-cms-l10n");
        // invalid
        artifactPrefixes.add("org.onehippo.cms7:hippo-cms-engine");

        final Map<String, String> map = includeLocalesMojo.map(artifactPrefixes);

        assertEquals(1, map.size());
        assertEquals("org.onehippo.cms7:hippo-cms-engine",map.keySet().iterator().next());
        assertEquals("hippo-cms-l10n",map.values().iterator().next());
    }
}
