/*
 * Copyright 2006 Hippo
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

import java.io.File;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.SimpleCredentials;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
// import javax.jcr.NodeTypeManager; Not yet in JCR1

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private JackrabbitRepository repository;
    private Session session;

    Server() throws RepositoryException {
        String workingDir = new File(".").getAbsolutePath();
        InputStream config = getClass().getResourceAsStream("repository.xml");
        repository = RepositoryImpl.create(RepositoryConfig.create(config, workingDir));
        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("node type registration support: " + result);
        session = repository.login(new SimpleCredentials("username", "password".toCharArray()));
        log.info("Logged in as " + session.getUserID() + " to a " + repository.getDescriptor(Repository.REP_NAME_DESC)
                + " repository.");
    }

    void traverse(Node parent) throws RepositoryException {
        System.out.println();
        traverse(parent, 0);
        System.out.println();
    }

    void traverse(Node parent, int level) throws RepositoryException {
        String prefix = "";
        for (int i = 0; i < level; i++)
            prefix += "  ";
        System.out.println(prefix + parent.getPath() + " [name=" + parent.getName() + ",depth=" + parent.getDepth()
                + "]");
        for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            System.out.print(prefix + "| " + prop.getPath() + " [name=" + prop.getName() + "] = ");
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                System.out.print("[ ");
                for (int i = 0; i < values.length; i++)
                    System.out.println((i > 0 ? ", " : "") + values[i].getString());
                System.out.println(" ]");
            } else
                System.out.println(prop.getString());
        }
        for (NodeIterator iter = parent.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (!node.getPath().equals("/jcr:system"))
                traverse(node, level + 1);
        }
    }

    void test() throws RepositoryException {
        Node root = session.getRootNode();
        Workspace workspace = session.getWorkspace();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        root.addNode("x");
        root.addNode("y");
        root.addNode("z");

        traverse(session.getRootNode());
        session.save();
        session.logout();
        repository.shutdown();
    }

    public static void main(String[] args) throws Exception {
        try {
            Server server = new Server();
            server.test();
        } catch (RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
