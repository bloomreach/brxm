/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.restapi.content.visitors;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.restapi.NodeVisitor;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.repository.HippoStdNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HippoHandleVisitorTest {


    private HippoHandleVisitor testedClass;

    @Before
    public void setUp() {
        testedClass = new HippoHandleVisitor();
    }

    @Test
    public void visit() throws RepositoryException {

        final String documentNodeName = "DocumentNode";
        MockNode rootNode = new MockNode(documentNodeName, "hippo:root");
        MockNode mockNode = rootNode.addNode(documentNodeName, "hippo:handle");

        final Node variantNode = mockNode.addNode(documentNodeName, "connect:documentation");
        variantNode.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);

        final ResourceContext resourceContext = createNiceMock(ResourceContext.class);
        final LinkedHashMap<String, Object> response = new LinkedHashMap<>();

        final NodeVisitor returnedVisitor = createNiceMock(NodeVisitor.class);
        expect(resourceContext.getVisitor(variantNode)).andReturn(returnedVisitor).atLeastOnce();
        replay(resourceContext);

        testedClass.visit(resourceContext, mockNode, response);
        expectLastCall().once();

        assertNotNull(response.get("id"));
        assertEquals(documentNodeName, response.get("name"));
        assertEquals(documentNodeName, response.get("displayName"));
    }

    @Test
    public void visit_no_variant() throws RepositoryException {

        final String documentNodeName = "DocumentNode";
        MockNode rootNode = new MockNode(documentNodeName, "hippo:root");
        MockNode handleNode = rootNode.addNode(documentNodeName, "hippo:handle");

        final ResourceContext resourceContext = createNiceMock(ResourceContext.class);
        final LinkedHashMap<String, Object> response = new LinkedHashMap<>();


        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(HippoHandleVisitor.class).build()) {
            testedClass.visit(resourceContext, handleNode, response);
            interceptor.messages().forEach(new Consumer<String>() {
                @Override
                public void accept(final String s) {
                    System.out.println(s);
                }
            });
            assertTrue(interceptor.messages()
                    .anyMatch(m -> m.contains("Variant for handle '/DocumentNode'")));
        }
    }

}