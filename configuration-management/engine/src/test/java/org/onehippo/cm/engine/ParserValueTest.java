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
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

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
        final TestFiles files = collectFilesFromResource("/parser/value_test/repo-config.yaml");
        final ConfigurationParser parser = new ConfigurationParser();

        final Map<String, Configuration> configurations = parser.parse(files.repoConfig, files.sources);
        assertEquals(1, configurations.size());

        final Configuration base = assertConfiguration(configurations, "base", new String[0], 1);
        final Project project = assertProject(base, "project1", new String[0], 1);
        final Module module = assertModule(project, "module1", new String[0], 1);
        final Source source = assertSource(module, "base.yaml", 2);

        final ConfigDefinition baseDefinition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode baseNode = assertNode(baseDefinition, "/base", "base", baseDefinition, false, 0, 6);
        assertProperty(baseNode, "/base/binary", "binary", baseDefinition, "hello world".getBytes());
        assertProperty(baseNode, "/base/bool", "bool", baseDefinition, true);
        assertProperty(baseNode, "/base/float", "float", baseDefinition, 3.1415);
        assertProperty(baseNode, "/base/long", "long", baseDefinition, (long) 42);
        assertProperty(baseNode, "/base/str", "str", baseDefinition, "hello world");
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(2015, 9, 21, 7, 28, 0);
        assertProperty(baseNode, "/base/timestamp", "timestamp", baseDefinition, calendar);

        final ConfigDefinition stringDefinition = assertDefinition(source, 1, ConfigDefinition.class);
        final DefinitionNode stringNode = assertNode(stringDefinition, "/string", "string", stringDefinition, false, 0, 8);
        assertProperty(stringNode, "/string/strBool", "strBool", stringDefinition, "true");
        assertProperty(stringNode, "/string/strFloat", "strFloat", stringDefinition, "3.1415");
        assertProperty(stringNode, "/string/strLong", "strLong", stringDefinition, "42");
        assertProperty(stringNode, "/string/strTimestamp", "strTimestamp", stringDefinition, "2015-10-21T07:28:00+8:00");
        assertProperty(stringNode, "/string/strWithQuotes", "strWithQuotes", stringDefinition, "string ' \"");
        assertProperty(stringNode, "/string/strWithLeadingSingleQuote", "strWithLeadingSingleQuote", stringDefinition, "' \" string");
        assertProperty(stringNode, "/string/strWithLeadingDoubleQuote", "strWithLeadingDoubleQuote", stringDefinition, "\" ' string");
        assertProperty(stringNode, "/string/strWithLineBreaks", "strWithLineBreaks", stringDefinition, "line one\nline two\n");
    }

}
