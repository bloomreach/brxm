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
import java.io.IOException;

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
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;

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
import org.hippocms.repository.jr.servicing.ServerServicingAdapterFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippocms.repository.jr.servicing.Workflow;
import org.hippocms.repository.jr.servicing.ClientServicesAdapterFactory;

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
              } catch(RepositoryException ex) {
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

        } catch(ParseException ex) {
          throw new RepositoryException("Could not preload repository with hippo node types", ex);
        } catch(InvalidNodeTypeDefException ex) {
          log.error("Could not preload repository with hippo node types: "+ex.getMessage());
        }

        if(!session.getRootNode().hasNode("navigation")) {
          log.info("Loading initial content");
          try {
            InputStream in = getClass().getResourceAsStream("content.xml");
            session.importXML("/", in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
          } catch(IOException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(PathNotFoundException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(ItemExistsException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(ConstraintViolationException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(VersionException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(InvalidSerializedDataException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(LockException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          } catch(RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
          }
          session.save();
        }
    }
    public Server() throws RepositoryException {
        initialize(".");
    }
    public Server(String location) throws RepositoryException {
        if(location.startsWith("rmi://")) {
          try {
            ClientServicesAdapterFactory adapterFactory = new ClientServicesAdapterFactory();
            ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory(adapterFactory);
            repository = repositoryFactory.getRepository(location);
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
        Session session = null;
        try {
          session = login();
          java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
          session.exportSystemView("/navigation", out, false, false);
        } catch(IOException ex) {
          System.err.println(ex.getMessage());
          ex.printStackTrace(System.err);
        } catch(RepositoryException ex) {
          System.err.println(ex.getMessage());
          ex.printStackTrace(System.err);
        } finally {
          if(session != null)
            session.logout();
        }
        try {
          if(rmiRepository != null) {
            rmiRepository = null;
            try {
              Naming.unbind(RMI_NAME);
            } catch(Exception ex) {
              // ignore
            }
          }
          jackrabbitRepository.shutdown();
          jackrabbitRepository = null;
        } catch(Exception ex) {
          // ignore
        }
      }
      repository = null;
    }

    static Registry registry = null;
    public void run(boolean background) throws RemoteException, AlreadyBoundException {
      Runtime.getRuntime().addShutdownHook(new Thread() {
          public void run() {
            close();
          }
        });
      Remote remote = new ServerServicingAdapterFactory().getRemoteRepository(repository);
      System.setProperty("java.rmi.server.useCodebaseOnly", "true");
      if(registry == null)
        registry = LocateRegistry.createRegistry(RMI_PORT);
      registry.bind(RMI_NAME, remote);
      rmiRepository = remote;
      log.info("RMI Server available on rmi://localhost:"+RMI_PORT+"/"+RMI_NAME);
      if(!background) {
        for(;;) {
            try {
              Thread.sleep(333);
            } catch (InterruptedException ex) {
              System.err.println(ex);
            }
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
            server.run(false);
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
