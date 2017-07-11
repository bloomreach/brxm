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

import java.util.HashSet;

import javax.jcr.ItemExistsException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ContentSourceImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;

import com.google.common.collect.ImmutableList;

import junit.framework.AssertionFailedError;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class ConfigurationContentServiceTest {

    private ConfigurationBaselineService baselineService;

    private JcrContentProcessor jcrContentProcessor;

    private ConfigurationContentService configurationContentService;

    @Before
    public void setUp() throws Exception {
        baselineService = createNiceMock(ConfigurationBaselineService.class);
        jcrContentProcessor = createNiceMock(JcrContentProcessor.class);
        configurationContentService = new ConfigurationContentService(baselineService, jcrContentProcessor);
    }

    @Test
    public void apply_failed_content() throws Exception {

        final Session session = createNiceMock(Session.class);

        final ConfigurationModelImpl model = createNiceMock(ConfigurationModelImpl.class);

        final ConfigurationNodeImpl configurationNode = new ConfigurationNodeImpl();
        configurationNode.setResidualNodeCategory(ConfigurationItemCategory.CONTENT);

        final ModuleImpl stubModule = new ModuleImpl("stubModule", new ProjectImpl("stubProject", new GroupImpl("stubGroup")));
        final DefinitionNodeImpl defNode4 = addContentDefinition(stubModule, "source4", "/some/sibling/path").getNode();
        final DefinitionNodeImpl defNode2 = addContentDefinition(stubModule, "source2", "/some/path/child").getNode();
        final DefinitionNodeImpl defNode1 = addContentDefinition(stubModule, "source1", "/some/path").getNode();
        final DefinitionNodeImpl defNode3 = addContentDefinition(stubModule, "source3", "/some/sibling").getNode();

        expect(model.getConfigurationRootNode()).andReturn(configurationNode).atLeastOnce();
        expect(model.getModules()).andReturn(ImmutableList.of(stubModule)).anyTimes();
        replay(model);

        expect(baselineService.getAppliedContentPaths(session)).andReturn(new HashSet<>()).times(4);
        baselineService.addAppliedContentPath(anyString(), anyObject());
        expectLastCall().times(2);
        replay(baselineService);

        jcrContentProcessor.apply(defNode1, ActionType.APPEND, session);
        expectLastCall().andThrow(new ItemExistsException("This is an intentional Exception")).once();
        jcrContentProcessor.apply(defNode3, ActionType.APPEND, session);
        expectLastCall().once();
        jcrContentProcessor.apply(defNode4, ActionType.APPEND, session);
        expectLastCall().once();
        jcrContentProcessor.apply(defNode2, ActionType.APPEND, session);
        expectLastCall().andThrow(new AssertionFailedError("Should not be executed")).anyTimes();

        replay(jcrContentProcessor);

        configurationContentService.apply(model, session);

        verify(baselineService);
        verify(jcrContentProcessor);
    }

    private ContentDefinitionImpl addContentDefinition(ModuleImpl module, String sourceName, String path) {
        ContentSourceImpl contentSource = module.addContentSource(sourceName);
        ContentDefinitionImpl contentDefinition = new ContentDefinitionImpl(contentSource);
        module.getContentDefinitions().add(contentDefinition);
        DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(path, contentDefinition);
        contentDefinition.setNode(definitionNode);
        return contentDefinition;
    }

}