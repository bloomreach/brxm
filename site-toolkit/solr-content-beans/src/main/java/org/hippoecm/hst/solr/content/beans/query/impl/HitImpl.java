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

import org.apache.solr.common.SolrDocument;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.hippoecm.hst.solr.DocumentObjectBinder;
import org.hippoecm.hst.solr.content.beans.BindingException;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.Highlight;
import org.hippoecm.hst.solr.content.beans.query.Hit;

public class HitImpl implements Hit {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HitImpl.class);
    private final SolrDocument solrDocument;
    private final DocumentObjectBinder binder;
    // identifiableContentBean and ContentBeanBinder do not need to be Serializable, hence transient
    private transient List<ContentBeanBinder> contentBeanBinders;
    private transient IdentifiableContentBean identifiableContentBean;
    private final Map<String,List<String>> highlights;

    /**
     *
     * @param solrDocument
     * @param binder
     * @param highlights the highlights for this hit and <code>null</code> if there are no highlights
     * @param contentBeanBinders the providers to be used to bind the hits to the original sources. When <code>null</code> the
     *                                  hits won't be attached to their providers
     */
    public HitImpl(final SolrDocument solrDocument, final DocumentObjectBinder binder, final Map<String,List<String>> highlights, final List<ContentBeanBinder> contentBeanBinders) {
        this.solrDocument = solrDocument;
        this.binder = binder;
        this.highlights = highlights;
        this.contentBeanBinders = contentBeanBinders;
    }

    @Override
    public SolrDocument getDoc() {
        return solrDocument;
    }

    @Override
    public IdentifiableContentBean getBean() throws BindingException {
        if (identifiableContentBean != null) {
            return identifiableContentBean;
        }

        try {
            identifiableContentBean = binder.getBean(IdentifiableContentBean.class, solrDocument);
            if (contentBeanBinders == null) {
                return identifiableContentBean;
            }
            for (ContentBeanBinder contentBeanBinder : contentBeanBinders) {
                if (contentBeanBinder.getBindableClasses().contains(identifiableContentBean.getClass())) {

                    try {
                        contentBeanBinder.callbackHandler(identifiableContentBean);
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
            return identifiableContentBean;
        } catch (Exception e) {
            throw new BindingException("Could not bind SolrDocument to Content Bean ", e);
        }

    }

    @Override
    public float getScore() {
        Object o = solrDocument.getFieldValue("score");
        if ( o instanceof Float) {
            return ((Float)o).floatValue();
        }
        return -1;
    }
    
    public List<Highlight> getHighlights() {
        List<Highlight> highlightList = new ArrayList<Highlight>();
        if (highlights != null) {
            for (Map.Entry<String,List<String>> entry : highlights.entrySet()) {
                highlightList.add(new HighlightImpl(entry.getKey(), entry.getValue()));
            }
        }
        return highlightList;
    }
}
