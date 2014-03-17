/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_ABSTRACT_COMPONENT;
import static org.junit.Assert.assertThat;

public class PrototypePageValidatorTest {

    private HstRequestContext hstRequestContext;
    private Session session;
    private Node node;
    private Object[] mocks;
    private NodeIterator nodeIterator;

    private PrototypePageValidator prototypePageValidator;
    private String prototypePageUuid = UUID.randomUUID().toString();

    @Before
    public void setUp() throws RepositoryException {

        // mocks
        this.hstRequestContext = createNiceMock(HstRequestContext.class);
        this.session = createNiceMock(Session.class);
        this.node = createNiceMock(Node.class);
        this.nodeIterator = createNiceMock(NodeIterator.class);
        this.mocks = new Object[]{hstRequestContext, session, node, nodeIterator};

        this.prototypePageValidator = new PrototypePageValidator(prototypePageUuid);

        // default expectancies
        expect(hstRequestContext.getSession()).andReturn(session).anyTimes();
        expect(session.getNodeByIdentifier(prototypePageUuid)).andReturn(node);
    }

    @Test
    public void testValidate_fails_node_not_correct_location() throws RepositoryException {

        expect(node.getPath()).andReturn("/not-correct-location/");
        replay(mocks);

        try {
            prototypePageValidator.validate(hstRequestContext);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.ITEM_NOT_CORRECT_LOCATION));
        }
    }

    @Test
    public void testValidate_fails_node_type_not_abstract_component() throws RepositoryException {

        expect(node.getPath()).andReturn("/" + HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES + "/");
        replay(mocks);

        try {
            prototypePageValidator.validate(hstRequestContext);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.INVALID_NODE_TYPE));
        }
    }

    @Test
    public void testValidate_fails_node_type_not_reference_component() throws RepositoryException {

        expect(node.getPath()).andReturn("/" + HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES + "/");
        expect(node.isNodeType(NODETYPE_HST_ABSTRACT_COMPONENT)).andReturn(true);
        expect(node.isNodeType(COMPONENT_PROPERTY_REFERECENCECOMPONENT)).andReturn(true);

        replay(mocks);

        try {
            prototypePageValidator.validate(hstRequestContext);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.INVALID_NODE_TYPE));
        }
    }

    @Test
    public void testValidate_succeeds() throws RepositoryException {

        expect(node.getPath()).andReturn("/" + HstNodeTypes.NODENAME_HST_PROTOTYPEPAGES + "/");
        expect(node.isNodeType(NODETYPE_HST_ABSTRACT_COMPONENT)).andReturn(true);
        expect(node.isNodeType(COMPONENT_PROPERTY_REFERECENCECOMPONENT)).andReturn(false);

        // mock that node has one child
        expect(node.getNodes()).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(true);
        expect(nodeIterator.next()).andReturn(node);
        expect(node.isNodeType(NODETYPE_HST_ABSTRACT_COMPONENT)).andReturn(true);
        expect(node.isNodeType(COMPONENT_PROPERTY_REFERECENCECOMPONENT)).andReturn(false);

        // mock that child has no children
        expect(node.getNodes()).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(false);

        replay(mocks);

        prototypePageValidator.validate(hstRequestContext);
    }
}
