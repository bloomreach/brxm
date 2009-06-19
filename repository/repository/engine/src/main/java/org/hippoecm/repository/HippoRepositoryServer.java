/*
 *  Copyright 2008 Hippo.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.Naming;
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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public final static int RMI_PORT = 1099;
    public final static String RMI_HOST = "localhost";
    public final static String RMI_NAME = "hipporepository";

    private boolean registryIsEmbedded = false;
    String bindingAddress;
    static Registry registry = null;
    private Remote rmiRepository;

    public HippoRepositoryServer() throws RepositoryException {
        super();
    }

    public HippoRepositoryServer(String location) throws RepositoryException {
        super(location);
    }

    @Override
    public void close() {
        super.close();
        
        // unbinding from registry
        String name = null;
        try {
            name = new RepositoryRmiUrl(bindingAddress).getName();
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
    }

    public void run(boolean background) throws RemoteException, AlreadyBoundException, MalformedURLException {
        run(null, background);
    }

    public void run(String name, boolean background) throws RemoteException, AlreadyBoundException, MalformedURLException {
        if (name == null || name.equals("")) {
            bindingAddress = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/" + RMI_NAME;
        } else {
            bindingAddress = name;
        }

        // the the remote repository
        RepositoryRmiUrl url = new RepositoryRmiUrl(bindingAddress);
        rmiRepository = new ServerServicingAdapterFactory().getRemoteRepository(repository);

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


    private class RepositoryRmiUrl {
        // defaults
        public final static String DEFAULT_RMI_NAME = "hipporepository";
        public final static String RMI_PREFIX = "rmi";
        private String name;
        private String host;
        private int port;

        RepositoryRmiUrl(String str) throws MalformedURLException {
            try {
                URI uri = new URI(str);
                if (uri.getFragment() != null) {
                    throw new MalformedURLException("invalid character, '#', in URL name: " + str);
                } else if (uri.getQuery() != null) {
                    throw new MalformedURLException("invalid character, '?', in URL name: " + str);
                } else if (uri.getUserInfo() != null) {
                    throw new MalformedURLException("invalid character, '@', in URL host: " + str);
                }
                String scheme = uri.getScheme();
                if (scheme != null && !scheme.equals(RMI_PREFIX)) {
                    throw new MalformedURLException("invalid URL scheme: " + str);
                }

                name = uri.getPath();
                if (name != null) {
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }
                    if (name.length() == 0) {
                        name = DEFAULT_RMI_NAME;
                    }
                }

                host = uri.getHost();
                if (host == null) {
                    host = "";
                    if (uri.getPort() == -1) {
                        /* handle URIs with explicit port but no host
                         * (e.g., "//:1098/foo"); although they do not strictly
                         * conform to RFC 2396, Naming's javadoc explicitly allows
                         * them.
                         */
                        String authority = uri.getAuthority();
                        if (authority != null && authority.startsWith(":")) {
                            authority = "localhost" + authority;
                            uri = new URI(null, authority, null, null, null);
                        }
                    }
                }
                port = uri.getPort();
                if (port == -1) {
                    port = Registry.REGISTRY_PORT;
                }
            } catch (URISyntaxException ex) {
                throw (MalformedURLException) new MalformedURLException("invalid URL string: " + str).initCause(ex);
            }
        }

        @Override
        public String toString() {
            return RMI_PREFIX + host + ":" + port + "/" + name;
        }

        public int getPort() {
            return port;
        }

        public String getName() {
            return name;
        }

        public String getHost() {
            return host;
        }
    }
}
