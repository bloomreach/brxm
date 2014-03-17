/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.model.DependencyRestful;
import org.onehippo.cms7.essentials.dashboard.model.DependencyType;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class DependencyUtilsTest extends BaseResourceTest {


    @Test
    public void testHasDependency() throws Exception {

        final EssentialsDependency dependency = new DependencyRestful();
        dependency.setType("cms");
        dependency.setArtifactId("hippo-plugins-shared");
        dependency.setVersion("1.01.00-SNAPSHOT");
        dependency.setGroupId("org.onehippo.cms7.essentials");
        assertEquals(DependencyType.CMS, dependency.getDependencyType());
        final boolean hasDep = DependencyUtils.hasDependency(dependency);
        assertTrue("Expected hippo-plugins-shared version", hasDep);

    }

    @Test
    public void testAddRemoveDependency() throws Exception {
        final EssentialsDependency dependency = new DependencyRestful();
        dependency.setType("cms");
        dependency.setArtifactId("hippo-plugins-non-existing");
        dependency.setVersion("1.01.00-SNAPSHOT");
        dependency.setGroupId("org.onehippo.cms7.essentials");
        assertEquals(DependencyType.CMS, dependency.getDependencyType());
        boolean hasDep = DependencyUtils.hasDependency(dependency);
        assertFalse("Expected no dependency", hasDep);
        // add
        DependencyUtils.addDependency(dependency);
        hasDep = DependencyUtils.hasDependency(dependency);
        assertTrue("Expected hippo-plugins-non-existing", hasDep);
        // remove
        DependencyUtils.removeDependency(dependency);
        hasDep = DependencyUtils.hasDependency(dependency);
        assertFalse("Expected hippo-plugins-non-existing to be removed", hasDep);


    }
}
