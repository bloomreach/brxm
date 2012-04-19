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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.hippoecm.hst.content.beans.standard.ContentBean;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.content.beans.BindingException;
import org.hippoecm.hst.solr.content.beans.ContentBeanValueProvider;
import org.hippoecm.hst.solr.content.beans.query.Highlight;
import org.hippoecm.hst.solr.content.beans.query.Hit;

public class HitImpl implements Hit {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HitImpl.class);
    private final SolrDocument solrDocument;
    private final DocumentObjectBinder binder;
    private volatile List<ContentBeanValueProvider> contentBeanValueProviders;
    private final Map<String,List<String>> highlights;
    
    /**
     *
     * @param solrDocument
     * @param binder
     * @param contentBeanValueProviders the providers to be used to bind the hits to the original sources. When <code>null</code> the
     *                                  hits won't be attached to their providers
     */
    public HitImpl(final SolrDocument solrDocument, final DocumentObjectBinder binder, final Map<String,List<String>> highlights, final List<ContentBeanValueProvider> contentBeanValueProviders) {
        this.solrDocument = solrDocument;
        this.binder = binder;
        this.highlights = highlights;
        this.contentBeanValueProviders = contentBeanValueProviders;
    }

    @Override
    public SolrDocument getDoc() {
        return solrDocument;
    }

    @Override
    public ContentBean getContentBean() {
        ContentBean next = binder.getBean(ContentBean.class, solrDocument);
        if (contentBeanValueProviders == null) {
            return next;
        }
        for (ContentBeanValueProvider contentBeanValueProvider : contentBeanValueProviders) {
            if (contentBeanValueProvider.getAnnotatedClasses().contains(next.getClass())) {

                try {
                    contentBeanValueProvider.callbackHandler(next);
                } catch (BindingException e) {
                    if (log.isDebugEnabled()) {
                        // log stacktrace in debug mode
                        log.warn("Could not bind bean to provider", e);
                    } else {
                        log.warn("Could not bind bean to provider", e.getMessage());
                    }
                }
            }
        }
        return next;

    }

    @Override
    public float getScore() {
        Object o = solrDocument.getFieldValue("score");
        if ( o instanceof Float) {
            return ((Float)o).floatValue();
        }
        return 0;
    }
    
    public List<Highlight> getHighlights() {
        List<Highlight> highlightList = new ArrayList<Highlight>();
        for (Map.Entry<String,List<String>> entry : highlights.entrySet()) {
            highlightList.add(new HighlightImpl(entry.getKey(), entry.getValue()));
        }
        return highlightList;
    }
}
