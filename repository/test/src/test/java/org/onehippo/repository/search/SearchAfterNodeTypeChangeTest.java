/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.search;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class SearchAfterNodeTypeChangeTest extends RepositoryTestCase {

    @Test
    public void assert_newly_added_nodetypes_can_be_found_through_searching_by_already_logged_in_user() throws Exception {

        // create test data
        Node testNode = session.getRootNode().addNode("test");
        testNode.addMixin("mix:referenceable");

        testNode.addNode("doc1", "hippo:authtestdocument").setProperty("title", "foo");
        session.save();

        final Session editor = server.login(new SimpleCredentials("editor", "editor".toCharArray()));

        final Query query = editor.getWorkspace().getQueryManager().createQuery("/jcr:root/test//element(*,nt:base)", "xpath");
        final QueryResult execute = query.execute();

        assertEquals(1L, execute.getNodes().getSize());

        // add a new nodetype and add documents of this new nodetype. Make sure the 'editor' can find these documents as well

        addNewNodeType();

        testNode.addNode("doc2", "hippo:authtestsubdocument").setProperty("title", "foo");
        session.save();

        editor.refresh(false);

        assertEquals("Expected that doc2 would also be found by 'editor' ",2L, query.execute().getNodes().getSize());
    }

    private void addNewNodeType() throws ParseException, RepositoryException, InvalidNodeTypeDefException {
        final InputStream in = IOUtils.toInputStream(
                "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>\n" +
                        "\n" +
                        "[hippo:authtestsubdocument] > hippo:authtestdocument");
        RepositoryImpl repository = (RepositoryImpl)RepositoryDecorator.unwrap(session.getRepository());
        final NamespaceRegistry namespaceRegistry = repository.getNamespaceRegistry();
        final CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> cndReader =
                new HippoCompactNodeTypeDefReader(new InputStreamReader(in), "<test>", namespaceRegistry);
        final List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
        final NodeTypeRegistry nodeTypeRegistry = ((NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager()).getNodeTypeRegistry();
        for (QNodeTypeDefinition ntd : ntdList) {
            nodeTypeRegistry.registerNodeType(ntd);
        }
    }
}
