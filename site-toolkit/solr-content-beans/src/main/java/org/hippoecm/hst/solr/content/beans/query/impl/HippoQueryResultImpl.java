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
package org.hippoecm.hst.solr.content.beans.query.impl;

import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.HippoSolrManager;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;
import org.hippoecm.hst.solr.content.beans.query.HitIterator;

public class HippoQueryResultImpl implements HippoQueryResult {

    private final QueryResponse queryResponse;
    private final SolrDocumentList docs;
    private final DocumentObjectBinder binder;

    // HippoSolrManager and ContentBeanBinder are not serializable hence transient
    private final transient HippoSolrManager manager;
    private transient List<ContentBeanBinder> contentBeanBinders;

    public HippoQueryResultImpl(final QueryResponse queryResponse, final SolrDocumentList docs, final DocumentObjectBinder binder,
                                final HippoSolrManager manager) {
        this.queryResponse = queryResponse;
        this.docs = docs;
        this.binder = binder;
        this.manager = manager;
    }


    @Override
    public int getSize() {
        if (docs.getNumFound() > Integer.MAX_VALUE) {
            throw new IllegalStateException(" Integer.MAX_VALUE hits is the maximum size");
        }
        return (int)docs.getNumFound();
    }

    @Override
    public QueryResponse getQueryResponse() {
        return queryResponse;
    }

    @Override
    public SolrDocumentList getDocs() {
        return docs;
    }

    @Override
    public HitIterator<IdentifiableContentBean> getHits() {
        return new HitIteratorImpl(queryResponse, docs, binder, contentBeanBinders);
    }

    @Override
    public void bindHits() {
        this.contentBeanBinders = manager.getContentBeanBinders();
    }

    @Override
    public void bindHits(final List<ContentBeanBinder> binders) {
        this.contentBeanBinders = binders;
    }

}
