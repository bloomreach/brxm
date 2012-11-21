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
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.HippoSolrClient;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;
import org.hippoecm.hst.solr.content.beans.query.HitIterator;

public class HippoQueryResultImpl implements HippoQueryResult {

    private final QueryResponse queryResponse;
    private final DocumentObjectBinder binder;

    // HippoSolrClient and ContentBeanBinder are not serializable hence transient
    private final transient HippoSolrClient client;
    private transient List<ContentBeanBinder> contentBeanBinders;

    public HippoQueryResultImpl(final QueryResponse queryResponse, final DocumentObjectBinder binder,
                                final HippoSolrClient client) {
        this.queryResponse = queryResponse;
        this.binder = binder;
        this.client = client;
    }


    @Override
    public int getSize() {
        if (queryResponse.getResults().getNumFound() > Integer.MAX_VALUE) {
            throw new IllegalStateException(" Integer.MAX_VALUE hits is the maximum size");
        }
        return (int)queryResponse.getResults().getNumFound();
    }

    @Override
    public QueryResponse getQueryResponse() {
        return queryResponse;
    }

    @Override
    public HitIterator getHits() {
        return new HitIteratorImpl(queryResponse, binder, contentBeanBinders);
    }

    @Override
    public void setContentBeanBinders() {
        this.contentBeanBinders = client.getContentBeanBinders();
    }

    @Override
    public void setContentBeanBinders(final List<ContentBeanBinder> binders) {
        this.contentBeanBinders = binders;
    }

}
