/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr.embedded;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;

import junit.framework.TestCase;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

/**
 * @version $Id$
 */
public class TrivialServerTest extends TestCase {

    private Server server;
    private JackrabbitRepository repository;

    public void setUp() throws ConfigurationException, RepositoryException {
        String workdir = System.getProperty("user.dir") + System.getProperty("file.separator") + "work";
        server = new Server(workdir);
        repository = server.startUp();
    }

    public void tearDown() {
        server.shutDown(repository);
    }

    public void test() throws RepositoryException {
        Session session = server.login(repository);

        Node root = session.getRootNode();
        Workspace workspace = session.getWorkspace();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        root.addNode("x");
        root.addNode("y");
        root.addNode("z");

        toStdout(session.getRootNode(), 0);
        //TODO: add asserts
                
        session.save();
        session.logout();
    }
    
    

    private void toStdout(Node parent, int level) throws RepositoryException {
        String prefix = "";
        for (int i = 0; i < level; i++) {
            prefix += "  ";
        }
        System.out.println(prefix + parent.getPath() + " [name=" + parent.getName() + ",depth=" + parent.getDepth()
                + "]");
        for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            System.out.print(prefix + "| " + prop.getPath() + " [name=" + prop.getName() + "] = ");
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                System.out.print("[ ");
                for (int i = 0; i < values.length; i++) {
                    System.out.println((i > 0 ? ", " : "") + values[i].getString());
                }
                System.out.println(" ]");
            } else {
                System.out.println(prop.getString());
            }
        }
        for (NodeIterator iter = parent.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system")) {
                toStdout(node, level + 1);
            }
        }
    }

}
