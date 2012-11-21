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


import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.hippoecm.hst.solr.content.beans.ContentBeanBinder;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryParser;

public interface HippoSolrClient {

    /**
     * @return the {@link SolrServer}
     * @throws SolrServerException
     */
    SolrServer getSolrServer() throws SolrServerException;

    /**
     * <p>
     * Creates a new {@link HippoQuery} with an initial bootstrappped {@link org.apache.solr.client.solrj.SolrQuery} which
     * has its {@link org.apache.solr.client.solrj.SolrQuery#setQuery(String)} called with <code>query</code>.
     * </p>
     * <p>For <code>query</code> thus the general Solr syntax can be used, see http://wiki.apache.org/solr/SolrQuerySyntax. For example
     * <pre>
     *     <code>
     *        query = title:hippo +createdate:[1976-03-06T23:59:59.999Z TO *]
     *     </code>
     *
     * </pre>
     * </p>
     * @param query the <code>query</code> to bootstrap the {@link org.apache.solr.client.solrj.SolrQuery} with.
     * @return a {@link HippoQuery}
     */
    HippoQuery createQuery(String query);

    /**
     * @return a HippoQueryParser instance which can be used to escape or remove Lucene specifc
     * query chars
     */
    HippoQueryParser getQueryParser();

    /**
     * 
     * @return the {@link List} of {@link org.hippoecm.hst.solr.content.beans.ContentBeanBinder} and empty list of no providers are available
     */
    List<ContentBeanBinder> getContentBeanBinders();
}
