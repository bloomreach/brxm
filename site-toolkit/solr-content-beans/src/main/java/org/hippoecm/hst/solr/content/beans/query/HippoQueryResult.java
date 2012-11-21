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
package org.hippoecm.hst.solr.content.beans.query;

import java.io.Serializable;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;

/**
 * The result of the execution of the HstQuery.
 */
public interface HippoQueryResult extends Serializable {

    /**
     *
     * @return the total number of hits.
     */
    int getSize();

    /**
     * @return the raw solr query response
     */
    QueryResponse getQueryResponse();

    /**
     * This returns the hits
     */
    HitIterator getHits();

    /**
     * <p>
     *  sets the binders for the IdentifiableContentBean's in the {@link Hit}s to their {@link org.hippoecm.hst.solr.content.beans.ContentBeanBinder
     *  if there is a binder for the hit. The default available {@link org.hippoecm.hst.solr.HippoSolrClient#getContentBeanBinders()}
     *  are used as binders.
     * </p>
     * <p>
     *  Note that binding the hits to their provider is less efficient than just using the populated beans from the search result. If
     *  the binder is for example populating hits by fetching external sources over http, this might have quite a negative 
     *  impact on performance. 
     * </p>
     * <p>
     *     Thus, if you can use the search result without binders, this will be always faster
     * </p>
     */
    void setContentBeanBinders();

    /**
     * See {@link #setContentBeanBinders} only now, the <code>binders</code> in the argument are used instead of the
     * default binders from the {@link org.hippoecm.hst.solr.HippoSolrClient}
     * @see #setContentBeanBinders
     * @param binders the <code>binders</code> to use for binding the hits
     */
    void setContentBeanBinders(List<ContentBeanBinder> binders);

}