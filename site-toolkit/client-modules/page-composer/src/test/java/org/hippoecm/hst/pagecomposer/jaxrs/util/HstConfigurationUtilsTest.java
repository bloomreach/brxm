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
package org.hippoecm.hst.pagecomposer.jaxrs.util;


import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HstConfigurationUtilsTest {

    @Test
    public void upstream_changes_are_filtered() throws Exception {
        final String[] paths = new String[]{
                "/hst:hst/hst:configurations/myproject",
                "/hst:hst/hst:configurations/myproject/hst:sitemap",
                "/hst:hst/hst:configurations/myproject/hst:workspace",
                "/hst:hst/hst:configurations/myproject/hst:upstream",
                "/hst:hst/hst:configurations/myproject/hst:upstream/hst:sitemap"
        };

        String[] filteredPaths = HstConfigurationUtils.filterOutUpstream(paths);

        List<String> list = Arrays.asList(filteredPaths);
        assertTrue(list.contains("/hst:hst/hst:configurations/myproject"));
        assertTrue(list.contains("/hst:hst/hst:configurations/myproject/hst:sitemap"));
        assertTrue(list.contains("/hst:hst/hst:configurations/myproject/hst:workspace"));
        assertFalse(list.contains("/hst:hst/hst:configurations/myproject/hst:upstream"));
        assertFalse(list.contains("/hst:hst/hst:configurations/myproject/hst:upstream/hst:sitemap"));
    }
}
