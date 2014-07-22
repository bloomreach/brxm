/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.content.beans.BindingException;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryParser;
import org.hippoecm.hst.solr.content.beans.query.impl.HippoQueryImpl;

public class HippoSolrClientImpl implements HippoSolrClient {


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HippoSolrClientImpl.class);


    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
    private String solrUrl = DEFAULT_SOLR_URL;
    private SolrServer solrServer;

    private volatile List<ContentBeanBinder> defaultContentBeanBinders;
    private DocumentObjectBinder documentObjectBinder;


    @SuppressWarnings("UnusedDeclaration")
    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDocumentObjectBinder(final DocumentObjectBinder documentObjectBinder) {
        this.documentObjectBinder = documentObjectBinder;
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
        public boolean canBind(final Class<? extends IdentifiableContentBean> clazz) {
            return (HippoBean.class.isAssignableFrom(clazz));
        }

        @Override
        public void bind(final IdentifiableContentBean identifiableContentBean) throws BindingException {
            if (!(identifiableContentBean instanceof HippoBean)) {
                log.warn("Cannot bind '{}' to a jcr node because the bean is not of (sub)type HippoBean. Return unbinded bean", identifiableContentBean.getClass().getName(), identifiableContentBean.getIdentifier());
                return;
            }
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
    
    public SolrServer getSolrServer() throws SolrServerException {
        if (solrServer != null) {
            if (isSolrServerLive()) {
                return solrServer;
            } else {
                throw new SolrServerException("Solr server not available");
            }
        }

        solrServer = new HttpSolrServer(solrUrl) {
            private static final long serialVersionUID = 1L;

            @Override
            public org.apache.solr.client.solrj.beans.DocumentObjectBinder getBinder() {
                if (documentObjectBinder == null) {
                    return new DocumentObjectBinder();
                }
                return documentObjectBinder;
            }
        };

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