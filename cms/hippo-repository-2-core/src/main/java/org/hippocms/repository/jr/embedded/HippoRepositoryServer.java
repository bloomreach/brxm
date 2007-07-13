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
import java.util.Properties;

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
import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

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

import org.hippocms.repository.jr.servicing.Service;
import org.hippocms.repository.jr.servicing.ServicingDecoratorFactory;
import org.hippocms.repository.jr.servicing.client.ClientServicesAdapterFactory;
import org.hippocms.repository.jr.servicing.server.ServerServicingAdapterFactory;

public class HippoRepositoryServer extends LocalHippoRepository {
    public static int RMI_PORT = 1099;
    public static String RMI_NAME = "jackrabbit.repository";

    static Registry registry = null;
    private Remote rmiRepository;

    public HippoRepositoryServer() throws RepositoryException {
        super();
    }

    public HippoRepositoryServer(String location) throws RepositoryException {
        super(location);
    }

    public void close() {
        if (rmiRepository != null) {
            rmiRepository = null;
            try {
                Naming.unbind(RMI_NAME);
            } catch (Exception ex) {
                // ignore
            }
        }
        super.close();
    }

    public void run(boolean background) throws RemoteException, AlreadyBoundException {
        run(null, background);
    }

    public void run(String name, boolean background) throws RemoteException, AlreadyBoundException {
        if (name == null || name.equals(""))
            name = "rmi://localhost:1099/jackrabbit.repository";
        String host = null;
        int port = 0;
        if (name.startsWith("rmi://")) {
            if (name.indexOf('/', 6) >= 0) {
                if (name.indexOf(':', 6) >= 0 && name.indexOf(':', 6) < name.indexOf('/', 6)) {
                    port = Integer.parseInt(name.substring(name.indexOf(':', 6) + 1, name.indexOf('/', name.indexOf(
                            ':', 6) + 1)));
                    host = name.substring(6, name.indexOf(':', 6));
                } else
                    host = name.substring(6, name.indexOf('/', 6));
                name = name.substring(name.indexOf('/', 6) + 1);
            } else
                name = name.substring(6);
        }
        if (registry == null) {
            if (host != null) {
                if (port > 0)
                    registry = LocateRegistry.getRegistry(host, port);
                else
                    registry = LocateRegistry.getRegistry(host);
            } else {
                if (port > 0)
                    registry = LocateRegistry.getRegistry(port);
                else
                    registry = LocateRegistry.getRegistry();
            }
            if (log.isDebugEnabled())
                log.debug("Using rmiregistry on " + host + " port " + port);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                close();
            }
        });
        Remote remote = new ServerServicingAdapterFactory().getRemoteRepository(repository);
        System.setProperty("java.rmi.server.useCodebaseOnly", "true");

        try {
            registry.bind(name, remote);
        } catch (ConnectException ex) {
            registry = LocateRegistry.createRegistry(port > 0 ? port : 1099);
            log.info("Started embedded rmiregistry " + (host != null ? "on " + host : "")
                    + (port > 0 ? " port " + port : ""));
            registry.bind(name, remote);
        }
        rmiRepository = remote;
        log.info("RMI Server available on " + name);
        if (!background) {
            for (;;) {
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
            HippoRepositoryServer server = null;
            if (args.length > 0)
                server = new HippoRepositoryServer(args.length > 0 ? args[0] : ".");
            else
                server = new HippoRepositoryServer();
            server.run(false);
            server.close();
        } catch (RemoteException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (AlreadyBoundException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
