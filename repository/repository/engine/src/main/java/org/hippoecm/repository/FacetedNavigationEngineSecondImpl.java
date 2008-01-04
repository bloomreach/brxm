/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class FacetedNavigationEngineSecondImpl
  implements FacetedNavigationEngine<FacetedNavigationEngineSecondImpl.QueryImpl,
                                     FacetedNavigationEngineSecondImpl.ContextImpl>
{
    class QueryImpl extends FacetedNavigationEngine.Query {
        String query;
        TermQuery term;
        public QueryImpl(String query) {
            this.query = query;
            this.term = null; // FIXME: use parser to create a query, which is then picked up later
        }
    }

    class ResultImpl extends FacetedNavigationEngine.Result {
        int hits;
        Set<String> documents;
        ResultImpl(int hits) {
            documents = new HashSet<String>();
        }
        ResultImpl(int hits, Set<String> documents) {
            this.documents = documents;
        }
        public int length() {
            return hits;
        }
        public Iterator<String> iterator() {
            if(documents == null)
                return null;
            else
                return documents.iterator();
        }
    }

    class ContextImpl extends FacetedNavigationEngine.Context {
        String principal;
        Map<String,String[]> authorizationQuery;
        ContextImpl(Session session, String principal, Map<String,String[]> authorizationQuery) {
            this.principal = principal;
            this.authorizationQuery = authorizationQuery;
        }
        public String toString() {
            StringBuffer sb = null;
            if(authorizationQuery != null) {
                for(Map.Entry<String,String[]> authorizationEntry : authorizationQuery.entrySet()) {
                    if(sb == null)
                        sb = new StringBuffer();
                    else
                        sb.append("||");
                    sb.append(authorizationEntry.getKey());
                    sb.append("=");
                    sb.append(authorizationEntry.getValue());
                }
            } else
                sb = new StringBuffer("(null)");
            sb.insert(0,"query=");
            sb.insert(0,",");
            sb.insert(0,principal);
            sb.insert(0,"[principal=");
            sb.insert(0, getClass().getName());
            sb.append("]");
            return new String(sb);
        }
    }

    static private final class Collector extends HitCollector {
        private IndexSearcher searcher;
        private IndexReader reader;
        private Map<String,Map<String,Count>> facets;
        private int numhits;
        private Set<Integer> hits;
        public Collector(IndexSearcher searcher, Map<String,Map<String,Count>> facets, boolean hitsRequested) {
            this.searcher = searcher;
            reader = searcher.getIndexReader();
            numhits = 0;
            this.facets = facets;
            if(hitsRequested)
                hits = new HashSet<Integer>();
            else
                hits = null;
        }
        public final void collect(final int docid, final float score) {
            try {
                if(hits != null) {
                    /* one can replace the set with a datastructure which
                     * keeps only the N top matching (by score) documents, to
                     * prevent retrieving and listing all documents.
                     */
                    hits.add(docid);
                }
                if(facets != null) {
                    for(Map.Entry<String,Map<String,Count>> entry : facets.entrySet()) {
                        final TermFreqVector tfv = reader.getTermFreqVector(docid, entry.getKey());
                        if(tfv != null) {
                            final int[] freqs = tfv.getTermFrequencies();
                            final int[] terms = tfv.indexesOf(tfv.getTerms(), 0, tfv.size());
                            for(int i=0; i<freqs.length; i++) {
                                /* FIXME: This is the remaining problem, the
                                 * terms are not facet values, but indices,
                                 * which leaves the problem how to translate
                                 * them back to facet values.  This can be
                                 * performed outside of the Collector class.
                                 */
                                Count count = entry.getValue().get(Integer.toString(terms[i]));
                                if(count == null)
                                    entry.getValue().put(Integer.toString(terms[i]), new Count(1));
                                else
                                    count.count += 1;
                            }
                        }
                    }
                }
                ++numhits;
            } catch(Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        public final Set<Integer> getHits() {
            return hits;
        }
        public final int getNumhits() {
            return numhits;
        }
    }

    Directory directory;
    IndexSearcher searcher;
    public FacetedNavigationEngineSecondImpl(Session session) throws RepositoryException {
        try {
            directory = new RAMDirectory();
            IndexWriter writer = new IndexWriter(directory, new StandardAnalyzer(), true);
            writer.setUseCompoundFile(false);
            writer.setMergeFactor(50);
            javax.jcr.query.Query query = session.getWorkspace().getQueryManager().createQuery("documents//node()",
                                                                                               javax.jcr.query.Query.XPATH);
            javax.jcr.query.QueryResult result = query.execute();
            for(NodeIterator iter = result.getNodes(); iter.hasNext(); iter.hasNext()) {
                Node node = iter.nextNode();
                Document document = new Document();
                document.add(new Field("id", node.getPath(), Field.Store.YES, Field.Index.TOKENIZED));
                document.add(new Field("type", "document", Field.Store.NO, Field.Index.TOKENIZED));
                if(node.hasProperty("x"))
                    document.add(new Field("x", node.getProperty("x").getString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.YES));
                if(node.hasProperty("y"))
                    document.add(new Field("y", node.getProperty("y").getString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.YES));
                if(node.hasProperty("z"))
                    document.add(new Field("z", node.getProperty("z").getString(), Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.YES));
                writer.addDocument(document);
            }
            writer.close();
            searcher = new IndexSearcher(directory);
        } catch(IOException ex) {
            throw new RepositoryException(ex);
        }
    }

    public ContextImpl prepare(String principal, Map<String,String[]> authorizationQuery, List<QueryImpl> initialQueries, Session session) {
        return new ContextImpl(session, principal, authorizationQuery);
    }
    public void unprepare(ContextImpl authorization) {
    }
    public void reload(Map<String,String[]> facetValues) {
    }
    public boolean requiresReload() {
        return false;
    }
    public boolean requiresNotify() {
        return false;
    }
    public void notify(String docId, Map<String,String[]> oldFacets, Map<String,String[]> newFacets) {
    }
    public void purge() {
        // deliberate ignore
    }
    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
                       Map<String,String> facetsQuery, QueryImpl openQuery,
                       Map<String,Map<String,Count>> resultset,
                       Map<Map<String,String>,Map<String,Map<String,Count>>> futureFacetsQueries,
                       HitsRequested hitsRequested) throws UnsupportedOperationException
    {
        // no impl
        return new ResultImpl(0);

    }
    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
                       Map<String,String> facetsQuery, QueryImpl openQuery,HitsRequested hitsRequested)
    {
        return view(queryName, initialQuery, authorization, facetsQuery, openQuery, null, null, hitsRequested);
    }

    public QueryImpl parse(String query)
    {
        return this . new QueryImpl(query);
    }
}
