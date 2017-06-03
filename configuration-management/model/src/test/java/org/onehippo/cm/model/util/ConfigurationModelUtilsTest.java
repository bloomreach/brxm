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
    public void validateItemCategory() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config.yaml", m1);
        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/a", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/a/b", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/a/b/c", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/a/b/c/d", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForProperty("/a/b/c/d", model));
        assertEquals(ConfigurationItemCategory.RUNTIME,       ConfigurationModelUtils.getCategoryForProperty("/a/b/c/e", model));

        assertEquals(ConfigurationItemCategory.CONTENT, ConfigurationModelUtils.getCategoryForNode("/a/b/c-content", model));
        assertEquals(ConfigurationItemCategory.CONTENT, ConfigurationModelUtils.getCategoryForNode("/a/b/c-content/d", model));
        assertEquals(ConfigurationItemCategory.CONTENT, ConfigurationModelUtils.getCategoryForProperty("/a/b/c-content/d", model));

        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/a/b/c-residual-content", model));
        assertEquals(ConfigurationItemCategory.CONTENT,       ConfigurationModelUtils.getCategoryForNode("/a/b/c-residual-content/d", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForProperty("/a/b/c-residual-content/d", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForProperty("/a/b/c-residual-content/e", model));

        assertEquals(ConfigurationItemCategory.RUNTIME, ConfigurationModelUtils.getCategoryForNode("/a/b/c-runtime", model));
        assertEquals(ConfigurationItemCategory.RUNTIME, ConfigurationModelUtils.getCategoryForNode("/a/b/c-runtime/d", model));
        assertEquals(ConfigurationItemCategory.RUNTIME, ConfigurationModelUtils.getCategoryForNode("/a/b/c-runtime/d/e", model));
        assertEquals(ConfigurationItemCategory.RUNTIME, ConfigurationModelUtils.getCategoryForProperty("/a/b/c-runtime/e", model));

        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForNode("/a/b/c-absent", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForProperty("/a/b/c-absent/d", model));

        assertEquals(ConfigurationItemCategory.RUNTIME,       ConfigurationModelUtils.getCategoryForNode("/absent", model));
        assertEquals(ConfigurationItemCategory.CONFIG, ConfigurationModelUtils.getCategoryForProperty("/absent", model));
    }
}
