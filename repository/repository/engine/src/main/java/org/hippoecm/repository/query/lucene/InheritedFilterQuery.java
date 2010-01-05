/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.query.lucene;

import java.util.Map;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InheritedFilterQuery {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(InheritedFilterQuery.class);

    /**
     * The lucene query
     */
    private BooleanQuery query;

    public InheritedFilterQuery(Map<Name, String> inheritedFilter, NamespaceMappings nsMappings,
                       ServicingIndexingConfiguration indexingConfig) {
        this.query = new BooleanQuery(true);

        if (inheritedFilter != null) {
            for (Map.Entry<Name, String> entry : inheritedFilter.entrySet()) {
                try {
                    Name nodeName = entry.getKey();
                    String field = nsMappings.translatePropertyName(nodeName);
                    Term t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, entry.getValue()));
                    Query wq = new TermQuery(t);
                    this.query.add(wq, Occur.MUST);
                } catch (IllegalNameException e) {
                    log.error(e.toString());
                } 
            }
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }
}
