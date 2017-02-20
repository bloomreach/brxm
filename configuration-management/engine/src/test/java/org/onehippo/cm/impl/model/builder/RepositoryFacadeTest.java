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
package org.onehippo.cm.impl.model.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.builder.eventutils.EventCollector;
import org.onehippo.cm.impl.model.builder.eventutils.EventPojo;
import org.onehippo.cm.impl.model.builder.eventutils.ExpectedEvents;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.impl.model.ModelTestUtils.parseNoSort;

public class RepositoryFacadeTest extends RepositoryTestCase {

    /* - add test for resources
     * - add test for all value types
     * - add test & logic for path references
     * - add node
     * - merge node
     * - reorder node
     * - attempt reorder within not-orderable node
     * - delete node
     * - jcrProperty and MixinType tests
     *   - check if it is possible to remove either using a prop-delete ...
     */
    private Node testNode;

    @Before
    public void createTestNode() throws RepositoryException {
        testNode = session.getRootNode().addNode("test");
        session.save();
    }

    @Test
    public void expect_unchanged_existing_properties_to_be_untouched() throws Exception {
        setProperty("/test", "single", PropertyType.STRING, "org");
        setProperty("/test", "multiple", PropertyType.STRING, new String[]{"org1","org2"});

        final String source
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - single: org\n"
                + "    - multiple: [org1, org2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents(); // aka, expect to see no events

        applyDefinitions(source, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProperty("/test/single", PropertyType.STRING, "org");
        expectProperty("/test/multiple", PropertyType.STRING, new String[]{"org1","org2"});
    }

    @Test
    public void expect_new_properties_to_be_created() throws Exception {
        // no initial content

        final String definition
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - single: new\n"
                + "    - multiple: [new1, new2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyAdded("/test/single")
                .expectPropertyAdded("/test/multiple");

        applyDefinitions(definition, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, single]");
        expectProperty("/test/single", PropertyType.STRING, "new");
        expectProperty("/test/multiple", PropertyType.STRING, new String[]{"new1","new2"});
    }

    @Test
    public void expect_updated_properties_to_be_updated() throws Exception {
        setProperty("/test", "single", PropertyType.STRING, "org");
        setProperty("/test", "multiple", PropertyType.STRING, new String[]{"org1","org2"});
        setProperty("/test", "reordered", PropertyType.STRING, new String[]{"new2","new1"});

        final String definition
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - single: new\n"
                + "    - multiple: [new1, new2]\n"
                + "    - reordered: [new1, new2]\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyChanged("/test/single")
                .expectPropertyChanged("/test/multiple")
                .expectPropertyChanged("/test/reordered");

        applyDefinitions(definition, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType, multiple, reordered, single]");
        expectProperty("/test/single", PropertyType.STRING, "new");
        expectProperty("/test/multiple", PropertyType.STRING, new String[]{"new1","new2"});
        expectProperty("/test/reordered", PropertyType.STRING, new String[]{"new1","new2"});
    }

    @Test
    public void expect_deleted_properties_to_be_gone() throws Exception {
        setProperty("/test", "not-in-config", PropertyType.STRING, "value");
        setProperty("/test", "explicitly-deleted", PropertyType.STRING, "value");

        final String definition1
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - explicitly-deleted: value\n"
                + "    - explicitly-deleted-non-existing: value\n"
                + "";
        final String definition2
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - explicitly-deleted:\n"
                + "        operation: delete\n"
                + "    - explicitly-deleted-non-existing:\n"
                + "        operation: delete\n"
                + "";

        final ExpectedEvents expectedEvents = new ExpectedEvents()
                .expectPropertyRemoved("/test/not-in-config")
                .expectPropertyRemoved("/test/explicitly-deleted");

        applyDefinitions(new String[]{definition1,definition2}, expectedEvents);

        expectNode("/test", "[]", "[jcr:primaryType]");
    }

    @Test
    public void expect_overrides_to_be_applied_if_necessary() throws Exception {
        setProperty("/test", "incorrect-should-be-single", PropertyType.STRING, new String[]{"org1","org2"});
        setProperty("/test", "incorrect-should-be-long", PropertyType.STRING, new String[]{"org1","org2"});
        setProperty("/test", "already-changed-to-single", PropertyType.STRING, "new");
        setProperty("/test", "already-changed-to-long", PropertyType.LONG, new String[]{"42","31415"});

        final String definition1
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - incorrect-should-be-single: [org1, org2]\n"
                + "    - incorrect-should-be-long: [org1, org2]\n"
                + "    - already-changed-to-single: [org1, org2]\n"
                + "    - already-changed-to-long: [org1, org2]\n"
                + "    - not-yet-existing: [org1, org2]\n"
                + "";
        final String definition2
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - incorrect-should-be-single:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "    - incorrect-should-be-long:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: [42, 31415]\n"
                + "    - already-changed-to-single:\n"
                + "        operation: override\n"
                + "        type: string\n"
                + "        value: new\n"
                + "    - already-changed-to-long:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: [42, 31415]\n"
                + "    - not-yet-existing:\n"
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
        expectProperty("/test/incorrect-should-be-single", PropertyType.STRING, "new");
        expectProperty("/test/incorrect-should-be-long", PropertyType.LONG, new String[]{"42","31415"});
        expectProperty("/test/already-changed-to-single", PropertyType.STRING, "new");
        expectProperty("/test/already-changed-to-long", PropertyType.LONG, new String[]{"42","31415"});
        expectProperty("/test/not-yet-existing", PropertyType.STRING, "new");
    }

    @Test
    public void expect_all_value_types_to_be_handled_correctly() throws Exception {
        final String definition
                = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - jcr:primaryType: nt:unstructured\n"
                + "    - string: hello world\n"
                + "    - binary: !!binary |-\n"
                + "        aGVsbG8gd29ybGQ=\n"
                + "    - long: 42\n"
                + "    - double: 3.1415\n"
                + "    - date: 2015-10-21T07:28:00+8:00\n"
                + "    - boolean: true\n"
                + "    - name:\n"
                + "          type: name\n"
                + "          value: nt:unstructured\n"
                + "    - path:\n"
                + "          type: path\n"
                + "          value: /path/to/something\n"
                + "    - reference:\n"
                + "          type: reference\n"
                + "          value: cafebabe-cafe-babe-cafe-babecafebabe\n"
                + "    - weakreference:\n"
                + "          type: weakreference\n"
                + "          value: cafebabe-cafe-babe-cafe-babecafebabe\n"
                + "    - uri:\n"
                + "          type: uri\n"
                + "          value: http://onehippo.org\n"
                + "    - decimal:\n"
                + "          type: decimal\n"
                + "          value: '31415926535897932384626433832795028841971'\n"
                + "";

        applyDefinitions(definition);

        expectProperty("/test/string", PropertyType.STRING, "hello world");
        expectProperty("/test/binary", PropertyType.BINARY, "hello world");
        expectProperty("/test/long", PropertyType.LONG, "42");
        expectProperty("/test/double", PropertyType.DOUBLE, "3.1415");
        expectProperty("/test/date", PropertyType.DATE, "2015-10-21T07:28:00.000+08:00");
        expectProperty("/test/boolean", PropertyType.BOOLEAN, "true");
        expectProperty("/test/name", PropertyType.NAME, "nt:unstructured");
        expectProperty("/test/path", PropertyType.PATH, "/path/to/something");
        expectProperty("/test/reference", PropertyType.REFERENCE, "cafebabe-cafe-babe-cafe-babecafebabe");
        expectProperty("/test/weakreference", PropertyType.WEAKREFERENCE, "cafebabe-cafe-babe-cafe-babecafebabe");
        expectProperty("/test/uri", PropertyType.URI, "http://onehippo.org");
        expectProperty("/test/decimal", PropertyType.DECIMAL, "31415926535897932384626433832795028841971");

        // when applying the same definition again, expect no events
        final ExpectedEvents expectedEvents = new ExpectedEvents();
        applyDefinitions(definition, expectedEvents);
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

        final RepositoryFacade repositoryFacade = new RepositoryFacade(session, testNode);
        final MergedModelBuilder mergedModelBuilder = new MergedModelBuilder();
        for (int i = 0; i < sources.length; i++) {
            final List<Definition> definitions = parseNoSort(sources[i], "test-module-" + i);
            assertTrue(definitions.size() > 0);
            final ConfigurationImpl configuration =
                    (ConfigurationImpl) definitions.get(0).getSource().getModule().getProject().getConfiguration();
            mergedModelBuilder.push(configuration);
        }
        final MergedModel mergedModel = mergedModelBuilder.build();

        repositoryFacade.push(mergedModel);

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

    private void setProperty(final String nodePath, final String name, final int valueType, final String value) throws RepositoryException {
        session.getNode(nodePath).setProperty(name, value, valueType);
        session.save();
    }

    private void setProperty(final String nodePath, final String name, final int valueType, final String[] values) throws RepositoryException {
        session.getNode(nodePath).setProperty(name, values, valueType);
        session.save();
    }

    private void expectProperty(final String path, final int expectedValueType, final String expectedValue) throws RepositoryException {
        final Property property = session.getProperty(path);
        assertEquals(expectedValueType, property.getType());
        assertFalse(property.isMultiple());
        assertEquals(expectedValue, property.getValue().getString());
    }

    private void expectProperty(final String path, final int expectedValueType, final String[] expectedValues) throws RepositoryException {
        final Property property = session.getProperty(path);
        assertEquals(expectedValueType, property.getType());
        assertTrue(property.isMultiple());
        final Value[] values = property.getValues();
        assertEquals(values.length, expectedValues.length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(expectedValues[i], values[i].getString());
        }
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
