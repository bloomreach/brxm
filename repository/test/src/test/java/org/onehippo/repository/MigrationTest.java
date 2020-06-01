/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.cluster.NamespaceEventListener;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

/**
 * Illustrates the mechanism whereby incompatible changes to a node type can be done.
 */
@Ignore
public class MigrationTest extends RepositoryTestCase {

    private static final String oldUri = "http://www.onehippo.org/jcr/test/1.0";
    private static final String newUri = "http://www.onehippo.org/jcr/test/1.1";

    private NamespaceRegistry namespaceRegistry;
    private NamespaceEventListener namespaceEventListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        RepositoryImpl repository = (RepositoryImpl) RepositoryDecorator.unwrap(session.getRepository());
        namespaceRegistry = repository.getNamespaceRegistry();
        namespaceEventListener = (NamespaceEventListener) namespaceRegistry;
        final Node test = session.getRootNode().addNode("test");
        namespaceRegistry.registerNamespace("foo", oldUri);
        importCnd("/migration/old.cnd");
        final Node foo = test.addNode("foo", "foo:foo");
        foo.setProperty("foo:bar", "bar");
        session.save();
    }

    @Test
    public void migrate() throws Exception {
        namespaceEventListener.externalRemap("foo", "migrate", oldUri);
        namespaceRegistry.registerNamespace("foo", newUri);

        importCnd("/migration/new.cnd");
        final Session newSession = session.impersonate(CREDENTIALS);
        final Node newFoo = newSession.getNode("/test/foo");
        newFoo.addMixin("hipposys:unstructured");
        newFoo.setPrimaryType("foo:foo");
        newFoo.setProperty("foo:baz", "baz");
        newFoo.getProperty("migrate:bar").remove();
        newFoo.removeMixin("hipposys:unstructured");
        newSession.save();
    }

    private void importCnd(String cnd) throws Exception {
        final InputStream in = getClass().getResourceAsStream(cnd);
        final CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> cndReader =
                new HippoCompactNodeTypeDefReader(new InputStreamReader(in), "<test>", namespaceRegistry);
        final List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
        final NodeTypeRegistry nodeTypeRegistry = ((NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager()).getNodeTypeRegistry();
        for (QNodeTypeDefinition ntd : ntdList) {
            nodeTypeRegistry.registerNodeType(ntd);
        }
    }
}
