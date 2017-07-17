/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

public class ResourceBundleLoaderTest {

    @Test
    public void testGetHippoArtifactsOnClasspath() throws Exception {
        final Collection<ArtifactInfo> infos = new ArrayList<>();
        new ResourceBundleLoader(Arrays.asList("en"), getClass().getClassLoader()) {
            @Override
            protected void collectResourceBundles(final ArtifactInfo artifactInfo, final Collection<ResourceBundle> bundles) throws IOException {
                infos.add(artifactInfo);
            }
        }.loadBundles();

        ArtifactInfo dummyInfo = Lists.newArrayList(infos).get(infos.size() - 1);
        assertEquals("hippo-cms-l10n-tooling-test-dummy", dummyInfo.getArtifactId());
        assertEquals("org.onehippo.cms.l10n", dummyInfo.getGroupId());
        assertEquals(8, dummyInfo.getEntries().size());
    }

}
