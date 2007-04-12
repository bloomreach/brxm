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

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class Server {
    
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private String workingDir;

    
    public Server(String workingDirectory) throws RepositoryException {
       workingDir = new File(workingDirectory).getAbsolutePath();
    }
    
    
    public JackrabbitRepository startUp() throws RepositoryException {
        InputStream config = getClass().getResourceAsStream("repository.xml");
        JackrabbitRepository repository = RepositoryImpl.create(RepositoryConfig.create(config, workingDir));
        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));
        
        return repository;
    }
    
    
    public Session login(JackrabbitRepository repository) throws RepositoryException {
        Session result = repository.login(new SimpleCredentials("username", "password".toCharArray()));
        log.info("Logged in as " + result.getUserID() + " to a " + repository.getDescriptor(Repository.REP_NAME_DESC)
                + " repository.");
        
        return result;
    }
    
    
    public void shutDown(JackrabbitRepository repository) {
        repository.shutdown();
    }
    

    public static void main(String[] args) {
        try {
            String defaultWorkdir = System.getProperty("user.dir") + System.getProperty("file.separator") + "work";
            
            Server server = new Server(args.length > 0 ? args[0] : defaultWorkdir);
            JackrabbitRepository repository  = server.startUp();
            Session session = server.login(repository);
            // do something
            server.shutDown(repository);

        } catch (RepositoryException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
