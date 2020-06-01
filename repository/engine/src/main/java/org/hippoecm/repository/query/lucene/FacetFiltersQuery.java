/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.JackrabbitQueryParser;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SynonymProvider;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.FacetFilters;
import org.hippoecm.repository.FacetFilters.FacetFilter;

public class FacetFiltersQuery {

    /**
     * The lucene query: note the boolean in constructor of BooleanQuery must be false because this query is
     * used in scoring as well!
     */
    private BooleanQuery query = new BooleanQuery(false);

    private boolean plainLuceneQuery = true;

    public FacetFiltersQuery(FacetFilters facetFilters, NamespaceMappings nsMappings, Analyzer analyzer, SynonymProvider synonymProvider) throws IllegalArgumentException {
       try {
            for (FacetFilter filter : facetFilters.getFilters()) {
                if (filter.operator == FacetFilters.NOOP_OPERATOR) {
                    plainLuceneQuery = false;
                    QueryParser parser = new JackrabbitQueryParser(FieldNames.FULLTEXT, analyzer, synonymProvider, null);
                    Query freeTextQuery = parser.parse(filter.queryString);
                    if(filter.negated) {
                        freeTextQuery = QueryHelper.negateQuery(freeTextQuery);
                    }
                    query.add(freeTextQuery, Occur.MUST);
                } else if (filter.operator == FacetFilters.CONTAINS_OPERATOR) {
                    plainLuceneQuery = false;
                    Name propName = NameFactoryImpl.getInstance().create(filter.namespacedProperty);
                    StringBuffer tmp = new StringBuffer();
                    tmp.append(nsMappings.getPrefix(propName.getNamespaceURI()));
                    tmp.append(":").append(FieldNames.FULLTEXT_PREFIX);
                    tmp.append(propName.getLocalName());
                    String fieldname = tmp.toString();
                    QueryParser parser = new JackrabbitQueryParser(fieldname, analyzer, synonymProvider, null);
                    Query textQuery = parser.parse(filter.queryString);
                    if(filter.negated) {
                        textQuery = QueryHelper.negateQuery(textQuery);
                    }
                    query.add(textQuery, Occur.MUST);
                    
                } else if (filter.operator == FacetFilters.EQUAL_OPERATOR
                        || filter.operator == FacetFilters.NOTEQUAL_OPERATOR) {
                    
                    Name propName = NameFactoryImpl.getInstance().create(filter.namespacedProperty);
                    Query wq = null;
                    
                    if(propName.equals(NameConstants.JCR_PRIMARYTYPE) || propName.equals(NameConstants.JCR_MIXINTYPES)) {
                        String internalFacetName = ServicingNameFormat.getInternalFacetName(propName, nsMappings);
                        Term t = new Term(internalFacetName , filter.queryString);
                        wq = new TermQuery(t);
                    } else {
                        String field = nsMappings.translateName(propName);
                        Term t = new Term(FieldNames.PROPERTIES, FieldNames.createNamedValue(field, filter.queryString));
                        wq = new TermQuery(t);
                    }
                    if(filter.operator == FacetFilters.NOTEQUAL_OPERATOR) {
                        wq = QueryHelper.negateQuery(wq);
                    }
                    if(filter.negated) {
                        wq = QueryHelper.negateQuery(wq);
                    }
                    this.query.add(wq, Occur.MUST);
                }
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse filter into a Lucene query : '"+e.getMessage()+"'");
        } catch (IllegalNameException e) {
            throw new IllegalArgumentException("Unable to parse filter into a Lucene query : '"+e.getMessage()+"'");
        } catch (NamespaceException e) {
            throw new IllegalArgumentException("Unable to parse filter into a Lucene query : '"+e.getMessage()+"'");
        }
       
    }
    
    public BooleanQuery getQuery() {
        return query;
    }

    public boolean isPlainLuceneQuery() {
        return plainLuceneQuery;
    }

}
