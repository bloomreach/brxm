/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

public class HandleIdentifierStrategyTest {

    private MockNode root;
    private MockNode childOfHandle;
    private MockNode handle;
    private MockNode descendantOfRootAscendantOfHandle;
    private IdentifierStrategy strategy;

    @Before
    public void setUp() throws Exception {
        strategy = new HandleIdentifierStrategy();
        root = new MockNode("root","rep:root");
        descendantOfRootAscendantOfHandle = new MockNode("descendantOfRootAscendantOfHandle");
        handle = new MockNode("handle", HippoNodeType.NT_HANDLE);
        childOfHandle = new MockNode("child");
        root.addNode(descendantOfRootAscendantOfHandle);
        descendantOfRootAscendantOfHandle.addNode(handle);
        handle.addNode(childOfHandle);
    }

    @Test
    public void getIdentifierFromRootReturnEmpty() throws RepositoryException {
        final String actual = strategy.getIdentifier(root);
        Assert.assertNotNull(actual);
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
        Assert.assertNotNull(actual);
    }
}
