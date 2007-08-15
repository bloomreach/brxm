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
package org.hippoecm.repository.jr.embedded;

import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.jr.servicing.server.ServerServicingAdapterFactory;

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
	    if(args.length > 1)
              server.run(args[1], false);
	    else
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
