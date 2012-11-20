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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.content.beans.BindingException;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryParser;
import org.hippoecm.hst.solr.content.beans.query.impl.HippoQueryImpl;

public class HippoSolrManagerImpl implements HippoSolrManager {


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HippoSolrManagerImpl.class);


    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
    private String solrUrl = DEFAULT_SOLR_URL;
    private SolrServer solrServer;

    private volatile List<ContentBeanBinder> defaultContentBeanBinders;

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
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
        if (defaultContentBeanBinders != null) {
            return defaultContentBeanBinders;
        }
        defaultContentBeanBinders = new ArrayList<ContentBeanBinder>();
        defaultContentBeanBinders.add(new JcrContentBeanBinder());
        return defaultContentBeanBinders;
    }

    public class JcrContentBeanBinder implements ContentBeanBinder {

        @Override
        public List<Class<? extends IdentifiableContentBean>> getBindableClasses() {
            return Collections.emptyList();
        }

        @Override
        public void callbackHandler(final IdentifiableContentBean identifiableContentBean) throws BindingException {
            if (identifiableContentBean instanceof HippoBean) {
                HippoBean bean = (HippoBean) identifiableContentBean;
                if (RequestContextProvider.get() == null) {
                    log.warn("Cannot bind '{}' to its backing jcr node because there is no hst request context. Return unbinded bean", bean.getClass().getName(), bean.getIdentifier());
                }
                try {
                    Node node = RequestContextProvider.get().getSession().getNodeByIdentifier(bean.getIdentifier());
                    bean.setNode(node);
                } catch (ItemNotFoundException e) {
                    log.warn("Cannot bind '{}' to its backing jcr node because the uuid '{}' does not exist (anymore). Return unbinded bean", bean.getClass().getName(), bean.getIdentifier());
                } catch (RepositoryException e) {
                    throw new BindingException("RepositoryException during binding to jcr node", e);
                }
            }
        }
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

            /*
            CommonsHttpSolrServer is thread-safe and if you are using the following constructor,
            you *MUST* re-use the same instance for all requests.  If instances are created on
            the fly, it can cause a connection leak. The recommended practice is to keep a
            static instance of CommonsHttpSolrServer per solr server url and share it for all requests.
            See https://issues.apache.org/jira/browse/SOLR-861 for more details
            */

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

}