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

import java.util.List;
import java.util.Iterator;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.NamespaceException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.XASession;
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
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;

import org.hippocms.repository.jr.servicing.ServicingDecoratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippocms.repository.jr.servicing.WorkspaceDecorator;
import org.hippocms.repository.jr.servicing.NodeDecorator;
import org.hippocms.repository.jr.servicing.Workflow;

/**
 * @version $Id$
 *
 */
public class Server {
    
    public final static String NS_URI = "http://www.hippocms.org/";
    public final static String NS_PREFIX = "hippo";

    public static int RMI_PORT = 1099;
    public static String RMI_NAME = "jackrabbit.repository";

    private String systemUsername = "username";
    private String systemPassword = "password";

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private String workingDir;
    private JackrabbitRepository jackrabbitRepository;
    private ServicingDecoratorFactory hippoRepositoryFactory;
    private Remote rmiRepository;
    protected Repository repository;

    private void initialize(String workingDirectory) throws RepositoryException {
        workingDir = new File(workingDirectory).getAbsolutePath();
        InputStream config = getClass().getResourceAsStream("repository.xml");
        jackrabbitRepository = RepositoryImpl.create(RepositoryConfig.create(config, workingDir));
        repository = jackrabbitRepository;

        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));

        repository = new VirtualRepositoryImpl(repository);

        hippoRepositoryFactory = new ServicingDecoratorFactory();
        repository = hippoRepositoryFactory.getRepositoryDecorator(repository);

        Session session = login();
        try {
          Workspace workspace = session.getWorkspace();

          NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
          try {
            nsreg.registerNamespace(NS_PREFIX, NS_URI);
          } catch(javax.jcr.NamespaceException ex) {
            log.warn(ex.getMessage());
          }

          String cndName = "repository.cnd";
          InputStream cndStream = getClass().getResourceAsStream(cndName);
          BufferedReader cndInput = new BufferedReader(new InputStreamReader(cndStream));
          CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName);
          List ntdList = cndReader.getNodeTypeDefs();
          NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
          NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
          boolean progress;
          do {
            progress = false;
            for(Iterator iter=ntdList.iterator(); iter.hasNext(); ) {
              NodeTypeDef ntd = (NodeTypeDef) iter.next();
              try {
                ntreg.unregisterNodeType(ntd.getName());
                progress = true;
              } catch(NoSuchNodeTypeException ex) {
                // save to ignore
              }
            }
          } while(progress);
          for(Iterator iter=ntdList.iterator(); iter.hasNext(); ) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            try {
              try {
                EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
              } catch(NamespaceException ex) {
                log.warn(ex.getMessage());
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
              }
            } catch(RepositoryException ex) {
              if(ex.getMessage().equals("not yet implemented")) {
                log.warn("cannot override typing; hoping they are equivalent");
              } else
                throw ex;
            }
          }
          session.save();

          /* FIXME:
           * The following should be implemented as an XML file which is by default imported
           * at startup when the navigation node is not present.
           */
          Node node, docs = session.getRootNode().addNode("navigation");

          node = docs.addNode("bySourceTest1","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceTest2","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "section" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceTest3","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "section", "type" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceTest4","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "section", "type", "author" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceTest5","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "year", "month", "day", "author" });
          node.setProperty("hippo:docbase", "files");
            
          node = docs.addNode("bySourceTest6","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "author", "year", "month", "day" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceTest7","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "documentdate", "section" });
          node.setProperty("hippo:docbase", "files");
          
          node = docs.addNode("bySectionSource","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "section", "source", "year", "month", "author", "type" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySectionDate","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "section", "year", "month", "source", "author", "type" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceSection","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "section", "year", "month", "author", "type" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("bySourceDate","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "source", "year", "month", "section", "author", "type" });
          node.setProperty("hippo:docbase", "files");
            
          node = docs.addNode("byAuthorDate","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "author", "year", "month", "section", "source", "type" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("byAuthorSource","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "author", "section", "source", "year", "month", "type" });
          node.setProperty("hippo:docbase", "files");
            
          node = docs.addNode("byDateAuthor","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "year", "month", "day", "author", "section", "source", "type" });
          node.setProperty("hippo:docbase", "files");

          node = docs.addNode("byDateSection","hippo:facetsearch");
          node.setProperty("hippo:facets", new String[] { "year", "month", "day", "section", "source", "author", "type" });
          node.setProperty("hippo:docbase", "files");
          session.save();
          /* end of FIXME */

        } catch(ParseException ex) {
          throw new RepositoryException("Could not preload repository with hippo node types", ex);
        } catch(InvalidNodeTypeDefException ex) {
          throw new RepositoryException("Could not preload repository with hippo node types", ex);
        }
    }
    public Server() throws RepositoryException {
        initialize(".");
    }
    public Server(String location) throws RepositoryException {
        if(location.startsWith("rmi://")) {
          try {
            ClientRepositoryFactory factory = new ClientRepositoryFactory();
            repository = factory.getRepository(location);
            hippoRepositoryFactory = new ServicingDecoratorFactory();
            repository = hippoRepositoryFactory.getRepositoryDecorator(repository);
          } catch(RemoteException ex) {
            // FIXME
          } catch(NotBoundException ex) {
            // FIXME
          } catch(MalformedURLException ex) {
            // FIXME
          }
        } else {
            initialize(location);
        }
    }

    public Session login() throws RepositoryException {
        Session session = null;
        if(systemUsername != null)
            session = repository.login(new SimpleCredentials(systemUsername, systemPassword.toCharArray()));
        else
            session = repository.login();
        log.info("Logged in as " + session.getUserID() + " to a " + repository.getDescriptor(Repository.REP_NAME_DESC)
                + " repository.");
        return session;
    }

    public void close() {
      if(jackrabbitRepository != null) {
        try {
          jackrabbitRepository.shutdown();
        } catch(Exception ex) {
          // ignore;
        }
        repository = null;
      }
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
      Remote remote = new ServerAdapterFactory().getRemoteRepository(repository);
      System.setProperty("java.rmi.server.useCodebaseOnly", "true");
      Registry registry = LocateRegistry.createRegistry(RMI_PORT);
      registry.bind(RMI_NAME, remote);
      rmiRepository = remote;
      log.info("RMI Server available on rmi://localhost:"+RMI_PORT+"/"+RMI_NAME);
      for(;;) {
        try {
          Thread.sleep(333);
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
