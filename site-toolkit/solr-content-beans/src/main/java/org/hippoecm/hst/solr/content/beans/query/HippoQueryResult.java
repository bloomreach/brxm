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

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;

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
     * @return the result as {@link SolrDocumentList}
     */
    SolrDocumentList getDocs();

    /**
     * This returns the hits
     */
    HitIterator<IdentifiableContentBean> getHits();
}