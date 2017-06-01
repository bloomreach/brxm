/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.model.util;

import org.junit.Test;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.ModuleImpl;

import static org.junit.Assert.assertEquals;

public class ConfigurationModelUtilsTest {

    @Test
    public void validateNodeCategory() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config.yaml", m1);
        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        assertEquals(ConfigurationModelUtils.getCategoryForNode("/", model), ConfigurationItemCategory.CONFIGURATION);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a", model), ConfigurationItemCategory.CONFIGURATION);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b", model), ConfigurationItemCategory.CONFIGURATION);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c", model), ConfigurationItemCategory.CONFIGURATION);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c/d", model), ConfigurationItemCategory.CONFIGURATION);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c/d/e", model), ConfigurationItemCategory.CONFIGURATION);

        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c-content/d/e", model), ConfigurationItemCategory.CONTENT);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c-content/d", model), ConfigurationItemCategory.CONTENT);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c-content", model), ConfigurationItemCategory.CONFIGURATION);

        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c-runtime", model), ConfigurationItemCategory.RUNTIME);
        assertEquals(ConfigurationModelUtils.getCategoryForNode("/a/b/c-absent", model), ConfigurationItemCategory.CONFIGURATION);
    }
}
