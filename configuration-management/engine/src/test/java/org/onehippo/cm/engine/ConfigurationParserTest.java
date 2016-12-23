/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

import static org.junit.Assert.assertEquals;

public class ConfigurationParserTest extends AbstractBaseTest {

    @Test
    public void expect_hello_world_loads() throws IOException {
        final TestFiles files = collectFiles("/hello_world/repo-config.yaml");
        final ConfigurationParser parser = new ConfigurationParser();

        final Map<String, Configuration> configurations = parser.parse(files.repoConfig, files.sources);
        assertEquals(2, configurations.size());

        final Configuration base = assertConfiguration(configurations, "base", new String[0], 1);
        final Project project1 = assertProject(base, "project1", new String[0], 1);
        final Module module1 = assertModule(project1, "module1", new String[0], 1);
        final Source source1 = assertSource(module1, "hello_world/repo-config/base/project1/module1/config.yaml", 1);
        final ConfigDefinition definition1 = assertDefinition(source1, 0, ConfigDefinition.class);

        final DefinitionNode rootDefinition1 =
                assertNode(definition1, "/", "/", definition1, false, 1, 0);
        final DefinitionNode nodeDefinition1 =
                assertNode(rootDefinition1, "node", "/node", false, definition1, false, 0, 2);
        final DefinitionProperty property1 =
                assertProperty(nodeDefinition1, "property1", "/node/property1", definition1, false, "value1");
        final DefinitionProperty property2 =
                assertProperty(nodeDefinition1, "property2", "/node/property2", definition1, false, new String[]{"value2","value3"});

        final Configuration myhippoproject = assertConfiguration(configurations, "myhippoproject", new String[]{"base"}, 1);
        final Project project2 = assertProject(myhippoproject, "project2", new String[0], 1);
        final Module module2 = assertModule(project2, "module2", new String[0], 1);
        final Source source2 = assertSource(module2, "hello_world/repo-config/myhippoproject/project2/module2/config.yaml", 1);
        final ConfigDefinition definition2 = assertDefinition(source2, 0, ConfigDefinition.class);

        final DefinitionNode rootDefinition2 =
                assertNode(definition2, "/node", "/node", definition2, false, 0, 2);
        final DefinitionProperty property2bis =
                assertProperty(rootDefinition2, "property2", "/node/property2", definition2, false, "override");
        final DefinitionProperty property3 =
                assertProperty(rootDefinition2, "property3", "/node/property3", definition2, false, "value3");
    }

}
