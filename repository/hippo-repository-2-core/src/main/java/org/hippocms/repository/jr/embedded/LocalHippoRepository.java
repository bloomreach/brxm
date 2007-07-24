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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.hippocms.repository.jr.servicing.ServicingDecoratorFactory;

class LocalHippoRepository extends HippoRepository {
    private final static String SVN_ID = "$Id$";

    public final static String NS_URI = "http://www.hippocms.org/nt/1.0";
    public final static String NS_PREFIX = "hippo";
    private JackrabbitRepository jackrabbitRepository = null;
    private ServicingDecoratorFactory hippoRepositoryFactory;

    public LocalHippoRepository() throws RepositoryException {
        super();
        initialize();
    }

    public LocalHippoRepository(String location) throws RepositoryException {
        super(location);
        initialize();
    }

    protected String getLocation() {
        // TODO Auto-generated method stub
        return super.getLocation();
    }
    
    protected Object clone() throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return super.clone();
    }
    
    /**
     * Construct the repository path, default getWorkingDirectory() is used.
     * If the system property repo.path can be used to override the default.
     * If repo.path starts with a '.' then the path is taken relative to the
     * getWorkingDirectory().
     * @return The absolute path to the file repository
     */
    private String getRepositoryPath() {
        String path = System.getProperty("repo.path");
        
        if (path == null || "".equals(path)) {
            path = getWorkingDirectory();
        } else if (path.charAt(0) == '.') {
            // relative path
            path = getWorkingDirectory() + System.getProperty("file.separator") + path;
        }
        return path;
    }
    
    private void initialize() throws RepositoryException {
        InputStream config = getClass().getResourceAsStream("repository.xml");
        jackrabbitRepository = RepositoryImpl.create(RepositoryConfig.create(config, getRepositoryPath()));
        repository = jackrabbitRepository;

        String result = repository.getDescriptor("OPTION_NODE_TYPE_REG_SUPPORTED");
        log.info("Node type registration support: " + (result != null ? result : "no"));

        hippoRepositoryFactory = new ServicingDecoratorFactory();
        repository = hippoRepositoryFactory.getRepositoryDecorator(repository);

        Session session = login();

        try {
            Workspace workspace = session.getWorkspace();

            NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
            try {
                nsreg.registerNamespace(NS_PREFIX, NS_URI);
            } catch (javax.jcr.NamespaceException ex) {
                log.warn(ex.getMessage());
            }
            createNodeTypesFromFile(workspace, "repository.cnd");
            createNodeTypesFromFile(workspace, "newsmodel.cnd");
            session.save();

        } catch (ParseException ex) {
            throw new RepositoryException("Could not preload repository with hippo node types", ex);
        } catch (InvalidNodeTypeDefException ex) {
            log.warn("Could not preload repository with hippo node types: " + ex.getMessage());
        }

        if (!session.getRootNode().hasNode("navigation")) {
            log.info("Loading initial content");
            try {
                InputStream in = getClass().getResourceAsStream("content.xml");
                session.importXML("/", in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (PathNotFoundException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (ItemExistsException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (ConstraintViolationException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (VersionException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (InvalidSerializedDataException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (LockException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
            session.save();
        }
    }

    private void createNodeTypesFromFile(Workspace workspace, String cndName) throws ParseException, RepositoryException, InvalidNodeTypeDefException {

        log.info("Loading initial nodeTypes from: " + cndName);
        
        InputStream cndStream = getClass().getResourceAsStream(cndName);
        BufferedReader cndInput = new BufferedReader(new InputStreamReader(cndStream));
        CompactNodeTypeDefReader cndReader = new CompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName);
        List ntdList = cndReader.getNodeTypeDefs();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
        /*
        boolean progress;
        do {
            progress = false;
            for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
                NodeTypeDef ntd = (NodeTypeDef) iter.next();
                try {
                    ntreg.unregisterNodeType(ntd.getName());
                    progress = true;
                } catch (RepositoryException ex) {
                    // save to ignore
                }
            }
        } while (progress);
        */
        
        for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            try {
                ntreg.unregisterNodeType(ntd.getName());
            } catch (RepositoryException ex) {
                // save to ignore
            }
            try {
                try {
                    EffectiveNodeType effnt = ntreg.registerNodeType(ntd);
                } catch (NamespaceException ex) {
                    log.warn(ex.getMessage());
                    System.err.println(ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            } catch (RepositoryException ex) {
                if (ex.getMessage().equals("not yet implemented")) {
                    log.warn("cannot override typing; hoping they are equivalent");
                } else
                    throw ex;
            }
        }
    }

    public synchronized void close() {
        Session session = null;
        if (repository != null) {
            try {
                session = login();
                java.io.OutputStream out = new java.io.FileOutputStream("dump.xml");
                session.exportSystemView("/navigation", out, false, false);
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            } finally {
                if (session != null)
                    session.logout();
            }
        }

        if (jackrabbitRepository != null) {
            try {
                jackrabbitRepository.shutdown();
                jackrabbitRepository = null;
            } catch (Exception ex) {
                // ignore
            }
        }
        repository = null;

        super.close();
    }
}
