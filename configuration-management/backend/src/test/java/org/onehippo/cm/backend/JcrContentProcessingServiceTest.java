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
package org.onehippo.cm.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.action.ActionType;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModelTestUtils;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.builder.ConfigurationModelBuilder;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.impl.model.ModelTestUtils.parseNoSort;

public class JcrContentProcessingServiceTest extends RepositoryTestCase {

    private final String source =
            "/test:\n" +
                    "   jcr:primaryType: nt:unstructured\n" +
                    "   boolean: true\n" +
                    "   date: 2015-10-21T07:28:00+08:00\n" +
                    "   double: 3.1415\n" +
                    "   longAsInt: 42\n" +
                    "   longAsLong: 4200000000\n" +
                    "   string: hello world\n" +
                    "   /testChildNode:\n" +
                    "       jcr:primaryType: nt:unstructured\n" +
                    "       boolean: true\n" +
                    "       date: 2015-10-21T07:28:00+08:00";


    private final String source1
            = "/test/node1:\n" +
            "  jcr:primaryType: nt:unstructured\n" +
            "  boolean: true\n" +
            "  date: 2015-10-21T07:28:00+08:00\n" +
            "  double: 3.1415\n" +
            "  decimal-multi-value:\n" +
            "       type: decimal\n" +
            "       value: ['42', '31415926535897932384626433832795028841971', '4.2E+314159265']\n"+
            "  longAsInt: 42\n" +
            "  longAsLong: 4200000000\n" +
            "  string: hello world";

    private final String source1_reload
            = "/test/node1:\n" +
            "  jcr:primaryType: nt:unstructured\n" +
            "  boolean: false\n" +
            "  string: hello new world";

    private final String source2
            = "/test/node2:\n" +
            "  jcr:primaryType: nt:unstructured\n" +
            "  boolean: true\n" +
            "  date: 2015-10-21T07:28:00+08:00\n" +
            "  double: 3.1415\n" +
            "  longAsLong: 4200000000\n" +
            "  string: hello world";

    private final String source3
            = "/test/node3:\n" +
            "  jcr:primaryType: nt:unstructured\n" +
            "  boolean: true\n" +
            "  date: 2015-10-21T07:28:00+08:00\n" +
            "  double: 3.1415\n" +
            "  string: hello world";

    private final String typelessNode
            = "/test/typelessNode:\n" +
            "  boolean: true\n" +
            "  date: 2015-10-21T07:28:00+08:00\n" +
            "  double: 3.1415\n" +
            "  string: hello world";

