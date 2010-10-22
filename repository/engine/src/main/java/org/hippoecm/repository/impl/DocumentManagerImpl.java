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
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jdo.JDOHelper;
import javax.jdo.JDODataStoreException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jpox.PersistenceManagerFactoryImpl;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.ocm.JCROID;
import org.hippoecm.repository.ocm.StoreManagerImpl;

public class DocumentManagerImpl implements DocumentManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);

    Session session;
    Node configuration;
    PersistenceManagerFactory pmf;
    StoreManagerImpl sm;
    PersistenceManager pm;
    private ClassLoader loader;

    public DocumentManagerImpl(Session session) {
        this.session = session;
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" +
                                                          HippoNodeType.DOCUMENTS_PATH);
        } catch(RepositoryException ex) {
            log.error("document manager configuration failed: "+ex.getMessage());
        }
        try {
            Properties properties = new Properties();
            InputStream istream = getClass().getClassLoader().getResourceAsStream("jdo.properties");
            properties.load(istream);
            properties.setProperty("javax.jdo.option.ConnectionURL", "jcr:file:" + System.getProperty("user.dir"));

            pmf = JDOHelper.getPersistenceManagerFactory(properties);
            pm = null;
            sm = (StoreManagerImpl) ((PersistenceManagerFactoryImpl)pmf).getOMFContext().getStoreManager();
            sm.setSession(session);
        } catch(IOException ex) {
            log.error("failed to initialize JDO layer: "+ex.getMessage());
        }
    }

    void reset() {
        if(pm != null) {
            pm.evictAll();
        }
    }

    public Session getSession() {
        return session;
    }

    public Object getObject(String uuid, String classname, Node types) {
        Object obj = null;
        if(pm == null) {
            pm = pmf.getPersistenceManager();
        }
        if(types != null) {
            sm.setTypes(types);
        }

        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (loader != null) {
                Thread.currentThread().setContextClassLoader(loader);
            }
            obj = pm.getObjectById(new JCROID(uuid, classname));
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            sm.setTypes(null);
            if (loader != null) {
                Thread.currentThread().setContextClassLoader(oldLoader);
            }
        }
        return obj;
    }

    public void putObject(String uuid, Node types, Object object) throws RepositoryException {
        if(pm == null) {
            pm = pmf.getPersistenceManager();
        }
        if(types != null) {
            sm.setTypes(types);
        }
        Transaction tx = pm.currentTransaction();
        tx.setNontransactionalRead(true);
        tx.setNontransactionalWrite(true);
        boolean transactional = true;
        if(transactional && !tx.isActive()) {
            try {
                tx.begin();
                if(uuid != null) {
                    Node node = session.getNodeByUUID(uuid);
                    if(!node.isCheckedOut()) {
                        if(node.isNodeType("mix:versionable")) {
                            node.checkout();
                        }
                        try {
                            Node parent = node.getParent();
                            if(parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                if(!parent.isCheckedOut()) {
                                    parent.checkout();
                                }
                            }
                        } catch(ItemNotFoundException ex)  {
                            // no parent as this is root node, ignore.
                        }
                    }
                }
                pm.makePersistent(object);
                tx.commit();
            } catch(JDODataStoreException ex) {
                Throwable e = ex.getCause();
                if(e instanceof RepositoryException) {
                    RepositoryException exception = (RepositoryException) e;
                    log.error(exception.getClass().getName()+": "+exception.getMessage(), exception);
                    throw exception;
                } else {
                    throw ex;
                }
            } finally {
                if(tx.isActive())
                    tx.rollback();
            }
        } else {
            pm.makePersistent(object);
        }
    }

    public Document getDocument(String category, String identifier) throws MappingException, RepositoryException {
        try {
            Node queryNode = configuration.getNode(category);
            HippoQuery query = (HippoQuery) session.getWorkspace().getQueryManager().getQuery(queryNode);
            QueryResult result;
            if(query.getArgumentCount() > 0) {
                Map<String,String> arguments = new TreeMap<String,String>();
                String[] queryArguments = query.getArguments();
                for(int i=0; i<queryArguments.length; i++) {
                    arguments.put(queryArguments[i], identifier);
                }
                result = query.execute(arguments);
            } else {
                result = query.execute();
            }
            NodeIterator iter = result.getNodes();
            if (iter.hasNext()) {
                Node resultNode = iter.nextNode();
                String uuid = resultNode.getUUID();
                if(queryNode.isNodeType(HippoNodeType.NT_OCMQUERY) || queryNode.isNodeType(HippoNodeType.NT_WORKFLOW)) {
                    return (Document) getObject(uuid, queryNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString(),
                                                queryNode.getNode(HippoNodeType.NT_TYPES));
                } else {
                    return new Document(uuid);
                }
            } else {
                return null;
            }
        } catch(javax.jdo.JDODataStoreException ex) {
            log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new MappingException("Representing JCR data to Java object failed", ex);
        } catch(PathNotFoundException ex) {
            log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            /* getDocument cannot and should not be used to create documents.
             * null is a valid way to check whether the document looked for exist,
             * as this is the only way for e.g. Workflow plugins to lookup
             * documents.
             */
            return null;
        }
    }
}
