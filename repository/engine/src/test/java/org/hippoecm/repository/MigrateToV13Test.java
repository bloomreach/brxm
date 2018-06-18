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

package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.hippoecm.repository.jackrabbit.HippoNodeTypeRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
@PowerMockIgnore("javax.management.*")

public class MigrateToV13Test {

    public static final String URL_REWRITER_SRC = "/content/urlrewriter";
    public static final String URL_REWRITER_DEST = "/hippo:configuration/hippo:modules/urlrewriter/hippo:moduleconfig";
    private Session session;

    MigrateToV13 testedClass;

    @Before
    public void setUp() throws Exception {

        HippoNodeTypeRegistry registry = createNiceMock(HippoNodeTypeRegistry.class);
        session = createNiceMock(Session.class);

        Workspace workspace = createNiceMock(Workspace.class);
        expect(session.getWorkspace()).andReturn(workspace).times(2);
        replay(session);

        NodeTypeManager typeManager = createNiceMock(NodeTypeManager.class);
        expect(workspace.getNodeTypeManager()).andReturn(typeManager).once();
        replay(workspace);

        testedClass = new MigrateToV13(session, registry, false);
    }

    @Test
    public void migrateUrlRewriter_nodes_do_not_exist() throws Exception {

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(MigrateToV13.class).build()) {
            testedClass.migrateUrlRewriter();
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    String.format("Source node %s does not exist, skipping migrating url rewriter", URL_REWRITER_SRC))));
        }
    }

    @Test
    public void migrateUrlRewriter_changes() throws Exception {

        EasyMock.reset(session);

        Node sourceNode = createNiceMock(Node.class);
        Node destinationNode = createNiceMock(Node.class);

        expect(session.nodeExists(URL_REWRITER_SRC)).andReturn(true).once();
        expect(session.nodeExists(URL_REWRITER_DEST)).andReturn(true).once();

        expect(session.getNode(URL_REWRITER_SRC)).andReturn(sourceNode).once();
        expect(session.getNode(URL_REWRITER_DEST)).andReturn(destinationNode).once();
        replay(session);

        PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
        expect(sourceNode.getProperties()).andReturn(propertyIterator);
        replay(sourceNode);

        Property property = createNiceMock(Property.class);
        expect(propertyIterator.hasNext()).andReturn(true).times(1);
        expect(propertyIterator.next()).andReturn(property);
        expect(propertyIterator.nextProperty()).andReturn(property);
        replay(propertyIterator);

        String propertyName = "urlrewriter:property";
        expect(property.getName()).andReturn(propertyName).anyTimes();
        replay(property);

        expect(destinationNode.getPath()).andReturn(URL_REWRITER_DEST);
        expect(destinationNode.hasProperty(propertyName)).andReturn(true);
        expect(destinationNode.getProperty(propertyName)).andReturn(createNiceMock(Property.class));
        replay(destinationNode);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(MigrateToV13.class).build()) {
            testedClass.migrateUrlRewriter();
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Migrating urlrewriter")));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(String.format("Migrating property '%s'", propertyName))));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(String.format("Removing property '%s' from source node", propertyName))));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(String.format("Setting property '%s' to destination node '%s'", propertyName, URL_REWRITER_DEST))));
        }
    }

}