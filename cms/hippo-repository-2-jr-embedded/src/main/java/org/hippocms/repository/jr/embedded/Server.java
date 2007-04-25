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
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class Server {
    
    public final static String NS_URI = "http://www.hippocms.org/";
    public final static String NS_PREFIX = "hippo";

    public static final int RMI_PORT = 1099;
    public static final String RMI_NAME = "jackrabbit.repository";

    private final static QName NODE_FACETSEARCH = new QName(NS_URI, "facetsearch");
    private final static QName NODE_FACETRESULT = new QName(NS_URI, "facetresult");
    private final static QName PROP_DOCBASE     = new QName(NS_URI, "docbase");
    private final static QName PROP_FACETS      = new QName(NS_URI, "facets");
    private final static QName PROP_SEARCH      = new QName(NS_URI, "search");

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private String workingDir;
    private JackrabbitRepository repository;
    private Remote rmiRepository;

    private void initialize(String workingDirectory) throws RepositoryException {
        initializeRepository(workingDirectory);
        initializeNodeTypes();
    }
    private void initializeRepository(String workingDirectory) throws RepositoryException {
        workingDir = new File(workingDirectory).getAbsolutePath();
        InputStream config = getClass().getResourceAsStream("repository.xml");
        repository = RepositoryImpl.create(RepositoryConfig.create(config, workingDir));
        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));
    }
    private void initializeNodeTypes() throws RepositoryException {
      Session session = login();
      try {
        Workspace workspace = session.getWorkspace();

        NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
        try {
          nsreg.registerNamespace(NS_PREFIX, NS_URI);
        } catch(javax.jcr.NamespaceException ex) {
          log.warn(ex.getMessage());
        }
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
        try {
          ntreg.unregisterNodeType(NODE_FACETSEARCH);
        } catch(NoSuchNodeTypeException ex) {
          // save to ignore
        }
        try {
          ntreg.unregisterNodeType(NODE_FACETRESULT);
        } catch(NoSuchNodeTypeException ex) {
          // save to ignore
        }

        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setMixin(false);
        ntd.setName(NODE_FACETRESULT);
        try {
          EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
        } catch(javax.jcr.NamespaceException ex) {
          log.warn(ex.getMessage());
        }

        PropDefImpl pd;
        PropDef[] pds = new PropDef[3];
        pds[0] = pd = new PropDefImpl();
        pd.setName(PROP_DOCBASE);
        pd.setRequiredType(PropertyType.STRING);
        pd.setMandatory(true);
        pd.setAutoCreated(false);
        pd.setMultiple(false);
        pd.setProtected(false);
        pd.setDeclaringNodeType(NODE_FACETSEARCH);
        pds[1] = pd = new PropDefImpl();
        pd.setName(PROP_FACETS);
        pd.setRequiredType(PropertyType.STRING);
        pd.setMandatory(true);
        pd.setAutoCreated(false);
        pd.setMultiple(true);
        pd.setProtected(false);
        pd.setDeclaringNodeType(NODE_FACETSEARCH);
        pds[2] = pd = new PropDefImpl();
        pd.setName(PROP_SEARCH);
        pd.setRequiredType(PropertyType.STRING);
        pd.setMandatory(false);
        pd.setAutoCreated(false);
        pd.setMultiple(true);
        pd.setProtected(false);
        pd.setDeclaringNodeType(NODE_FACETSEARCH);

        NodeDefImpl nd;
        NodeDef[] nds = new NodeDef[2];
        nds[0] = nd = new NodeDefImpl();
        nd.setName(new QName("", "resultset"));
        nd.setRequiredPrimaryTypes(new QName[]{NODE_FACETRESULT});
        nd.setDefaultPrimaryType(NODE_FACETRESULT);
        nd.setProtected(false);
        nd.setAllowsSameNameSiblings(true);
        nd.setDeclaringNodeType(NODE_FACETSEARCH);
        nds[1] = nd = new NodeDefImpl();
        nd.setName(new QName("", "*"));
        nd.setRequiredPrimaryTypes(new QName[]{NODE_FACETSEARCH});
        nd.setDefaultPrimaryType(NODE_FACETSEARCH);
        nd.setProtected(false);
        nd.setAllowsSameNameSiblings(true);
        nd.setDeclaringNodeType(NODE_FACETSEARCH);

        ntd = new NodeTypeDef();
        ntd.setMixin(false);
        ntd.setPropertyDefs(pds);
        ntd.setChildNodeDefs(nds);
        ntd.setName(NODE_FACETSEARCH);
        try {
          EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
        } catch(javax.jcr.NamespaceException ex) {
          log.warn(ex.getMessage());
          System.err.println(ex.getMessage());
          ex.printStackTrace(System.err);
        }
        session.save();
      } catch(InvalidNodeTypeDefException ex) {
        throw new RepositoryException("Could not preload repository with hippo node types", ex);
      }
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
        return new VirtualSessionImpl(result);
    }

    public void close() {
      if(repository != null) {
        try {
          repository.shutdown();
        } catch(Exception ex) {
          // ignore;
        }
        repository = null;
      }
    }

    private static void dump(Node parent, int level) throws RepositoryException {
        String prefix = "";
        for (int i = 0; i < level; i++) {
            prefix += "  ";
        }
        System.out.println(prefix + parent.getPath() + " [name=" + parent.getName() + ",depth=" + parent.getDepth() + "]");
        for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            System.out.print(prefix + "| " + prop.getPath() + " [name=" + prop.getName() + "] = ");
            if(prop.getDefinition().isMultiple()) {
              Value[] values = prop.getValues();
              System.out.print("[ ");
              for (int i = 0; i < values.length; i++) {
                System.out.print((i > 0 ? ", " : "") + values[i].getString());
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
    public static void dump(Node parent) throws RepositoryException {
        dump(parent, 0);
    }


    public void run() throws RemoteException, AlreadyBoundException {
      Runtime.getRuntime().addShutdownHook(new Thread() {
          public void run() {
            close();
            if(rmiRepository != null) {
              rmiRepository = null;
              try {
                Naming.unbind(RMI_NAME);
              }
              catch (Exception e) {
                // ignore
              }
            }
          }
        });
      Remote remote = new ServerAdapterFactory().getRemoteRepository(new VirtualRepositoryImpl(repository));
      System.setProperty("java.rmi.server.useCodebaseOnly", "true");
      Registry registry = LocateRegistry.createRegistry(RMI_PORT);
      registry.bind(RMI_NAME, remote);
      rmiRepository = remote;
      log.info("RMI Server available on rmi://localhost:"+RMI_PORT+"/"+RMI_NAME);
      for(;;) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ex) {
          System.err.println(ex);
        }
      }
    }

    public static void main(String[] args) {
        try {
            Server server = null;
            if(args.length > 0)
                server = new Server(args.length > 0 ? args[0] : ".");
            else
                server = new Server();
            
            Session session = server.login();

            Node docs, node, root = session.getRootNode();
            docs = root.addNode("navigation");
            node = docs.addNode("byproduct","hippo:facetsearch");
            node.setProperty("hippo:facets", new String[] { "product", "brand" });
            node.setProperty("hippo:docbase", "documents");
	    // node.setProperty("hippo:search", new String[] { "has='ambilight'" });
            docs = root.addNode("documents");
            node = docs.addNode("42PF9831D");
            node.addMixin("mix:referenceable");
            node.setProperty("product","television");
            node.setProperty("brand","philips");
            node = docs.addNode("Bravia");
            node.addMixin("mix:referenceable");
            node.setProperty("product","television");
            node.setProperty("brand","sony");
            node = docs.addNode("DVP-FX810");
            node.addMixin("mix:referenceable");
            //node.setProperty("product",new String[] { "television", "dvdplayer" });
            node.setProperty("product","dvdplayer");
            node.setProperty("brand","sony");
            node = docs.addNode("spoon");
            node.addMixin("mix:referenceable");
            node.setProperty("product","dvdplayer");
            session.save();
            root = session.getRootNode();
            node = root.getNode("documents/42PF9831D");
            String uuid;
            if(!node.isNodeType("mix:referenceable")) {
              uuid = "";
              System.out.println("UUID NOT SUPPORTED");
            } else
              uuid = node.getUUID();
            System.out.println("\nEntire tree: "+session.getRootNode().getClass().getName());
            server.dump(session.getRootNode());

            System.out.println("\nTelevisions:");
            node = root.getNode("navigation/byproduct/television/resultset");
            for(NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
              node = iter.nextNode();
              server.dump(node);
            }

            System.out.println("\nTelevisions by Philips:");
            node = root.getNode("navigation/byproduct/television/philips/resultset");
            boolean found = false;
            for(NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
              node = iter.nextNode();
              try {
                if(uuid.equals(node.getUUID()))
                  found = true;
              } catch(UnsupportedRepositoryOperationException ex) {
              }
              server.dump(node);
            }
            System.out.println(found ? "FOUND" : "NOT FOUND");

            session.save();

            if(false) {
              node = root.getNode("navigation/free/product[facet='television']/brand[facet='philips']");
              found = false;
              for(NodeIterator iter=node.getNodes(); iter.hasNext(); ) {
                node = iter.nextNode();
                if(uuid.equals(node.getUUID()))
                  found = true;
                server.dump(node);
              }
              System.out.println(found ? "FOUND" : "NOT FOUND");
            }

            
            session.logout();
            server.run();
            server.close();
        } catch(RemoteException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(AlreadyBoundException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
