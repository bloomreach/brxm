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
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jdo.JDODataStoreException;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.datanucleus.OMFContext;
import org.datanucleus.jdo.JDOPersistenceManagerFactory;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.ocm.JcrOID;
import org.hippoecm.repository.ocm.JcrPersistenceHandler;
import org.hippoecm.repository.ocm.JcrStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentManagerImpl implements DocumentManager, HippoSession.CloseCallback {

    private final Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);

    private Session session;
    private Node configuration;
    private PersistenceManagerFactory pmf;
    private PersistenceManager pm;
    private JcrPersistenceHandler ph;
    private JcrStoreManager storeManager;

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
            ((JcrStoreManager)((JDOPersistenceManagerFactory)pmf).getOMFContext().getStoreManager()).setSession(session);
            ((JDOPersistenceManagerFactory)pmf).setPrimaryClassLoader(getClass().getClassLoader());
            pm = null;
            ph = (JcrPersistenceHandler) ((JDOPersistenceManagerFactory)pmf).getOMFContext().getStoreManager().getPersistenceHandler();
        } catch(IOException ex) {
            log.error("failed to initialize JDO layer: "+ex.getMessage(), ex);
        }
        if (session instanceof HippoSession) {
            ((HippoSession)session).registerSessionCloseCallback(this);
        }
    }

    public void close() {
        if (ph != null) {
            ph.close();
            ph = null;
        }
        if (pm != null) {
            pm.evictAll();
            pm.close();
            pm = null;
        }
        if (pmf != null) {
            pmf.getDataStoreCache().evictAll();
            pmf.close();
            pmf = null;
        }
    }

    void reset() {
        if (pm != null) {
            pmf.getDataStoreCache().evictAll();
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
            OMFContext omfContext = ((JDOPersistenceManagerFactory)pmf).getOMFContext();
            storeManager = ((JcrStoreManager)omfContext.getStoreManager());
            storeManager.setSession(session); // FIXME is storeManger really per session?
        }
        if(types != null) {
            storeManager.setTypes(types);
        }

        try {
            obj = pm.getObjectById(new JcrOID(uuid, classname));
        } catch(Exception ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } finally {
            storeManager.setTypes(null);
        }
        return obj;
    }

    public <T> T getDocument(String uuid, Class<T> clazz) {
        return (T) getObject(uuid, clazz.getName(), null);
    }
    
    public void putDocument(Document document) throws RepositoryException {
        putObject(document.getIdentity(), null, document);
    }
    
    public void putObject(String uuid, Node types, Object object) throws RepositoryException {
        try {
            if (pm == null) {
                pm = pmf.getPersistenceManager();
            }
            if (types != null) {
                storeManager.setTypes(types);
            }

            Transaction tx = pm.currentTransaction();
            tx.setNontransactionalRead(true);
            tx.setNontransactionalWrite(true);
            boolean transactional = true;
            if (transactional && !tx.isActive()) {
                try {
                    tx.begin();
                    if (uuid != null) {
                        Node node = session.getNodeByUUID(uuid);
                        if (!node.isCheckedOut()) {
                            if (node.isNodeType("mix:versionable")) {
                                node.checkout();
                            }
                            try {
                                Node parent = node.getParent();
                                if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    if (!parent.isCheckedOut()) {
                                        parent.checkout();
                                    }
                                }
                            } catch (ItemNotFoundException ex) {
                                // no parent as this is root node, ignore.
                            }
                        }
                    }
                    pm.makePersistent(object);
                    tx.commit();
                } catch (JDODataStoreException ex) {
                    Throwable e = ex.getCause();
                    if (e instanceof RepositoryException) {
                        RepositoryException exception = (RepositoryException)e;
                        log.error(exception.getClass().getName() + ": " + exception.getMessage(), exception);
                        throw exception;
                    } else {
                        throw ex;
                    }
                } finally {
                    if (tx.isActive())
                        tx.rollback();
                }
            } else {
                pm.makePersistent(object);
            }
        } finally {
            storeManager.setTypes(null);
        }
}

    public Document getDocument(String category, String identifier) throws MappingException, RepositoryException {
        try {
            Node queryNode = configuration.getNode(category);
            QueryResult result;
            Query query = session.getWorkspace().getQueryManager().getQuery(queryNode);
            if (query instanceof HippoQuery) {
                HippoQuery hippoQuery = (HippoQuery)session.getWorkspace().getQueryManager().getQuery(queryNode);
                if (hippoQuery.getArgumentCount() > 0) {
                    Map<String, String> arguments = new TreeMap<String, String>();
                    String[] queryArguments = hippoQuery.getArguments();
                    for (int i = 0; i < queryArguments.length; i++) {
                        arguments.put(queryArguments[i], identifier);
                    }
                    result = hippoQuery.execute(arguments);
                } else {
                    result = hippoQuery.execute();
                }
            } else {
                String[] bindVariableNames = query.getBindVariableNames();
                for (int i = 0; bindVariableNames != null && i < bindVariableNames.length; i++) {
                    query.bindValue(bindVariableNames[i], session.getValueFactory().createValue(identifier));
                }
                result = query.execute();
            }
            RowIterator iter = result.getRows();
            String selectorName = (result.getSelectorNames().length > 1 ? result.getSelectorNames()[result.getSelectorNames().length - 1] : null);
            if (iter.hasNext()) {
                Node resultNode = null;
                while (iter.hasNext()) {
                    Row resultRow = iter.nextRow();
                    Node node = (selectorName != null ? resultRow.getNode(selectorName) : resultRow.getNode());
                    if (node != null) {
                        if (resultNode == null || node.getPath().length() > resultNode.getPath().length())
                            resultNode = node;
                    }
                }
                String uuid = resultNode.getUUID();
                if(queryNode.isNodeType(HippoNodeType.NT_OCMQUERY) || queryNode.isNodeType(HippoNodeType.NT_WORKFLOW)) {
                    reset();
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
