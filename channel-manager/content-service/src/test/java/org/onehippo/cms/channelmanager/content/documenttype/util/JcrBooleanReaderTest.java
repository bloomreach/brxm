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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JcrBooleanReaderTest {

    @Test
    public void isSingleton() {
        assertTrue(JcrBooleanReader.get() == JcrBooleanReader.get());
    }

    @Test
    public void readBoolean() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("prop", false);

        assertThat(JcrBooleanReader.get().read(node, "prop"), equalTo(Optional.of(false)));
    }

    @Test
    public void readMissingProperty() throws RepositoryException {
        final MockNode node = MockNode.root();
        assertThat(JcrBooleanReader.get().read(node, "prop"), equalTo(Optional.empty()));
    }

    @Test
    public void readOtherType() throws RepositoryException {
        final MockNode node = MockNode.root();
        node.setProperty("prop", new String[]{"a", " b"});

        assertThat(JcrBooleanReader.get().read(node, "prop"), equalTo(Optional.empty()));
    }

    @Test
    public void readProblem() throws RepositoryException {
        final Node node = EasyMock.createMock(Node.class);
        expect(node.hasProperty("prop")).andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("/");
        replay(node);

        assertThat(JcrBooleanReader.get().read(node, "prop"), equalTo(Optional.empty()));
    }
}