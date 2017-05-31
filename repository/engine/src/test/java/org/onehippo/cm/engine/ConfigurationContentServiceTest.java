package org.onehippo.cm.engine;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onehippo.cm.model.ActionItem;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
public class ConfigurationContentServiceTest extends RepositoryTestCase {

    @Test
    public void validateContentNode() throws Exception {

        final ConfigurationContentService configurationContentService = new ConfigurationContentService();

        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/content.yaml", m1, false);

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();
        assertTrue(configurationContentService.isContentNodePathValid(model.getContentDefinitions().get(0).getModifiableNode().getPath(), model));
    }

    @Test
    public void validateContentNode_strict() throws Exception {

        final ConfigurationContentService configurationContentService = new ConfigurationContentService();

        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config-strict.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/content.yaml", m1, false);

        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();
        assertFalse(configurationContentService.isContentNodePathValid(model.getContentDefinitions().get(0).getModifiableNode().getPath(), model));
    }

    @Test
    public void validateDeleteActions_delete_content() throws Exception {
        final ConfigurationContentService configurationContentService = new ConfigurationContentService();

        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/content.yaml", m1, false);

        List<ActionItem> actionItems = new ArrayList<>();
        actionItems.add(new ActionItem("/a1/a2/a3/a4", ActionType.DELETE));

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();
        configurationContentService.validateDeleteActions(model, actionItems);

    }

    @Test(expected = RuntimeException.class)
    public void validateDeleteActions_delete_config() throws Exception {
        final ConfigurationContentService configurationContentService = new ConfigurationContentService();

        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config-strict.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/content.yaml", m1, false);

        final List<ActionItem> actionItems = new ArrayList<>();
        actionItems.add(new ActionItem("/a1/a2/a3", ActionType.DELETE));

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();
        configurationContentService.validateDeleteActions(model, actionItems);

    }

    @Test
    public void validateDeleteActions_delete_config_node() throws Exception {

        final ConfigurationContentService configurationContentService = new ConfigurationContentService();

        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/config-strict.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/content.yaml", m1, false);

        final List<ActionItem> actionItems = new ArrayList<>();
        actionItems.add(new ActionItem("/a1/a2/a3/a4/b", ActionType.DELETE));

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        try {
            configurationContentService.validateDeleteActions(model, actionItems);
            fail("Exception should be thrown");

        } catch(ConfigurationRuntimeException ignored) {}


    }

}