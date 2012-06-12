/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.solr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.hippoecm.hst.component.support.spring.util.MetadataReaderClasspathResourceScanner;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.content.beans.BindingException;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryParser;
import org.hippoecm.hst.solr.content.beans.query.impl.HippoQueryImpl;
import org.hippoecm.hst.util.LazyInitSingletonObjectFactory;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.ObjectFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.springframework.core.io.DefaultResourceLoader;

public class HippoSolrManagerImpl implements HippoSolrManager {


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HippoSolrManagerImpl.class);

    private static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM_ERROR_MSG =
            "Please check HST-2 Content Beans Annotation configuration as servlet context parameter.\n" +
                    "You can set a servlet context parameter named 'hst-beans-annotated-classes' with xml or classes location filter.\n" +
                    "For example, '/WEB-INF/beans-annotated-classes.xml' or 'classpath*:org/examples/beans/**/*.class'";


    /*
    CommonsHttpSolrServer is thread-safe and if you are using the following constructor,
    you *MUST* re-use the same instance for all requests.  If instances are created on
    the fly, it can cause a connection leak. The recommended practice is to keep a
    static instance of CommonsHttpSolrServer per solr server url and share it for all requests.
    See https://issues.apache.org/jira/browse/SOLR-861 for more details
    */

    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
    private String solrUrl = DEFAULT_SOLR_URL;
    private SolrServer solrServer;
    private SolrIndexerThread solrIndexerThread;

    protected long checkInterval = 5000L;
    protected Repository repository;
    protected Credentials credentials;
    protected ObjectConverter objectConverter = null;

    private static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    private List<Class<? extends HippoBean>> annotatedClasses;
    private List<ContentBeanBinder> defaultContentBeanBinders;
    private ObjectFactory<List<ContentBeanBinder>, Object> defaultContentBeanBindersFactory = new LazyInitSingletonObjectFactory<List<ContentBeanBinder>, Object>() {
        @Override
        protected List<ContentBeanBinder> createInstance(Object... args) {
            List<ContentBeanBinder> binders = new ArrayList<ContentBeanBinder>();
            binders.add(new JcrContentBeanBinder());
            return binders;
        }
    };
    
    protected Session session;
    protected boolean stopped = false;
    protected String rootScope = "/content";

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }


    public void start() {
        System.out.println("START SOLR INTEGRATION!!!!!!!!!!!!");
        doInit();
    }

    protected void doInit() {
        objectConverter = getObjectConverter();
        if (objectConverter == null) {
            log.error("Cannot get annotated beans");
            return;
        }

        solrIndexerThread = new SolrIndexerThread();
        solrIndexerThread.start();


    }

    protected void doDeinit() {
        doDeInitSolrServer();
        doDeInitJcrSession();
    }

    protected void doInitSolrServer() throws Exception {
        solrServer = getSolrServer();
        solrServer.ping();

    }

    protected void doDeInitSolrServer() {

    }

    protected void doInitJcrSession() throws RepositoryException {
        if (this.credentials == null) {
            session = this.repository.login();
        } else {
            session = this.repository.login(this.credentials);
        }

    }

    protected void doDeInitJcrSession() {

    }

    public void stop() {
        this.stopped = true;
        doDeinit();
    }


    @Override
    public HippoQuery createQuery(String query) {
        return new HippoQueryImpl(this, query);
    }

    @Override
    public HippoQueryParser getQueryParser() {
        return HippoQueryParser.getInstance();
    }

    @Override
    public List<ContentBeanBinder> getContentBeanBinders() {
        if (defaultContentBeanBinders == null) {
            defaultContentBeanBinders = defaultContentBeanBindersFactory.getInstance();
        }
        return defaultContentBeanBinders;
    }

    public class JcrContentBeanBinder implements ContentBeanBinder {
        List<Class<? extends IdentifiableContentBean>> bindableClasses;


        public JcrContentBeanBinder() {
            this.bindableClasses = new ArrayList<Class<? extends IdentifiableContentBean>>();
            for ( Class<? extends HippoBean> annotatedClass : HippoSolrManagerImpl.this.getAnnotatedClasses()) {
                bindableClasses.add(annotatedClass);
            }
        }
        
        @Override
        public List<Class<? extends IdentifiableContentBean>> getBindableClasses() {
            return bindableClasses; 
        }

        @Override
        // TODO THIS IS NOT ALLOWED TO BE THE CURRENT JCR SESSION!! MUST BE PLUGGED IN!
        public void callbackHandler(final IdentifiableContentBean identifiableContentBean) throws BindingException {
            if (bindableClasses.contains(identifiableContentBean.getClass())) {
                if (identifiableContentBean instanceof HippoBean) {
                    HippoBean bean = (HippoBean) identifiableContentBean;
                    try {
                        Node node = session.getNodeByIdentifier(bean.getIdentifier());
                        // TODO virtualize the node !!!!!
                        bean.setNode(node);
                    } catch (ItemNotFoundException e) {
                        log.warn("Cannot bind '{}' to its backing jcr node because the uuid '{}' does not exist (anymore). Return unbinded bean", bean.getClass().getName(), bean.getIdentifier());
                    } catch (RepositoryException e) {
                        throw new BindingException("RepositoryException during binding to jcr node", e);
                    }
                }
            }
        }
    }
    
    private ObjectConverter getObjectConverter() {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses();
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }

    private List<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = null;

        String ocmAnnotatedClassesResourcePath = "classpath*:**/*.class";
        try {
            if (ocmAnnotatedClassesResourcePath.startsWith("classpath*:")) {
                MetadataReaderClasspathResourceScanner scanner = new MetadataReaderClasspathResourceScanner();
                scanner.setResourceLoader(new DefaultResourceLoader());
                annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(scanner, StringUtils.split(ocmAnnotatedClassesResourcePath, ", \t\r\n"));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return annotatedClasses;
    }


    public SolrServer getSolrServer() throws SolrServerException {
        if (solrServer != null) {
            if (isSolrServerLive()) {
                return solrServer;
            } else {
                throw new SolrServerException("Solr server not available");
            }
        }
        try {
            solrServer = new CommonsHttpSolrServer(solrUrl) {
                private static final long serialVersionUID = 1L;

                @Override
                public org.apache.solr.client.solrj.beans.DocumentObjectBinder getBinder() {
                    return new DocumentObjectBinder();
                }
            };
        } catch (MalformedURLException e) {
            throw new SolrServerException("Malformed solr URL '"+solrUrl+"'");
        }

        if (!isSolrServerLive()) {
            throw new SolrServerException("Solr server not available");
        }

        return solrServer;
    }

    public boolean isSolrServerLive() {
        if (solrServer != null) {
            try {
                solrServer.ping();
                // solrServer up & running & accessible
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    private class SolrIndexerThread extends Thread {

        private boolean firstIndexingCrawlDone = false;

        private SolrIndexerThread() {
            super("SolrAndJcrSessionChecker");
            setDaemon(true);
        }

        public void run() {
            while (!HippoSolrManagerImpl.this.stopped) {

                boolean canIndex = true;
                boolean isSessionLive = false;
                try {
                    if (HippoSolrManagerImpl.this.session != null) {
                        isSessionLive = HippoSolrManagerImpl.this.session.isLive();
                    }
                } catch (Exception e) {
                    log.debug("Exception while checking jcr session: {}", e.toString());
                }

                if (HippoSolrManagerImpl.this.session == null || !isSessionLive) {
                    doDeInitJcrSession();
                    try {
                        doInitJcrSession();
                    } catch (RepositoryException e) {
                        canIndex = false;
                        e.printStackTrace();
                    }
                }

                boolean isSolrServerAvailable = false;
                if (HippoSolrManagerImpl.this.solrServer != null) {
                    try {
                        solrServer.ping();
                        isSolrServerAvailable = true;
                    } catch (Exception e) {
                        log.debug("Exception while checking solr server: {}", e.toString());
                    }
                }

                if (!isSolrServerAvailable) {
                    doDeInitSolrServer();
                    try {
                        doInitSolrServer();
                    } catch (Exception e) {
                        canIndex = false;
                        e.printStackTrace();
                    }
                }
               // firstIndexingCrawlDone = false;
                if (canIndex) {
                    if (!firstIndexingCrawlDone) {
                        try {
                            SolrQuery solrQuery = new SolrQuery();
                            solrQuery.setQuery("*:*");
                            if (solrServer.query(solrQuery).getResults().size()  > 0 ) {
                                firstIndexingCrawlDone = true;
                            } else {
                                solrServer.deleteByQuery("*:*"); // delete everything!
                                solrServer.commit();

                                long start = System.currentTimeMillis();
                                Node jcrNode = session.getNode(rootScope);
                                AutoBeanCommittingAndClearingList<IdentifiableContentBean> beansToIndex =
                                        new AutoBeanCommittingAndClearingList<IdentifiableContentBean>(1000, solrServer);
                                indexDescendantDocuments(jcrNode, beansToIndex);
                                beansToIndex.commit();
                                firstIndexingCrawlDone = true;
                                System.out.println("It took " + (System.currentTimeMillis() - start) + " ms to index " + beansToIndex.totalCommitted+" documents ");
                            }
                        } catch (SolrServerException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (PathNotFoundException e) {
                            e.printStackTrace();
                        } catch (RepositoryException e) {
                            e.printStackTrace();
                        }  catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        // pick up queue
                    }
                }

                synchronized (this) {
                    try {
                        wait(HippoSolrManagerImpl.this.checkInterval);
                    } catch (InterruptedException e) {
                        if (HippoSolrManagerImpl.this.stopped) {
                            break;
                        }
                    }
                }
            }
        }

        private void indexDescendantDocuments(Node node, List<IdentifiableContentBean> beansToIndex) {
            try {
                NodeIterator childNodes = node.getNodes();
                while (childNodes.hasNext()) {
                    Node child = childNodes.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_HARDHANDLE)) {
                        // fetch the beans for all the documents that are of type hippo:harddocument and index them
                        NodeIterator documents = child.getNodes();
                        while (documents.hasNext()) {
                            Node doc = documents.nextNode();
                            if (doc.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                                try {
                                    HippoDocumentBean documentBean = (HippoDocumentBean) objectConverter.getObject(doc);
                                    beansToIndex.add(documentBean);
                                } catch (ObjectBeanManagerException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (child.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                        indexDescendantDocuments(child, beansToIndex);
                    }
                }
            } catch (RepositoryException e) {
                e.printStackTrace();
            }

        }


    }


    private class AutoBeanCommittingAndClearingList<E extends IdentifiableContentBean> extends ArrayList<E> {

        private static final long serialVersionUID = 1L;

        private int limit;
        private SolrServer server;
        private int totalCommitted;

        AutoBeanCommittingAndClearingList(int limit, SolrServer server) {
            this.limit = limit;
            this.server = server;
        }

        @Override
        public boolean add(E e) {
            boolean b = super.add(e);
            ifFullCommit();
            return b;
        }

        @Override
        public void add(int index,E element) {
            super.add(index, element);
            ifFullCommit();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean b = super.addAll(c);
            ifFullCommit();
            return b;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            boolean b = super.addAll(index, c);
            ifFullCommit();
            return b;
        }

        private void ifFullCommit() {
            if (size() >= limit) {
                commit();
            }
        }

        private void commit() {
            try {
                server.addBeans(this);
                server.commit();
                totalCommitted += this.size();
                System.out.println("totalCommitted = " + totalCommitted);
                this.clear();
            } catch (SolrServerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}