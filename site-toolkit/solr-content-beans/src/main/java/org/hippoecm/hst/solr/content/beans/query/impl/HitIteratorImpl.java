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
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.content.beans.ContentBeanValueProvider;
import org.hippoecm.hst.solr.content.beans.query.Hit;
import org.hippoecm.hst.solr.content.beans.query.HitIterator;

public class HitIteratorImpl implements HitIterator<IdentifiableContentBean> {

    private long position = 0;
    private final QueryResponse queryResponse;
    private SolrDocumentList docs;
    private DocumentObjectBinder binder;
    private volatile List<ContentBeanValueProvider> contentBeanValueProviders;

    /**
     *
     * @param queryResponse
     * @param docs
     * @param binder
     * @param contentBeanValueProviders the providers to be used to bind the hits to the original sources. When <code>null</code> the
     */
    public HitIteratorImpl(final QueryResponse queryResponse, SolrDocumentList docs, DocumentObjectBinder binder, final List<ContentBeanValueProvider> contentBeanValueProviders) {
        this.queryResponse = queryResponse;
        this.docs = docs;
        this.binder = binder;
        this.contentBeanValueProviders = contentBeanValueProviders;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public long getSize() {
        return docs.size();
    }

    @Override
    public void skip(int skipNum) {
        position += skipNum;
        if (position >= getSize()) {
            position = getSize() - 1;
        }
    }

    @Override
    public boolean hasNext() {
        return position < getSize();
    }

    @Override
    public Hit next() {
        if (position >= getSize()) {
            throw new NoSuchElementException("No next bean");
        }
        if (position > Integer.MAX_VALUE) {
            throw new IllegalStateException("Can't iterate beyond Integer.MAX_VALUE hits");
        }

        SolrDocument solrDoc = docs.get((int) position);
        Map<String,List<String>> highlights = null;
        if (queryResponse.getHighlighting() != null) {
            highlights = queryResponse.getHighlighting().get(solrDoc.get("id"));
        }

        Hit next = new HitImpl(solrDoc, binder, highlights, contentBeanValueProviders);
        position++;
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported");
    }

}

