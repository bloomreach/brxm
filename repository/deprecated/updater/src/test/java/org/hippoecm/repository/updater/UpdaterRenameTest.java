/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.updater;

import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdaterRenameTest extends RepositoryTestCase {

    private final String[] content = {
        "/test", "nt:unstructured",
            "/test/d", "testsubns:document",
                "jcr:mixinTypes", "mix:versionable",
                "testsuperns:x", "1",
                "testsubns:x", "2",
                "testsuperns:y", "11",
                "testsubns:y", "12"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
        session.save();
    }

    @Test
    public void testCommonNamespace() throws RepositoryException {
        UpdaterModule module = new UpdaterModule() {
            public void register(UpdaterContext context) {
                context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("testsubns:document") {
                    protected void leaving(Node node, int level) throws RepositoryException {
                        if (node.hasProperty("testsuperns:y")) {
                            node.setProperty("testsuperns:z", node.getProperty("testsuperns:y").getString());
                        }
                        if (node.hasProperty("testsubns:y")) {
                            node.setProperty("testsubns:z", node.getProperty("testsubns:y").getString());
                        }
                    }
                });
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "testsuperns", "-",
                                                                                new InputStreamReader(
                                                                                        getClass().getClassLoader().getResourceAsStream(
                                                                                                "repository-testsuperns2.cnd"))));
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "testsubns", "-",
                                                                                new InputStreamReader(
                                                                                        getClass().getClassLoader().getResourceAsStream(
                                                                                                "repository-testsubns2.cnd"))));
            }
        };
        List<UpdaterModule> list = new LinkedList<UpdaterModule>();
        list.add(module);
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        UpdaterEngine.migrate(session, new Modules<UpdaterModule>(list));
        session.logout();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node = session.getRootNode().getNode("test").getNode("d");
        assertTrue(node.hasProperty("testsuperns:x"));
        assertTrue(node.hasProperty("testsubns:x"));
        assertFalse(node.hasProperty("testsuperns:y"));
        assertFalse(node.hasProperty("testsubns:y"));
        assertTrue(node.hasProperty("testsuperns:z"));
        assertTrue(node.hasProperty("testsubns:z"));
    }
}
