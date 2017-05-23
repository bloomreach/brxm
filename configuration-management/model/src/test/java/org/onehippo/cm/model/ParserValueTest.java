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
package org.onehippo.cm.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Test;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.parser.SourceParser;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParserValueTest extends AbstractBaseTest {

    @Test
    public void expect_value_test_loads() throws IOException, ParserException, URISyntaxException {
        final PathConfigurationReader.ReadResult result = readFromTestJar("/parser/value_test/"+ Constants.HCM_MODULE_YAML);
        final Map<String, GroupImpl> groups = result.getGroups();

        assertEquals(1, groups.size());

        final Group base = assertGroup(groups, "base", new String[0], 1);
        final Project project = assertProject(base, "project1", new String[0], 1);
        final Module module = assertModule(project, "module1", new String[0], 2);
        final Source source = assertSource(module, "base.yaml", 5);
        final Source contentSource = assertSource(module, "content.yaml", 1);

        final ContentDefinition contentDefinition = assertDefinition(contentSource, 0, ContentDefinition.class);
        final DefinitionNode contentDetectedNode = assertNode(contentDefinition, "/node", "node", contentDefinition, 0, 6);
        assertProperty(contentDetectedNode, "/node/double", "double", contentDefinition, ValueType.DOUBLE, 3.1415);
        assertProperty(contentDetectedNode, "/node/longAsInt", "longAsInt", contentDefinition, ValueType.LONG, (long) 42);
        assertProperty(contentDetectedNode, "/node/longAsLong", "longAsLong", contentDefinition, ValueType.LONG, 4200000000L);
        assertProperty(contentDetectedNode, "/node/string", "string", contentDefinition, ValueType.STRING, "hello world");


        final ConfigDefinition autoDetectedDefinition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode autoDetectedNode = assertNode(autoDetectedDefinition, "/autodetected", "autodetected", autoDetectedDefinition, 2, 6);
        assertProperty(autoDetectedNode, "/autodetected/boolean", "boolean", autoDetectedDefinition, ValueType.BOOLEAN, true);
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTimeInMillis(0);
        calendar.set(2015, 9, 21, 7, 28, 0);
        calendar.setLenient(false);
        assertProperty(autoDetectedNode, "/autodetected/date", "date", autoDetectedDefinition, ValueType.DATE, calendar);
        assertProperty(autoDetectedNode, "/autodetected/double", "double", autoDetectedDefinition, ValueType.DOUBLE, 3.1415);
        assertProperty(autoDetectedNode, "/autodetected/longAsInt", "longAsInt", autoDetectedDefinition, ValueType.LONG, (long) 42);
        assertProperty(autoDetectedNode, "/autodetected/longAsLong", "longAsLong", autoDetectedDefinition, ValueType.LONG, 4200000000L);
        assertProperty(autoDetectedNode, "/autodetected/string", "string", autoDetectedDefinition, ValueType.STRING, "hello world");
        final DefinitionNode nodeWithMixins = assertNode(autoDetectedNode, "/autodetected/node-with-mixins",
                "node-with-mixins", autoDetectedDefinition, 0, 2);
        assertProperty(nodeWithMixins, "/autodetected/node-with-mixins/jcr:primaryType", "jcr:primaryType", autoDetectedDefinition,
                ValueType.NAME, "some:type");
        assertProperty(nodeWithMixins, "/autodetected/node-with-mixins/jcr:mixinTypes", "jcr:mixinTypes", autoDetectedDefinition,
                ValueType.NAME, new String[]{"some:mixin", "some:otherMixin"});
        final DefinitionNode nodeWithEmptyMixins = assertNode(autoDetectedNode, "/autodetected/node-with-empty-mixins",
                "node-with-empty-mixins", autoDetectedDefinition, 0, 2);
        assertProperty(nodeWithEmptyMixins, "/autodetected/node-with-empty-mixins/jcr:primaryType", "jcr:primaryType", autoDetectedDefinition,
                ValueType.NAME, "some:type");
        assertProperty(nodeWithEmptyMixins, "/autodetected/node-with-empty-mixins/jcr:mixinTypes", "jcr:mixinTypes", autoDetectedDefinition,
                ValueType.NAME, new String[0]);

        final ConfigDefinition explicitDefinition = assertDefinition(source, 1, ConfigDefinition.class);
        final DefinitionNode explicitNode = assertNode(explicitDefinition, "/explicit", "explicit", explicitDefinition, 0, 12);
        assertProperty(explicitNode, "/explicit/binaryval", "binaryval", explicitDefinition, PropertyOperation.REPLACE, ValueType.BINARY, "binary.txt", true, false);
        assertProperty(explicitNode, "/explicit/decimal", "decimal", explicitDefinition, ValueType.DECIMAL,
                new BigDecimal("31415926535897932384626433832795028841971"));
        assertProperty(explicitNode, "/explicit/decimal-multi-value", "decimal-multi-value", explicitDefinition,
                ValueType.DECIMAL,
                new BigDecimal[] {
                        new BigDecimal("42"),
                        new BigDecimal("31415926535897932384626433832795028841971"),
                        new BigDecimal("4.2E+314159265")
                });

        assertProperty(explicitNode, "/explicit/binary-multi-string", "binary-multi-string", explicitDefinition,
                ValueType.STRING,
                new String[] {
                        "some","second", String.valueOf((char)27)
                });

        assertProperty(explicitNode, "/explicit/name", "name", explicitDefinition, ValueType.NAME, "prefix:local-name");
        assertProperty(explicitNode, "/explicit/path", "path", explicitDefinition, ValueType.PATH, "/path/to/something");
        assertProperty(explicitNode, "/explicit/reference", "reference", explicitDefinition, ValueType.REFERENCE,
                UUID.fromString("cafebabe-cafe-babe-cafe-babecafebabe"));
        assertProperty(explicitNode, "/explicit/reference-with-path", "reference-with-path",
                explicitDefinition, PropertyOperation.REPLACE, ValueType.REFERENCE, "/path/to/something", false, true);
        assertProperty(explicitNode, "/explicit/reference-with-multi-value-path", "reference-with-multi-value-path",
                explicitDefinition, PropertyOperation.REPLACE, ValueType.REFERENCE,
                new String[]{"/path/to/something", "/path/to/something-else"}, false, true);
        assertProperty(explicitNode, "/explicit/uri", "uri", explicitDefinition, ValueType.URI, new URI("http://onehippo.org"));
        assertProperty(explicitNode, "/explicit/weakreference", "weakreference", explicitDefinition, ValueType.WEAKREFERENCE,
                UUID.fromString("cafebabe-cafe-babe-cafe-babecafebabe"));

        final ConfigDefinition stringDefinition = assertDefinition(source, 2, ConfigDefinition.class);
        final DefinitionNode stringNode = assertNode(stringDefinition, "/string", "string", stringDefinition, 0, 9);
        assertProperty(stringNode, "/string/strBinary", "strBinary", stringDefinition, ValueType.STRING, String.valueOf((char)27));
        assertProperty(stringNode, "/string/strBoolean", "strBoolean", stringDefinition, ValueType.STRING, "true");
        assertProperty(stringNode, "/string/strDate", "strDate", stringDefinition, ValueType.STRING, "2015-10-21T07:28:00.000+08:00");
        assertProperty(stringNode, "/string/strDouble", "strDouble", stringDefinition, ValueType.STRING, "3.1415");
        assertProperty(stringNode, "/string/strLong", "strLong", stringDefinition, ValueType.STRING, "42");
        assertProperty(stringNode, "/string/strWithQuotes", "strWithQuotes", stringDefinition, ValueType.STRING, "string ' \"");
        assertProperty(stringNode, "/string/strWithLeadingSingleQuote", "strWithLeadingSingleQuote", stringDefinition, ValueType.STRING, "' \" string");
        assertProperty(stringNode, "/string/strWithLeadingDoubleQuote", "strWithLeadingDoubleQuote", stringDefinition, ValueType.STRING, "\" ' string");
        assertProperty(stringNode, "/string/strWithLineBreaks", "strWithLineBreaks", stringDefinition, ValueType.STRING, "line one\nline two\n");

        final ConfigDefinition emptyDefinition = assertDefinition(source, 3, ConfigDefinition.class);
        final DefinitionNode emptyNode = assertNode(emptyDefinition, "/empty", "empty", emptyDefinition, 0, 6);
        assertProperty(emptyNode, "/empty/emptyBinary", "emptyBinary", emptyDefinition, ValueType.BINARY, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyBoolean", "emptyBoolean", emptyDefinition, ValueType.BOOLEAN, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyDate", "emptyDate", emptyDefinition, ValueType.DATE, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyDouble", "emptyDouble", emptyDefinition, ValueType.DOUBLE, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyLong", "emptyLong", emptyDefinition, ValueType.LONG, new Object[0]);
        assertProperty(emptyNode, "/empty/emptyString", "emptyString", emptyDefinition, ValueType.STRING, new Object[0]);

        final ConfigDefinition categoryDefinition = assertDefinition(source, 4, ConfigDefinition.class);
        final DefinitionNode categoryTreeNode = assertNode(categoryDefinition, "/categories", "categories", categoryDefinition, 2, 2);
        assertNull(categoryTreeNode.getCategory());
        assertNull(categoryTreeNode.getResidualChildNodeCategory());
        final DefinitionProperty regularProperty = categoryTreeNode.getProperties().get("regular-property");
        assertNull(regularProperty.getCategory());
        final DefinitionProperty categoryProperty = categoryTreeNode.getProperties().get("runtime-property");
        assertEquals("runtime", categoryProperty.getCategory().toString());

        final DefinitionNode runtimeCategoryNode = assertNode(categoryTreeNode, "/categories/category", "category", categoryDefinition, 0, 0);
        assertEquals("runtime", runtimeCategoryNode.getCategory().toString());
        assertNull(runtimeCategoryNode.getResidualChildNodeCategory());
        final DefinitionNode residualChildNodes = assertNode(categoryTreeNode, "/categories/residual-child-node-category", "residual-child-node-category", categoryDefinition, 0, 0);
        assertNull(residualChildNodes.getCategory());
        assertEquals("content", residualChildNodes.getResidualChildNodeCategory().toString());
    }

    @Test
    public void expect_property_value_map_without_type_to_yield_string() throws ParserException {
        final SourceParser sourceParser = new ConfigSourceParser(DUMMY_RESOURCE_INPUT_PROVIDER);
        final GroupImpl group = new GroupImpl("group");
        final ProjectImpl project = new ProjectImpl("project", group);
        final ModuleImpl module = new ModuleImpl("module", project);

        final String yaml =
                "definitions:\n" +
                "  config:\n" +
                "    /node:\n" +
                "      property:\n" +
                "        value: []";
        final InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        sourceParser.parse(inputStream, "dummy.yaml", "dummy.yaml", module);

        final Source source = assertSource(module, "dummy.yaml", 1);
        final ConfigDefinition definition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode node = assertNode(definition, "/node", "node", definition, 0, 1);
        assertProperty(node, "/node/property", "property", definition, ValueType.STRING, new Object[0]);
    }

    @Test
    public void expect_date_without_time_to_yield_non_lenient_UTC_date() throws ParserException {
        final SourceParser sourceParser = new ConfigSourceParser(DUMMY_RESOURCE_INPUT_PROVIDER);
        final GroupImpl group = new GroupImpl("group");
        final ProjectImpl project = new ProjectImpl("project", group);
        final ModuleImpl module = new ModuleImpl("module", project);

        final String yaml =
                "definitions:\n" +
                "  config:\n" +
                "    /node:\n" +
                "      dateShort: 2015-10-21";
        final InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        sourceParser.parse(inputStream, "dummy.yaml", "dummy.yaml", module);

        final Source source = assertSource(module, "dummy.yaml", 1);
        final ConfigDefinition definition = assertDefinition(source, 0, ConfigDefinition.class);
        final DefinitionNode node = assertNode(definition, "/node", "node", definition, 0, 1);

        Calendar calendarShort = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendarShort.setTimeInMillis(0);
        calendarShort.set(2015, 9, 21);
        calendarShort.setLenient(false);
        assertProperty(node, "/node/dateShort", "dateShort", definition, ValueType.DATE, calendarShort);
    }

}