    private final String orphanedNode
            = "/test/non_existing_path/node1:\n" +
            "  jcr:primaryType: nt:unstructured\n" +
            "  boolean: true\n" +
            "  date: 2015-10-21T07:28:00+08:00\n" +
            "  double: 3.1415\n" +
            "  longAsInt: 42\n" +
            "  longAsLong: 4200000000\n" +
            "  string: hello world";
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        applyAndSaveDefinitions(new String[]{source});
    }

    @Test
    public void append_content() throws Exception {

        applyAndSaveDefinitions(new String[]{source1});
        applyAndSaveDefinitions(new String[]{source2, source3});

        Node node1 = session.getNode("/test/node1");
        assertEquals(8, node1.getProperties().getSize());
        Node node2 = session.getNode("/test/node2");
        assertEquals(6, node2.getProperties().getSize());
        Node node3 = session.getNode("/test/node3");
        assertEquals(5, node3.getProperties().getSize());
        Node testChildNode = session.getNode("/test/testChildNode");
        assertEquals(3, testChildNode.getProperties().getSize());
    }

    @Test(expected = ItemExistsException.class)
    public void append_existing_content() throws Exception {
        applyAndSaveDefinitions(new String[]{source1});
        applyAndSaveDefinitions(new String[]{source1_reload}, ActionType.APPEND);
    }

    @Test(expected = PathNotFoundException.class)
    public void process_orphaned_content() throws Exception {
        applyAndSaveDefinitions(new String[]{orphanedNode});
    }

    @Test(expected = RuntimeException.class)
    public void process_typeless_content() throws Exception {
        applyAndSaveDefinitions(new String[]{typelessNode});
    }

    @Test
    public void import_content_append() throws Exception {
        final ConfigurationModel configModel1 = createConfigurationModel(new String[]{source1});
        saveContent(configModel1);
        session.save();

        final ConfigurationModel configModel2 = createConfigurationModel(new String[]{source2});

        ValueConverter valueConverter = new ValueConverter(session);
        final ContentProcessingService processingService = new JcrContentProcessingService(session, valueConverter);

        Node parentNode = session.getNode("/test/node1");
        processingService.importNode(configModel2.getContentDefinitions().get(0).getNode(), parentNode, ActionType.APPEND);
        session.save();

        Node importedNode = session.getNode("/test/node1/node2");
        assertEquals(6, importedNode.getProperties().getSize());

    }

    @Test
    public void reload_content() throws Exception {

        applyAndSaveDefinitions(new String[]{source1});

        final Node originalNode = session.getNode("/test/node1");

        assertEquals(8, originalNode.getProperties().getSize());

        final boolean boolValue = getPropertyByName("boolean", originalNode).getBoolean();
        final String stringValue = getPropertyByName("string", originalNode).getString();

        assertEquals(true, boolValue);
        assertEquals("hello world", stringValue);

        applyAndSaveDefinitions(new String[]{source1_reload}, ActionType.RELOAD);

        final Node reloadedNode = session.getNode("/test/node1");

        assertEquals(3, reloadedNode.getProperties().getSize());

        final boolean newBoolValue = getPropertyByName("boolean", reloadedNode).getBoolean();
        final String newStringValue = getPropertyByName("string", reloadedNode).getString();

        assertEquals(false, newBoolValue);
        assertEquals("hello new world", newStringValue);
    }

    @Test
    public void delete_content() throws Exception {
        append_content();

        final ValueConverter valueConverter = new ValueConverter(session);
        final ContentProcessingService processingService = new JcrContentProcessingService(session, valueConverter);

        final DefinitionNodeImpl deleteNode = new DefinitionNodeImpl("/test/node3", "node3", null);
        processingService.apply(deleteNode, ActionType.DELETE);

        final DefinitionNodeImpl nonExistingNode = new DefinitionNodeImpl("/not_existing_path/node", "node", null);
        processingService.apply(nonExistingNode, ActionType.DELETE);

    }

        private Property getPropertyByName(final String name, final Node node) throws RepositoryException {
        for(PropertyIterator iterator = node.getProperties(); iterator.hasNext();) {
            Property property = iterator.nextProperty();
            if (property.getName().equals(name)) {
                return property;
            }
        }
        throw new RuntimeException(String.format("Property '%s' not found in node '%s'", name, node.getName()));
    }

    private void saveContent(final ConfigurationModel configModel) throws RepositoryException {
        saveContent(configModel, ActionType.APPEND);
    }

    private void saveContent(final ConfigurationModel configModel, final ActionType actionType) throws RepositoryException {
        final ValueConverter valueConverter = new ValueConverter(session);
        final ContentProcessingService processingService = new JcrContentProcessingService(session, valueConverter);
        for (final ContentDefinition contentDefinition : configModel.getContentDefinitions()) {
            processingService.apply(contentDefinition.getNode(), actionType);
        }
    }

    private void applyAndSaveDefinitions(final String[] sources) throws Exception {
        applyAndSaveDefinitions(sources, ActionType.APPEND);
    }

    private void applyAndSaveDefinitions(final String[] sources, final ActionType actionType) throws Exception {

        final ConfigurationModel configurationModel = createConfigurationModel(sources);
        saveContent(configurationModel, actionType);
        session.save();
    }

    private ConfigurationModel createConfigurationModel(final String[] sources) throws Exception {
        final Map<Module, ResourceInputProvider> resourceInputProviders = new HashMap<>();
        final ConfigurationModelBuilder configurationModelBuilder = new ConfigurationModelBuilder();
        for (int i = 0; i < sources.length; i++) {
            final List<Definition> definitions = parseNoSort(sources[i], "test-module-" + i, ContentDefinition.class);
            assertTrue(definitions.size() == 1);
            final ModuleImpl module = (ModuleImpl) definitions.get(0).getSource().getModule();
            module.setConfigResourceInputProvider(ModelTestUtils.getTestResourceInputProvider());
            module.setContentResourceInputProvider(ModelTestUtils.getTestResourceInputProvider());
            final GroupImpl configuration = (GroupImpl) module.getProject().getGroup();
            configurationModelBuilder.push(configuration);
        }
        return configurationModelBuilder.build();
    }



}