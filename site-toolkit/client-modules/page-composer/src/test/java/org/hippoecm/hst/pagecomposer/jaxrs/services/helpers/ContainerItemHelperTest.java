/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.EasyMock;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

public class ContainerItemHelperTest {

    private LockHelper lockHelper;
    private PageComposerContextService service;
    private List<Object> mocks = new ArrayList<>();

    private ContainerItemHelper helper;

    @Before
    public void setUp() {
        ModifiableRequestContextProvider.set(new MockHstRequestContext());
        helper = new ContainerItemHelper();
        helper.setLockHelper(lockHelper = mock(LockHelper.class));
        helper.setPageComposerContextService(service = mock(PageComposerContextService.class));
    }

    @Test
    public void testLock_calls_lockhelper() throws RepositoryException {

        final Session session = mock(Session.class);
        expect(session.getRootNode()).andReturn(mock(Node.class)).anyTimes();

        final Node node = mock(Node.class);
        expect(node.getSession()).andReturn(session).anyTimes();
        // mock traversing up to the container component
        expect(node.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)).andReturn(true).anyTimes();
        expect(node.getParent()).andReturn(node).anyTimes();
        expect(node.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)).andReturn(true).anyTimes();

        expect(service.getRequestConfigNodeById("id", NODETYPE_HST_CONTAINERITEMCOMPONENT, session)).andReturn(node);
        lockHelper.acquireLock(node, 0L);
        expectLastCall().once();

        replay(mocks.toArray());

        helper.lock("id", 0L, session);

        verify(lockHelper);
    }

    @Test(expected = ClientException.class)
    public void testLock_throws_if_id_is_not_found() throws RepositoryException {

        final String id = "id";
        final Session session = null;
        expect(service.getRequestConfigNodeById(id, NODETYPE_HST_CONTAINERITEMCOMPONENT, session))
                .andThrow(new ItemNotFoundException());
        replay(mocks.toArray());

        helper.lock(id, 0L, session);
    }


    private <T> T mock(final Class<T> type) {
        T mock = EasyMock.createNiceMock(type);
        mocks.add(mock);
        return mock;
    }
}
