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
package org.onehippo.cm.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.testutils.jcr.event.EventCollector;
import org.onehippo.testutils.jcr.event.EventPojo;
import org.onehippo.testutils.jcr.event.ExpectedEvents;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModelTestUtils;
import org.onehippo.cm.impl.model.builder.MergedModelBuilder;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cm.impl.model.ModelTestUtils.parseNoSort;

public class ConfigurationPersistenceServiceTest extends RepositoryTestCase {

    private Node testNode;

    @Before
    public void createTestNode() throws RepositoryException {
        testNode = session.getRootNode().addNode("test");
        session.save();
    }

    @Test
    public void expect_namespaces_and_cnds_to_be_registered() throws Exception {
        final String source
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test\n"
                + "    uri: http://www.onehippo.org/test/nt/1.0\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test'='http://www.onehippo.org/test/nt/1.0'>\n"
                + "    [test:type] > nt:base\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/node", JCR_PRIMARYTYPE);

        applyDefinitions(source, expectedEvents);

        expectNode("/test/node", "[]", "[jcr:primaryType]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test:type");
    }

    @Test
    public void expect_cnd_reloads_to_work() throws Exception {
        /* Test in three steps:
         *  - step 1: load a basic cnd for a node type that does not allow sibling properties
         *  - step 2: validate that it is not possible to create a sibling property
         *  - step 3: reload the cnd, allowing a sibling property and test it is possible to load some content
         */

        // step 1
        final String startConfiguration
                = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: test\n"
                + "    uri: http://www.onehippo.org/test/nt/1.0\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test'='http://www.onehippo.org/test/nt/1.0'>\n"
                + "    [test:type] > nt:base\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "";

        applyDefinitions(startConfiguration);

        expectNode("/test/node", "[]", "[jcr:primaryType]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test:type");

        // step 2
        final String additionalPropertyConfiguration
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "        test:property: value\n"
                + "";

        try {
            applyDefinitions(additionalPropertyConfiguration);
        } catch (Exception e) {
            assertEquals(
                    "Failed to process property '/test/node/test:property' defined through"
                    + " [test-configuration/test-project/test-module-0 [string]]: no matching property definition"
                    + " found for {http://www.onehippo.org/test/nt/1.0}property",
                    e.getMessage());
        }

        // step 3
        final String reregisterConfiguration
                = "definitions:\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    <'test'='http://www.onehippo.org/test/nt/1.0'>\n"
                + "    [test:type] > nt:base\n"
                + "     - test:property (string)\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "        test:property: value\n"
                + "";

        applyDefinitions(reregisterConfiguration);

        expectNode("/test/node", "[]", "[jcr:primaryType, test:property]");
        expectProp("/test/node/jcr:primaryType", PropertyType.NAME, "test:type");
        expectProp("/test/node/test:property", PropertyType.STRING, "value");
    }

    @Test
    public void expect_parse_error_in_cnd() throws Exception {
        final String source
                = "definitions:\n"
                + "  cnd:\n"
                + "  - |\n"
                + "    <'nt'='http://www.jcp.org/jcr/nt/1.0'>\n"
                + "    [unknown:type] > nt:unstructured\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /node:\n"
                + "        jcr:primaryType: test:type\n"
                + "";

        try {
            applyDefinitions(source);
            fail("an exception should have occurred");
        } catch (RepositoryException e) {
            assertTrue(e.getMessage().contains("Failed to parse cnd test-configuration/test-project/test-module-0 [string]"));
        }
    }

    @Test
    public void expect_unchanged_existing_properties_to_be_untouched() throws Exception {
        setProperty("/test", "single", PropertyType.STRING, "org");
        setProperty("/test", "multiple", PropertyType.STRING, new String[]{"org1","org2"});

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: org\n"
                + "      multiple: [org1, org2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents(); // aka, expect to see no events

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "org");
        expectProp("/test/multiple", PropertyType.STRING, "[org1, org2]");
    }

    @Test
    public void expect_new_properties_to_be_created() throws Exception {
        // no initial content

        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: new\n"
                + "      multiple: [new1, new2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/single")
                .expectPropertyAdded("/test/multiple");

        applyDefinitions(definition, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProp("/test/single", PropertyType.STRING, "new");
        expectProp("/test/multiple", PropertyType.STRING, "[new1, new2]");
    }

    @Test
    public void expect_updated_properties_to_be_updated() throws Exception {
        setProperty("/test", "single", PropertyType.STRING, "org");
        setProperty("/test", "multiple", PropertyType.STRING, new String[]{"org1","org2"});
        setProperty("/test", "reordered", PropertyType.STRING, new String[]{"new2","new1"});

        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      single: new\n"
                + "      multiple: [new1, new2]\n"
                + "      reordered: [new1, new2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/single")
                .expectPropertyChanged("/test/multiple")
                .expectPropertyChanged("/test/reordered");

        applyDefinitions(definition, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, reordered, single]");
        expectProp("/test/single", PropertyType.STRING, "new");
        expectProp("/test/multiple", PropertyType.STRING, "[new1, new2]");
        expectProp("/test/reordered", PropertyType.STRING, "[new1, new2]");
    }

    @Test
    public void expect_deleted_properties_to_be_gone() throws Exception {
        setProperty("/test", "not-in-config", PropertyType.STRING, "value");
        setProperty("/test", "explicitly-deleted", PropertyType.STRING, "value");

        final String definition1
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      explicitly-deleted: value\n"
                + "      explicitly-deleted-non-existing: value\n"
                + "";
        final String definition2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      explicitly-deleted:\n"
                + "        operation: delete\n"
                + "      explicitly-deleted-non-existing:\n"
                + "        operation: delete\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyRemoved("/test/not-in-config")
                .expectPropertyRemoved("/test/explicitly-deleted");

        applyDefinitions(new String[]{definition1,definition2}, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType]");
    }

    @Test
    public void expect_propertytype_overrides_to_be_applied_if_necessary() throws Exception {
        setProperty("/test", "incorrect-should-be-single", PropertyType.STRING, new String[]{"org1","org2"});
        setProperty("/test", "incorrect-should-be-long", PropertyType.STRING, new String[]{"org1","org2"});
        setProperty("/test", "already-changed-to-single", PropertyType.STRING, "new");
        setProperty("/test", "already-changed-to-long", PropertyType.LONG, new String[]{"42","31415"});

        final String definition1
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      incorrect-should-be-single: [org1, org2]\n"
                + "      incorrect-should-be-long: [org1, org2]\n"
                + "      already-changed-to-single: [org1, org2]\n"
                + "      already-changed-to-long: [org1, org2]\n"
                + "      not-yet-existing: [org1, org2]\n"
                + "";
        final String definition2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      incorrect-should-be-single:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "      incorrect-should-be-long:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: [42, 31415]\n"
                + "      already-changed-to-single:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "      already-changed-to-long:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: [42, 31415]\n"
                + "      not-yet-existing:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/incorrect-should-be-single")
                .expectPropertyChanged("/test/incorrect-should-be-long")
                .expectPropertyAdded("/test/not-yet-existing");

        applyDefinitions(new String[]{definition1,definition2}, expectedEvents);

        expectNode("/test", "[]", "[already-changed-to-long, already-changed-to-single, incorrect-should-be-long, "
                + "incorrect-should-be-single, jcr:primaryType, not-yet-existing]");
        expectProp("/test/incorrect-should-be-single", PropertyType.STRING, "new");
        expectProp("/test/incorrect-should-be-long", PropertyType.LONG, "[42, 31415]");
        expectProp("/test/already-changed-to-single", PropertyType.STRING, "new");
        expectProp("/test/already-changed-to-long", PropertyType.LONG, "[42, 31415]");
        expectProp("/test/not-yet-existing", PropertyType.STRING, "new");
    }

    @Test
    public void expect_all_value_types_to_be_handled_correctly() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      string: hello world\n"
                + "      binary: !!binary |-\n"
                + "        aGVsbG8gd29ybGQ=\n"
                + "      long: 42\n"
                + "      double: 3.1415\n"
                + "      date: 2015-10-21T07:28:00.000+08:00\n"
                + "      boolean: true\n"
                + "      name:\n"
                + "          type: name\n"
                + "          value: nt:unstructured\n"
                + "      path:\n"
                + "          type: path\n"
                + "          value: /path/to/something\n"
                + "      reference:\n"
                + "          type: reference\n"
                + "          value: cafebabe-cafe-babe-cafe-babecafebabe\n"
                + "      weakreference:\n"
                + "          type: weakreference\n"
                + "          value: cafebabe-cafe-babe-cafe-babecafebabe\n"
                + "      uri:\n"
                + "          type: uri\n"
                + "          value: http://onehippo.org\n"
                + "      decimal:\n"
                + "          type: decimal\n"
                + "          value: '31415926535897932384626433832795028841971'\n"
                + "";

        applyDefinitions(definition); // ignore all events

        expectProp("/test/string", PropertyType.STRING, "hello world");
        expectProp("/test/binary", PropertyType.BINARY, "hello world");
        expectProp("/test/long", PropertyType.LONG, "42");
        expectProp("/test/double", PropertyType.DOUBLE, "3.1415");
        expectProp("/test/date", PropertyType.DATE, "2015-10-21T07:28:00.000+08:00");
        expectProp("/test/boolean", PropertyType.BOOLEAN, "true");
        expectProp("/test/name", PropertyType.NAME, "nt:unstructured");
        expectProp("/test/path", PropertyType.PATH, "/path/to/something");
        expectProp("/test/reference", PropertyType.REFERENCE, "cafebabe-cafe-babe-cafe-babecafebabe");
        expectProp("/test/weakreference", PropertyType.WEAKREFERENCE, "cafebabe-cafe-babe-cafe-babecafebabe");
        expectProp("/test/uri", PropertyType.URI, "http://onehippo.org");
        expectProp("/test/decimal", PropertyType.DECIMAL, "31415926535897932384626433832795028841971");

        // when applying the same definition again, expect no events
        final ExpectedEvents expectedEvents = new ExpectedEvents();
        applyDefinitions(definition, expectedEvents);
    }

    @Test
    public void expect_resources_are_loaded() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      string:\n"
                + "        type: string\n"
                + "        resource: folder/string.txt\n"
                + "      binary:\n"
                + "        type: binary\n"
                + "        resource: folder/binary.bin\n"
                + "";

        ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/string")
                .expectPropertyAdded("/test/binary");

        applyDefinitions(definition, expectedEvents);

        expectProp("/test/string", PropertyType.STRING,
                "test-configuration/test-project/test-module-0/string/folder/string.txt");
        expectProp("/test/binary", PropertyType.BINARY,
                "test-configuration/test-project/test-module-0/string/folder/binary.bin");

        // when applying the same definition again, expect no events
        expectedEvents = new ExpectedEvents();
        applyDefinitions(definition, expectedEvents);
    }

    @Test
    public void expect_value_add_on_resource_to_work() throws Exception {
        final String definition1
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      string:\n"
                + "        type: string\n"
                + "        resource: [folder/string1.txt]\n"
                + "";
        final String definition2
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      string:\n"
                + "        operation: add\n"
                + "        resource: [folder/string1.txt]\n"
                + "";

        ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/string");

        applyDefinitions(new String[]{definition1,definition2}, expectedEvents);

        expectProp("/test/string", PropertyType.STRING,
                "[test-configuration/test-project/test-module-0/string/folder/string1.txt, " +
                "test-configuration/test-project/test-module-1/string/folder/string1.txt]");

        // when applying the same definition again, expect no events
        expectedEvents = new ExpectedEvents();
        applyDefinitions(new String[]{definition1,definition2}, expectedEvents);
    }

    @Test
    public void expect_path_references_to_be_resolved() throws Exception {
        final String definition
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      absolute:\n"
                + "        type: reference\n"
                + "        path: /test/foo/bar\n"
                + "      relative:\n"
                + "        type: reference\n"
                + "        path: foo/bar\n"
                + "      /foo:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        /bar:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          jcr:mixinTypes: ['mix:referenceable']\n"
                + "";

        applyDefinitions(definition);

        final Node bar = testNode.getNode("foo/bar");
        expectProp("/test/absolute", PropertyType.REFERENCE, bar.getIdentifier());
        expectProp("/test/relative", PropertyType.REFERENCE, bar.getIdentifier());

        // when applying the same definition again, expect no events
        final ExpectedEvents expectedEvents = new ExpectedEvents();
        applyDefinitions(definition, expectedEvents);
    }

    @Test
    public void expect_unchanged_existing_nodes_to_be_untouched() throws Exception {
        addNode("/test", "a", "nt:unstructured");
        addNode("/test/a", "z", "nt:unstructured");
        addNode("/test", "b", "nt:unstructured");
        setProperty("/test/a", "property", PropertyType.STRING, "a");
        setProperty("/test/a/z", "property", PropertyType.STRING, "z");
        setProperty("/test/b", "property", PropertyType.STRING, "b");

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents(); // aka, expect to see no events

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[a, b]", "[jcr:primaryType]");
        expectNode("/test/a", "[z]", "[jcr:primaryType, property]");
        expectNode("/test/a/z", "[]", "[jcr:primaryType, property]");
        expectNode("/test/b", "[]", "[jcr:primaryType, property]");
        expectProp("/test/a/property", PropertyType.STRING, "a");
        expectProp("/test/a/z/property", PropertyType.STRING, "z");
        expectProp("/test/b/property", PropertyType.STRING, "b");
    }

    @Test
    public void expect_nodes_to_be_merged() throws Exception {
        addNode("/test", "a", "nt:unstructured");
        addNode("/test/a", "z", "nt:unstructured");
        addNode("/test", "b", "nt:unstructured");
        setProperty("/test/a", "property", PropertyType.STRING, "a");
        setProperty("/test/a/z", "property", PropertyType.STRING, "z");
        setProperty("/test/b", "property", PropertyType.STRING, "b");

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /first:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: first\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "        /z:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: z\n"
                + "        /recurse:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "          property: recurse\n"
                + "      /middle:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: middle\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: b\n"
                + "      /last:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: last\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeAdded("/test/first", JCR_PRIMARYTYPE)
                .expectNodeAdded("/test/middle", JCR_PRIMARYTYPE)
                .expectNodeAdded("/test/last", JCR_PRIMARYTYPE)
                .expectNodeAdded("/test/a/recurse", JCR_PRIMARYTYPE)
                .expectPropertyAdded("/test/first/property")
                .expectPropertyAdded("/test/middle/property")
                .expectPropertyAdded("/test/last/property")
                .expectPropertyAdded("/test/a/recurse/property");

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[first, a, middle, b, last]", "[jcr:primaryType]");
        expectNode("/test/first", "[]", "[jcr:primaryType, property]");
        expectProp("/test/first/property", PropertyType.STRING, "first");
        expectNode("/test/middle", "[]", "[jcr:primaryType, property]");
        expectProp("/test/middle/property", PropertyType.STRING, "middle");
        expectNode("/test/last", "[]", "[jcr:primaryType, property]");
        expectProp("/test/last/property", PropertyType.STRING, "last");
        expectNode("/test/a/recurse", "[]", "[jcr:primaryType, property]");
        expectProp("/test/a/recurse/property", PropertyType.STRING, "recurse");
    }

    @Test
    public void expect_deleted_nodes_to_be_deleted() throws Exception {
        addNode("/test", "a", "nt:unstructured");
        addNode("/test/a", "z", "nt:unstructured");
        addNode("/test", "b", "nt:unstructured");
        setProperty("/test/a", "property", PropertyType.STRING, "a");
        setProperty("/test/a/z", "property", PropertyType.STRING, "z");
        setProperty("/test/b", "property", PropertyType.STRING, "b");

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property: a\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectNodeRemoved("/test/a/z")
                .expectNodeRemoved("/test/b");

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[a]", "[jcr:primaryType]");
    }

    @Test
    public void expect_nodetype_overrides_to_be_applied_if_necessary() throws Exception {
        addNode("/test", "keep-as-is", "nt:unstructured", new String[]{"mix:language"});
        addNode("/test", "remove-mixin", "nt:unstructured", new String[]{"mix:language"});
        addNode("/test", "change-type-and-mixin", "nt:unstructured", new String[]{"mix:language"});

        // TODO: as part of HCM-24, expand this test
        // TODO: replace hippo types with custom test types
        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /keep-as-is:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        jcr:mixinTypes: ['mix:language']\n"
                + "      /remove-mixin:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /change-type-and-mixin:\n"
                + "        jcr:primaryType: hippo:document\n"
                + "        jcr:mixinTypes: ['hippostd:relaxed']\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyRemoved("/test/remove-mixin/jcr:mixinTypes")
                .expectPropertyChanged("/test/change-type-and-mixin/jcr:primaryType")
                .expectPropertyChanged("/test/change-type-and-mixin/jcr:mixinTypes")
                .expectPropertyAdded("/test/change-type-and-mixin/hippo:paths");

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[keep-as-is, remove-mixin, change-type-and-mixin]", "[jcr:primaryType]");
        expectNode("/test/keep-as-is", "[]", "[jcr:mixinTypes, jcr:primaryType]");
        expectProp("/test/keep-as-is/jcr:primaryType", PropertyType.NAME, "nt:unstructured");
        expectProp("/test/keep-as-is/jcr:mixinTypes", PropertyType.NAME, "[mix:language]");
        expectNode("/test/remove-mixin", "[]", "[jcr:primaryType]");
        expectProp("/test/remove-mixin/jcr:primaryType", PropertyType.NAME, "nt:unstructured");
        expectNode("/test/change-type-and-mixin", "[]", "[hippo:paths, jcr:mixinTypes, jcr:primaryType]");
        expectProp("/test/change-type-and-mixin/jcr:primaryType", PropertyType.NAME, "hippo:document");
        expectProp("/test/change-type-and-mixin/jcr:mixinTypes", PropertyType.NAME, "[hippostd:relaxed]");
    }

    @Test
    public void expect_reorders_to_be_applied() throws Exception {
        addNode("/test", "a", "nt:unstructured");
        addNode("/test", "b", "nt:unstructured");
        addNode("/test", "c", "nt:unstructured");
        final String idA = session.getNode("/test/a").getIdentifier();
        final String idB = session.getNode("/test/b").getIdentifier();
        final String idC = session.getNode("/test/c").getIdentifier();

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /c:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /b:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "      /a:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "";

        applyDefinitions(source);

        // JackRabbit's event system does not represent node reorders well. Checking that the node IDs did not change
        // to ensure the nodes did not get deleted and recreated.

        expectNode("/test", "[c, b, a]", "[jcr:primaryType]");
        assertEquals(idA, session.getNode("/test/a").getIdentifier());
        assertEquals(idB, session.getNode("/test/b").getIdentifier());
        assertEquals(idC, session.getNode("/test/c").getIdentifier());
    }

    @Test
    public void expect_node_order_ignored_in_non_orderable_node() throws Exception {
        addNode("/test", "non-orderable", "hippostd:directory", new String[]{"hippostd:relaxed"});
        addNode("/test/non-orderable", "a", "nt:unstructured");
        addNode("/test/non-orderable", "b", "nt:unstructured");
        addNode("/test/non-orderable", "c", "nt:unstructured");

        final String source
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /non-orderable:\n"
                + "        jcr:primaryType: hippostd:directory\n"
                + "        jcr:mixinTypes: ['hippostd:relaxed']\n"
                + "        /c:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "        /b:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "        /a:\n"
                + "          jcr:primaryType: nt:unstructured\n"
                + "";

        // TODO: replace hippo types with custom test types to get rid of changed hippo:paths property
        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/non-orderable/hippo:paths");

        applyDefinitions(source, expectedEvents);

        expectNode("/test/non-orderable", "[a, b, c]", "[hippo:paths, jcr:mixinTypes, jcr:primaryType]");
    }

    private void setProperty(final String nodePath, final String name, final int valueType, final String value) throws RepositoryException {
        session.getNode(nodePath).setProperty(name, value, valueType);
        session.save();
    }

    private void setProperty(final String nodePath, final String name, final int valueType, final String[] values) throws RepositoryException {
        session.getNode(nodePath).setProperty(name, values, valueType);
        session.save();
    }

    private void addNode(final String parent, final String name, final String primaryType) throws RepositoryException {
        session.getNode(parent).addNode(name, primaryType);
        session.save();
    }

    private void addNode(final String parent, final String name, final String primaryType, final String[] mixinTypes) throws RepositoryException {
        final Node node = session.getNode(parent).addNode(name, primaryType);
        for (String mixinType : mixinTypes) {
            node.addMixin(mixinType);
        }
        session.save();
    }

    private void applyDefinitions(final String source) throws Exception {
        applyDefinitions(new String[]{source}, null);
    }

    private void applyDefinitions(final String source, final ExpectedEvents expectedEvents) throws Exception {
        applyDefinitions(new String[]{source}, expectedEvents);
    }

    private void applyDefinitions(final String[] sources, final ExpectedEvents expectedEvents) throws Exception {
        final EventCollector eventCollector = new EventCollector(session, testNode);
        eventCollector.start();

        final Map<Module, ResourceInputProvider> resourceInputProviders = new HashMap<>();
        final MergedModelBuilder mergedModelBuilder = new MergedModelBuilder();
        for (int i = 0; i < sources.length; i++) {
            final List<Definition> definitions = parseNoSort(sources[i], "test-module-" + i);
            assertTrue(definitions.size() > 0);
            final Module module = definitions.get(0).getSource().getModule();
            final ConfigurationImpl configuration = (ConfigurationImpl) module.getProject().getConfiguration();
            mergedModelBuilder.push(configuration);
            resourceInputProviders.put(module, ModelTestUtils.getTestResourceInputProvider());
        }
        final MergedModel mergedModel = mergedModelBuilder.build();

        final ConfigurationPersistenceService helper = new ConfigurationPersistenceService(session, resourceInputProviders);
        helper.apply(mergedModel);

        session.save();

        final List<EventPojo> events = eventCollector.stop();

        if (expectedEvents != null) {
            expectedEvents.check(events);
        }
    }

    private void expectNode(final String nodePath, final String childNodes, final String childProperties) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        assertEquals(childNodes, createChildNodesString(node));
        assertEquals(childProperties, createChildPropertiesString(node));
    }

    private void expectProp(final String path, final int expectedValueType, final String expectedValue) throws RepositoryException {
        final Property property = session.getProperty(path);
        assertEquals(expectedValueType, property.getType());

        final String actualValue;
        if (property.isMultiple()) {
            final List<String> values = new ArrayList();
            for (Value value : property.getValues()) {
                values.add(value.getString());
            }
            actualValue = values.toString();
        } else {
            actualValue = property.getValue().getString();
        }

        assertEquals(expectedValue, actualValue);
    }

    String createChildNodesString(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Node child : new NodeIterable(node.getNodes())) {
            names.add(child.getName());
        }
        if (!node.getPrimaryNodeType().hasOrderableChildNodes()) {
            Collections.sort(names);
        }
        return names.toString();
    }

    String createChildPropertiesString(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            names.add(property.getName());
        }
        Collections.sort(names);
        return names.toString();
    }

}
