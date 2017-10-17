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

package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.junit.Before;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.jcr.event.EventCollector;
import org.onehippo.testutils.jcr.event.EventPojo;
import org.onehippo.testutils.jcr.event.ExpectedEvents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.engine.ConfigurationServiceTestUtils.createChildNodesString;
import static org.onehippo.cm.engine.ConfigurationServiceTestUtils.createChildPropertiesString;
import static org.onehippo.cm.model.impl.ModelTestUtils.parseNoSort;

/**
 * {@link ConfigurationConfigService} is a rather complex class, requiring a lot of unit tests.
 * In order to divide these unit tests over a number of unit test classes, this class provides
 * support and tooling for use in all of these unit test classes.
 */
public abstract class BaseConfigurationConfigServiceTest extends RepositoryTestCase {
    protected Node testNode;

    protected static final String[] DEFAULT_BASELINE_SOURCES = {
            "definitions:\n"
                    + "  config:\n"
                    + "    /test:\n"
                    + "      jcr:primaryType: nt:unstructured"
    };

    @Before
    public void createTestNode() throws RepositoryException {
        testNode = session.getRootNode().addNode("test");
        session.save();
    }

    protected void setProperty(final String nodePath, final String name, final int valueType, final String value)
            throws RepositoryException {
        session.getNode(nodePath).setProperty(name, value, valueType);
        session.save();
    }

    protected void setProperty(final String nodePath, final String name, final int valueType, final String[] values)
            throws RepositoryException {
        session.getNode(nodePath).setProperty(name, values, valueType);
        session.save();
    }

    protected void addNode(final String parent, final String name, final String primaryType)
            throws RepositoryException {
        session.getNode(parent).addNode(name, primaryType);
        session.save();
    }

    protected void addNode(final String parent, final String name, final String primaryType, final String[] mixinTypes)
            throws RepositoryException {
        final Node node = session.getNode(parent).addNode(name, primaryType);
        for (String mixinType : mixinTypes) {
            node.addMixin(mixinType);
        }
        session.save();
    }


    protected ConfigurationModelImpl applyDefinitions(final String source) throws Exception {
        return applyDefinitions(new String[]{source}, makeMergedModel(DEFAULT_BASELINE_SOURCES), false);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final boolean forceApply) throws Exception {
        return applyDefinitions(new String[]{source}, makeMergedModel(DEFAULT_BASELINE_SOURCES), forceApply);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final ConfigurationModel baseline) throws Exception {
        return applyDefinitions(new String[]{source}, baseline, false);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final ConfigurationModel baseline, final boolean forceApply) throws Exception {
        return applyDefinitions(new String[]{source}, baseline, forceApply);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(new String[]{source}, makeMergedModel(DEFAULT_BASELINE_SOURCES), false, expectedEvents);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final boolean forceApply, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(new String[]{source}, makeMergedModel(DEFAULT_BASELINE_SOURCES), forceApply, expectedEvents);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final ConfigurationModel baseline, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(new String[]{source}, baseline, false, expectedEvents);
    }

    protected ConfigurationModelImpl applyDefinitions(final String source, final ConfigurationModel baseline, final boolean forceApply, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(new String[]{source}, baseline, forceApply, expectedEvents);
    }

    protected ConfigurationModelImpl applyDefinitions(final String[] sources, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(sources, makeMergedModel(DEFAULT_BASELINE_SOURCES), false, expectedEvents);
    }

    protected ConfigurationModelImpl applyDefinitions(final String[] sources,
                                                  final ConfigurationModel baseline,
                                                  final boolean forceApply,
                                                  final ExpectedEvents expectedEvents) throws Exception {
        final ConfigurationModelImpl configurationModel;

        if (expectedEvents != null) {
            final EventCollector eventCollector = new EventCollector(session, testNode);
            eventCollector.start();

            configurationModel = applyDefinitions(sources, baseline, forceApply);

            final List<EventPojo> events = eventCollector.stop();
            expectedEvents.check(events);
        } else {
            configurationModel = applyDefinitions(sources, baseline, forceApply);
        }

        return configurationModel;
    }

    protected ConfigurationModelImpl applyDefinitions(final String[] sources,
                                                  final ConfigurationModel baseline,
                                                  final boolean forceApply) throws Exception {
        final ConfigurationModelImpl configurationModel = makeMergedModel(sources);

        final ConfigurationConfigService helper = new ConfigurationConfigService();
        helper.computeAndWriteDelta(baseline, configurationModel, session, forceApply);
        session.save();

        return configurationModel;
    }

    private final class TestResourceInputProvider implements ResourceInputProvider {
        public boolean hasResource(final Source source, final String resourcePath) {
            return this.getClass().getResource(resourcePath) != null;
        }

        public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
            return this.getClass().getResourceAsStream("/configuration_config_service_test/" + resourcePath);
        }

    }

    protected ConfigurationModelImpl makeMergedModel(final String... sources) throws Exception {
        return makeMergedModel("test-module-", sources);
    }

    protected ConfigurationModelImpl makeMergedModel(final String modulePrefix, final String[] sources) throws Exception {
        final ConfigurationModelImpl configurationModelImpl = new ConfigurationModelImpl();
        for (int i = 0; i < sources.length; i++) {
            final List<AbstractDefinitionImpl> definitions = parseNoSort(sources[i], modulePrefix + i, true);
            assertTrue(definitions.size() > 0);
            final ModuleImpl module = definitions.get(0).getSource().getModule();
            module.setConfigResourceInputProvider(new TestResourceInputProvider());
            module.setContentResourceInputProvider(new TestResourceInputProvider());
            final GroupImpl group = module.getProject().getGroup();
            configurationModelImpl.addGroup(group);
        }
        return configurationModelImpl.build();
    }

    protected void expectNode(final String nodePath, final String childNodes, final String childProperties)
            throws RepositoryException {
        final Node node = session.getNode(nodePath);
        assertEquals(childNodes, createChildNodesString(node));
        assertEquals(childProperties, createChildPropertiesString(node));
    }

    protected void expectProp(final String path, final int expectedValueType, final String expectedValue)
            throws RepositoryException {
        final Property property = session.getProperty(path);
        assertEquals(expectedValueType, property.getType());

        final String actualValue;
        if (property.isMultiple()) {
            final List<String> values = new ArrayList<>();
            for (Value value : property.getValues()) {
                values.add(value.getString());
            }
            actualValue = values.toString();
        } else {
            actualValue = property.getValue().getString();
        }

        assertEquals(expectedValue, actualValue);
    }

}
