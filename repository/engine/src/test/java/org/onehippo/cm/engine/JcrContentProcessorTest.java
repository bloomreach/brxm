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
package org.onehippo.cm.engine;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.ExportModuleContext;
import org.onehippo.cm.model.FileConfigurationWriter;
import org.onehippo.cm.model.ModuleContext;
import org.onehippo.cm.model.impl.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import com.google.common.io.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cm.engine.ConfigurationServiceTestUtils.createChildNodesString;
import static org.onehippo.cm.model.impl.ModelTestUtils.parseNoSort;

public class JcrContentProcessorTest extends RepositoryTestCase {

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
            "  jcr:mixinTypes:\n" +
            "       value: ['mix:shareable', 'mix:referenceable']\n"+
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
        PropertyIterator properties = node1.getProperties();
        assertEquals(10, properties.getSize());
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
        try (Log4jInterceptor ignored = Log4jInterceptor.onWarn().deny(JcrContentProcessor.class).build()) {
            JcrContentProcessorTest.this.applyAndSaveDefinitions(new String[]{source1_reload}, ActionType.APPEND);
        };
    }

    @Test(expected = PathNotFoundException.class)
    public void process_orphaned_content() throws Exception {
        try (Log4jInterceptor ignored = Log4jInterceptor.onWarn().deny(JcrContentProcessor.class).build()) {
            applyAndSaveDefinitions(new String[]{orphanedNode});
        };
    }

    @Test(expected = RuntimeException.class)
    public void process_typeless_content() throws Exception {
        try (Log4jInterceptor ignored = Log4jInterceptor.onWarn().deny(JcrContentProcessor.class).build()) {
            applyAndSaveDefinitions(new String[]{typelessNode});
        };
    }

    @Test
    public void import_content_append() throws Exception {
        final ConfigurationModel configModel1 = createConfigurationModel(new String[]{source1});
        saveContent(configModel1);
        session.save();

        final ConfigurationModelImpl configModel2 = createConfigurationModel(new String[]{source2});

        final JcrContentProcessor processingService = new JcrContentProcessor();

        Node parentNode = session.getNode("/test/node1");
        processingService.importNode(configModel2.getContentDefinitions().get(0).getNode(), parentNode, ActionType.APPEND);
        session.save();

        Node importedNode = session.getNode("/test/node1/node2");
        assertEquals(6, importedNode.getProperties().getSize());

    }

    @Test
    public void export_content() throws Exception {
        import_content_append();
        Node importedNode = session.getNode("/test");

        InputStream contentStream = IOUtils.toInputStream("SomeContentGoesHere", Charset.defaultCharset());
        final ValueFactory factory = session.getValueFactory();
        Binary binary = factory.createBinary(contentStream);
        importedNode.setProperty("binary", binary);
        session.save();

        final ExportContentProcessor exportContentProcessor = new ExportContentProcessor();
        ModuleImpl module = exportContentProcessor.exportNode(importedNode);

        File tempDir = Files.createTempDir();
        Path moduleRootPath = Paths.get(tempDir.getPath());

        ModuleContext moduleContext = new ExportModuleContext(module, moduleRootPath);
        new FileConfigurationWriter(new JcrResourceInputProvider(session))
                .writeModule(module, org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING, moduleContext);
        tempDir.delete();
    }

    @Test
    public void reload_content() throws Exception {

        applyAndSaveDefinitions(new String[]{source1});

        final Node originalNode = session.getNode("/test/node1");

        assertEquals(10, originalNode.getProperties().getSize());

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

        final JcrContentProcessor processingService = new JcrContentProcessor();

        final DefinitionNodeImpl deleteNode = new DefinitionNodeImpl("/test/node3", "node3", null);
        processingService.apply(deleteNode, ActionType.DELETE, session);

        final DefinitionNodeImpl nonExistingNode = new DefinitionNodeImpl("/not_existing_path/node", "node", null);
        processingService.apply(nonExistingNode, ActionType.DELETE, session);

    }

    @Test
    public void apply_sns() throws Exception {
        final String yaml =
                "/test/sns:\n" +
                "  jcr:primaryType: nt:unstructured\n" +
                "  /a:\n" +
                "    jcr:primaryType: nt:unstructured\n" +
                "  /sns[1]:\n" +
                "    jcr:primaryType: nt:unstructured\n" +
                "    property: value1\n" +
                "  /b:\n" +
                "    jcr:primaryType: nt:unstructured\n" +
                "  /sns[2]:\n" +
                "    jcr:primaryType: nt:unstructured\n" +
                "    property: value2\n" +
                "  /c:\n" +
                "    jcr:primaryType: nt:unstructured\n";

        applyAndSaveDefinitions(new String[]{yaml});

        Node snsNode = session.getNode("/test/sns");
        assertEquals("[a, sns, b, sns, c]", createChildNodesString(snsNode));
        assertEquals("value1", session.getNode("/test/sns/sns[1]").getProperty("property").getValue().getString());
        assertEquals("value2", session.getNode("/test/sns/sns[2]").getProperty("property").getValue().getString());
    }

    @Test
    public void expect_error_when_using_root_with_sns_index() throws Exception {
        final String yaml =
                "/test/sns[2]:\n" +
                "  jcr:primaryType: nt:unstructured\n";

        try {
            applyAndSaveDefinitions(new String[]{yaml});
            fail("Should have thrown exception");
        } catch (ParserException e) {
            assertEquals("Path must not contain name indices", e.getMessage());
        }
    }

    @Test
    public void expect_error_when_multiple_roots() throws Exception {
        final String yaml =
                "/one:\n" +
                "  jcr:primaryType: nt:unstructured\n" +
                "/two:\n" +
                "  jcr:primaryType: nt:unstructured\n";

        try {
            applyAndSaveDefinitions(new String[]{yaml});
            fail("Should have thrown exception");
        } catch (ParserException e) {
            assertEquals("Content definitions can only contain single root node", e.getMessage());
        }
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
        final JcrContentProcessor processingService = new JcrContentProcessor();
        for (final ContentDefinition contentDefinition : configModel.getContentDefinitions()) {
            processingService.apply(contentDefinition.getNode(), actionType, session);
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

    private ConfigurationModelImpl createConfigurationModel(final String[] sources) throws Exception {
        final ConfigurationModelImpl model = new ConfigurationModelImpl();
        for (int i = 0; i < sources.length; i++) {
            final List<AbstractDefinitionImpl> definitions = parseNoSort(sources[i], "test-module-" + i, false);
            assertTrue(definitions.size() == 1);
            final ModuleImpl module = definitions.get(0).getSource().getModule();
            module.setConfigResourceInputProvider(ModelTestUtils.getTestResourceInputProvider());
            module.setContentResourceInputProvider(ModelTestUtils.getTestResourceInputProvider());
            final GroupImpl group = module.getProject().getGroup();
            model.addGroup(group);
        }
        return model.build();
    }



}