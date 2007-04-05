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
 * distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package org.hippocms.repository.jr.standalone;

import java.io.File;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class Server 
{
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    
    private JackrabbitRepository repo;
    private Remote rmiRepo;
    
    public static final int RMI_PORT = 1099;
    public static final String RMI_NAME = "jr-standalone";
    
    public static void main( String[] args ) throws Exception
    {
        String workingDirPath = ".";
        if ( args.length > 0 ) {
            System.out.println("par[0] "+args[0]);
            workingDirPath = args[0];            
        }
        new Server().run(new File(workingDirPath).getAbsolutePath());
    }
    
    public void run(String workingDir) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {                
                if ( repo != null ) {
                    try {
                        repo.shutdown();
                    }
                    catch (Exception e) {
                        // ignore
                    }                    
                    repo = null;
                }
                if ( rmiRepo != null ) {
                    rmiRepo = null;
                    try {
                        Naming.unbind(RMI_NAME);
                    }
                    catch (Exception e) {
                        // ignore
                    }                    
                }
            }
        });
        InputStream config = getClass().getResourceAsStream("repository.xml");
        log.info("running from "+workingDir);
        
        repo = RepositoryImpl.create(RepositoryConfig.create(config, workingDir));
        Remote remote = new ServerAdapterFactory().getRemoteRepository(repo);
        System.setProperty("java.rmi.server.useCodebaseOnly", "true");
        Registry reg = LocateRegistry.createRegistry(RMI_PORT);
        reg.bind(RMI_NAME, remote);
        rmiRepo = remote;
        log.info("RMI Server available on rmi://localhost:"+RMI_PORT+"/"+RMI_NAME);
        while (true) ;
    }
}
