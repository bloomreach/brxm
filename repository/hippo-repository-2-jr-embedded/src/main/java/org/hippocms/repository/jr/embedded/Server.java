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

import java.io.File;
import java.io.InputStream;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.NamespaceRegistry;
//import javax.jcr.NodeTypeManager;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class Server {
    
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private String workingDir;
    private JackrabbitRepository repository;

    private void initialize(String workingDirectory) throws RepositoryException {
        workingDir = new File(workingDirectory).getAbsolutePath();
        InputStream config = getClass().getResourceAsStream("repository.xml");
        repository = RepositoryImpl.create(RepositoryConfig.create(config, workingDir));
        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));
    }
    public Server() throws RepositoryException {
        initialize(".");
    }
    public Server(String workingDirectory) throws RepositoryException {
        initialize(workingDirectory);
    }

    public Session login() throws RepositoryException {
        Session result = repository.login(new SimpleCredentials("username", "password".toCharArray()));
        log.info("Logged in as " + result.getUserID() + " to a " + repository.getDescriptor(Repository.REP_NAME_DESC)
                + " repository.");
        return result;
    }

    public void close() {
        repository.shutdown();
    }

    private void dump(Node parent, int level) throws RepositoryException {
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
                dump(node, level + 1);
            }
        }
    }
    public void dump(Node parent) throws RepositoryException {
        dump(parent, 0);
    }

    public static void main(String[] args) {
        try {
            Server server = null;
            if(args.length > 0)
                server = new Server(args.length > 0 ? args[0] : ".");
            else
                server = new Server();
            Session session = server.login();

            Node root = session.getRootNode();
            Workspace workspace = session.getWorkspace();

            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
            nsreg.registerNamespace("hippo", "http://www.hippocms.org/");

            NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
            NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
            NodeTypeDef ntd = new NodeTypeDef();
            ntd.setMixin(false);
            ntd.setName(new QName("http://www.hippocms.org/","facet"));
            EffectiveNodeType effnt = ntreg.registerNodeType(ntd);

            root.addNode("x");
            root.addNode("y");
            root.addNode("z");
            Node node = root.addNode("documents", "hippo:facet");

            server.dump(session.getRootNode());

            server.close();
        } catch(InvalidNodeTypeDefException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
