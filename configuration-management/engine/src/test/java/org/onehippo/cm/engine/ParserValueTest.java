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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

import static org.junit.Assert.assertEquals;

public class ParserValueTest extends AbstractBaseTest {

    @Test
    public void expect_value_test_loads() throws IOException {
        final TestFiles files = collectFiles("/parser/value_test/repo-config.yaml");
        final ConfigurationParser parser = new ConfigurationParser();

        final Map<String, Configuration> configurations = parser.parse(files.repoConfig, files.sources);
        assertEquals(1, configurations.size());

        final Configuration base = assertConfiguration(configurations, "base", new String[0], 1);
        final Project project = assertProject(base, "project1", new String[0], 1);
        final Module module = assertModule(project, "module1", new String[0], 1);
        final Source source = assertSource(module, "value_test/repo-config/base.yaml", 2);

        final ConfigDefinition explicitDefinition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode explicitNode = assertNode(explicitDefinition, "/explicit", "/explicit", explicitDefinition, false, 0, 6);
        assertProperty(explicitNode, "binary", "/explicit/binary", explicitDefinition, false, "hello world".getBytes());
        assertProperty(explicitNode, "bool", "/explicit/bool", explicitDefinition, false, true);
        assertProperty(explicitNode, "float", "/explicit/float", explicitDefinition, false, 3.1415);
        assertProperty(explicitNode, "int", "/explicit/int", explicitDefinition, false, 42);
        assertProperty(explicitNode, "str", "/explicit/str", explicitDefinition, false, "hello world");
        assertProperty(explicitNode, "timestamp", "/explicit/timestamp", explicitDefinition, false,
                Date.from(ZonedDateTime.of(2015, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT+8")).toInstant()));

        final ConfigDefinition implicitDefinition = assertDefinition(source, 1, ConfigDefinition.class);
        final DefinitionNode implicitNode = assertNode(implicitDefinition, "/implicit", "/implicit", implicitDefinition, false, 0, 5);
        assertProperty(implicitNode, "boolsy", "/implicit/boolsy", implicitDefinition, false, true);
        assertProperty(implicitNode, "floatsy", "/implicit/floatsy", implicitDefinition, false, 3.1415);
        assertProperty(implicitNode, "intsy", "/implicit/intsy", implicitDefinition, false, 42);
        assertProperty(implicitNode, "strsy", "/implicit/strsy", implicitDefinition, false, "hello world");
        assertProperty(implicitNode, "timestampsy", "/implicit/timestampsy", implicitDefinition, false,
                Date.from(ZonedDateTime.of(2015, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT+8")).toInstant()));
    }

}
