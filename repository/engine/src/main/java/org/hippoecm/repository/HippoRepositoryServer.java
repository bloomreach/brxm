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
package org.hippoecm.repository;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.decorating.server.ServerServicingAdapterFactory;

public class HippoRepositoryServer extends LocalHippoRepository {

    private boolean registryIsEmbedded = false;
    String bindingAddress;
    static Registry registry = null;
    private Remote rmiRepository;

    public HippoRepositoryServer() throws RepositoryException {
        initialize();
    }

    public HippoRepositoryServer(String location) throws RepositoryException {
        this(location, null);
    }

    public HippoRepositoryServer(String location, String bindingAddress) throws RepositoryException {
        super(location);
        this.bindingAddress = bindingAddress;
        initialize();
    }

    @Override
    public void close() {
        super.close();
        
        // unbinding from registry
        String name = null;
        try {
            name = new RepositoryUrl(bindingAddress).getName();
            log.info("Unbinding '"+name+"' from registry.");
            if(registry != null) {
                // An alternate would be to use: Naming.unbind(name); which also handles Context based Naming
                registry.unbind(name);
            }
        } catch (RemoteException ex) {
            log.info("Error during unbinding '" + name + "': " + ex.getMessage());
        } catch (NotBoundException ex) {
            log.info("Error during unbinding '" + name + "': " + ex.getMessage());
        } catch (MalformedURLException ex) {
            log.info("MalformedURLException while parsing '" + bindingAddress + "': " + ex.getMessage());
        }

        // unexporting from registry
        try {
            log.info("Unexporting rmi repository: " + bindingAddress);
            UnicastRemoteObject.unexportObject(rmiRepository, true);
        } catch (NoSuchObjectException ex) {
            log.info("Error during rmi shutdown for address: " + bindingAddress, ex);
        }

        // shutdown registry
        if (registryIsEmbedded) {
            try {
                log.info("Closing rmiregistry: " + bindingAddress);
                UnicastRemoteObject.unexportObject(registry, true);
            } catch (NoSuchObjectException ex) {
                log.info("Error during rmi shutdown for address: " + bindingAddress, ex);
            } finally {
                registryIsEmbedded = false;
                registry = null;
            }
        }

        // force the distributed GC to fire, otherwise in tomcat with embedded
        // rmi registry the process won't end, this procedure is deliberately
        // different from the RepositoryServlet, as this class will be used in
        // a different environment, where the server isn't necessarily shut down.
        System.gc();
    }

    public void run(boolean background) throws RemoteException, AlreadyBoundException, MalformedURLException {
        run(null, background);
    }

    public void run(String name, boolean background) throws RemoteException, AlreadyBoundException, MalformedURLException {
        // the the remote repository
        RepositoryUrl url = new RepositoryUrl(bindingAddress);
        rmiRepository = new ServerServicingAdapterFactory(url).getRemoteRepository(repository);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                close();
            }
        });
        System.setProperty("java.rmi.server.useCodebaseOnly", "true");

        // Get or start registry and bind the remote repository
        try {
            registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            registry.rebind(url.getName(), rmiRepository); // connection exception happens here
            log.info("Using existing RMI registry on " + url.getHost() + ":" + url.getPort());
        } catch (NoSuchObjectException ex) {
            registry = LocateRegistry.createRegistry(url.getPort());
            registry.rebind(url.getName(), rmiRepository);
            log.info("Started RMI registry on port " + url.getPort());
            registryIsEmbedded = true;
        } catch (ConnectException ex) {
            registry = LocateRegistry.createRegistry(url.getPort());
            registry.rebind(url.getName(), rmiRepository);
            log.info("Started RMI registry on port " + url.getPort());
            registryIsEmbedded = true;
        }
        log.info("RMI Server available on " + name);
        if (!background) {
            for (;;) {
                try {
                    Thread.sleep(333);
                } catch (InterruptedException ex) {
                    if (log.isDebugEnabled()) {
                        log.debug("Interrupted option", ex);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            HippoRepositoryServer server = null;
            if (args.length > 0) {
                server = new HippoRepositoryServer(args.length > 0 ? args[0] : ".");
            } else {
                server = new HippoRepositoryServer();
            }
            if (args.length > 1) {
                server.run(args[1], false);
            } else {
                server.run(false);
            }
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
        } catch (MalformedURLException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
