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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.junit.Before;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModelTestUtils;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.builder.ConfigurationModelBuilder;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.jcr.event.EventCollector;
import org.onehippo.testutils.jcr.event.EventPojo;
import org.onehippo.testutils.jcr.event.ExpectedEvents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.impl.model.ModelTestUtils.parseNoSort;

/**
 * {@link ConfigurationConfigService} is a rather complex class, requiring a lot of unit tests.
 * In order to divide these unit tests over a number of unit test classes, this class provides
 * support and tooling for use in all of these unit test classes.
 */
public abstract class BaseConfigurationConfigServiceTest extends RepositoryTestCase {
    protected Node testNode;

    private static final String[] DEFAULT_BASELINE_SOURCES = {
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


    protected ConfigurationModel applyDefinitions(final String source) throws Exception {
        return applyDefinitions(new String[]{source}, makeMergedModel(DEFAULT_BASELINE_SOURCES), false);
    }

    protected ConfigurationModel applyDefinitions(final String source, final ConfigurationModel baseline) throws Exception {
        return applyDefinitions(new String[]{source}, baseline, false);
    }

    protected ConfigurationModel applyDefinitions(final String source, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(new String[]{source}, makeMergedModel(DEFAULT_BASELINE_SOURCES), false, expectedEvents);
    }

    protected ConfigurationModel applyDefinitions(final String source, final ConfigurationModel baseline, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(new String[]{source}, baseline, false, expectedEvents);
    }

    protected ConfigurationModel applyDefinitions(final String[] sources, final ExpectedEvents expectedEvents) throws Exception {
        return applyDefinitions(sources, makeMergedModel(DEFAULT_BASELINE_SOURCES), false, expectedEvents);
    }

    protected ConfigurationModel applyDefinitions(final String[] sources,
                                                  final ConfigurationModel baseline,
                                                  final boolean forceApply,
                                                  final ExpectedEvents expectedEvents) throws Exception {
        final ConfigurationModel configurationModel;

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

    protected ConfigurationModel applyDefinitions(final String[] sources,
                                                  final ConfigurationModel baseline,
                                                  final boolean forceApply) throws Exception {
        final ConfigurationModel configurationModel = makeMergedModel(sources);

        final ConfigurationConfigService helper = new ConfigurationConfigService();
        helper.computeAndWriteDelta(baseline, configurationModel, session, forceApply);
        session.save();

        return configurationModel;
    }

    private ConfigurationModel makeMergedModel(final String[] sources) throws Exception {
        final ConfigurationModelBuilder configurationModelBuilder = new ConfigurationModelBuilder();
        for (int i = 0; i < sources.length; i++) {
            final List<Definition> definitions = parseNoSort(sources[i], "test-module-" + i, ConfigDefinition.class);
            assertTrue(definitions.size() > 0);
            final ModuleImpl module = (ModuleImpl) definitions.get(0).getSource().getModule();
            module.setConfigResourceInputProvider(ModelTestUtils.getTestResourceInputProvider());
            module.setContentResourceInputProvider(ModelTestUtils.getTestResourceInputProvider());
            final GroupImpl configuration = (GroupImpl) module.getProject().getGroup();
            configurationModelBuilder.push(configuration);
        }
        return configurationModelBuilder.build();
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

    private String createChildNodesString(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Node child : new NodeIterable(node.getNodes())) {
            names.add(child.getName());
        }
        if (!node.getPrimaryNodeType().hasOrderableChildNodes()) {
            Collections.sort(names);
        }
        return names.toString();
    }

    private String createChildPropertiesString(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            names.add(property.getName());
        }
        Collections.sort(names);
        return names.toString();
    }
}
