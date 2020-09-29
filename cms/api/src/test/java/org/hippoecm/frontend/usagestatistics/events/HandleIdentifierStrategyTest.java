/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.usagestatistics.events;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.value.StringValue;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;

public class HandleIdentifierStrategyTest {

    private MockNode root;
    private MockNode childOfHandle;
    private MockNode handle;
    private MockNode descendantOfRootAscendantOfHandle;
    private IdentifierStrategy strategy;
    private MockNode jcrFrozen;
    private MockNode ntVersion;

    @Before
    public void setUp() throws Exception {
        strategy = new HandleIdentifierStrategy();
        root = MockNode.root();
        descendantOfRootAscendantOfHandle = new MockNode("descendantOfRootAscendantOfHandle");
        handle = new MockNode("handle", HippoNodeType.NT_HANDLE);
        childOfHandle = new MockNode("child");
        root.addNode(descendantOfRootAscendantOfHandle);
        descendantOfRootAscendantOfHandle.addNode(handle);
        handle.addNode(childOfHandle);
        ntVersion = new MockNode("nt:version", JcrConstants.NT_VERSION);
        jcrFrozen = new MockNode("jcr:frozenNode", JcrConstants.NT_FROZEN_NODE);
        jcrFrozen.setProperty(HippoNodeType.HIPPO_RELATED, new Value[]{new StringValue(handle.getIdentifier())} );
        ntVersion.addNode(jcrFrozen);
        root.addNode(ntVersion);
    }

    @Test
    public void getIdentifierFromRootReturnEmpty() throws RepositoryException {
        final String actual = strategy.getIdentifier(root);
        Assert.assertNull(actual);
    }

    @Test
    public void getIdentifierFromHandleReturnIdentifierOfHandle() throws RepositoryException {
        final String expected = handle.getIdentifier();
        final String actual = strategy.getIdentifier(handle);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getIdentifierFromDescendantOfHandleReturnIdentifierOfHandle() throws RepositoryException{
        final String expected = handle.getIdentifier();
        final String actual = strategy.getIdentifier(childOfHandle);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getIdentifierAscendantOfHandleDescendantOfRootReturnEmpty() throws RepositoryException{
        final String actual = strategy.getIdentifier(descendantOfRootAscendantOfHandle);
        Assert.assertNull(actual);
    }

    @Test
    public void getIdentifierAssociatedWithVersionNodeReturnIdentifierOfHandle() throws RepositoryException {
        final String expected = handle.getIdentifier();
        final String actual = strategy.getIdentifier(ntVersion);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getIdentifierAssociatedWithFrozenNodeReturnIdentifierOfHandle() throws RepositoryException {
        final String expected = handle.getIdentifier();
        final String actual = strategy.getIdentifier(jcrFrozen);
        Assert.assertEquals(expected, actual);
    }
}
