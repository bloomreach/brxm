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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResultModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchDocumentsProviderTest extends PluginTest {

    String[] content = new String[] {
            "/test", "nt:unstructured",
                "/test/content", "hippostd:folder",
                    "/test/content/folder", "hippostd:folder",
                    "/test/content/document1", "hippo:handle",
                        "jcr:mixinTypes", "mix:referenceable",
                        "/test/content/document1/document1", "frontendtest:document",
                            "jcr:mixinTypes", "mix:referenceable",
                            "a", "xxx",
                         "/test/content/document1/document1", "frontendtest:document",
                            "jcr:mixinTypes", "mix:referenceable",
                            "b", "xxx",
                      "/test/content/document2", "hippo:handle",
                         "jcr:mixinTypes", "mix:referenceable",
                        "/test/content/document2/document2", "frontendtest:document",
                            "jcr:mixinTypes", "mix:referenceable",
                            "a", "xxx",
                      "/test/content/document3", "hippo:handle",
                        "jcr:mixinTypes", "mix:referenceable",
                        "/test/content/document3/document3", "frontendtest:document",
                            "jcr:mixinTypes", "mix:referenceable",
                            "a", "xxx",
                      "/test/content/document4", "hippo:handle",
                         "jcr:mixinTypes", "mix:referenceable",
                        "/test/content/document4/document4", "frontendtest:document",
                            "jcr:mixinTypes", "mix:referenceable",
                            "a", "xxx",
                    "/test/content/otherfolder", "hippostd:folder",
    };

    @Test
    public void foldersBeforeDocumentsAndOrderKeptAndDuplicateDocsMerged() throws Exception {
        build(session, content);
        session.save();

        BrowserSearchResultModel bsrm = new BrowserSearchResultModel(new BrowserSearchResult("query",
                new LoadableDetachableModel<QueryResult>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QueryResult load() {

                        final List<Node> nodes = new LinkedList<Node>();
                        try {
                            nodes.add(root.getNode("test/content/folder"));
                            nodes.add(root.getNode("test/content/document4/document4"));
                            nodes.add(root.getNode("test/content/document3/document3"));
                            nodes.add(root.getNode("test/content/document2/document2"));
                            nodes.add(root.getNode("test/content/document1/document1"));
                            nodes.add(root.getNode("test/content/document1/document1[1]"));
                            nodes.add(root.getNode("test/content/otherfolder"));
                        } catch (RepositoryException e) {
                            throw new RuntimeException("Could not find node", e);
                        }
                        return new QueryResult() {

                            int position = 0;
                            
                            public String[] getColumnNames() throws RepositoryException {
                                return new String[] { "jcr:path" };
                            }

                            public NodeIterator getNodes() throws RepositoryException {
                                final Iterator<Node> upstream = nodes.iterator();
                                return new NodeIterator() {

                                    public Node nextNode() {
                                        position++;
                                        return upstream.next();
                                    }

                                    public long getPosition() {
                                        return position;
                                    }

                                    public long getSize() {
                                        return nodes.size();
                                    }

                                    public void skip(long skipNum) {
                                        position+=skipNum;
                                    }

                                    public boolean hasNext() {
                                        return upstream.hasNext();
                                    }

                                    public Object next() {
                                        return upstream.next();
                                    }

                                    public void remove() {
                                        upstream.remove();
                                    }
                                };
                            }

                            public RowIterator getRows() throws RepositoryException {
                                final Iterator<Node> upstream = nodes.iterator();
                                return new RowIterator() {

                                    public Row nextRow() {
                                        final Node upstreamNode = upstream.next();
                                        position++;
                                        return new Row() {

                                            public Value getValue(String propertyName) throws ItemNotFoundException,
                                                    RepositoryException {
                                                return session.getValueFactory().createValue(upstreamNode.getPath());
                                            }

                                            public Value[] getValues() throws RepositoryException {
                                                return null;
                                            }

                                            public Node getNode() throws RepositoryException {
                                                return upstreamNode;
                                            }

                                            public Node getNode(String selectorName) throws RepositoryException {
                                                throw new UnsupportedOperationException("Not supported yet.");
                                            }

                                            public String getPath() throws RepositoryException {
                                                throw new UnsupportedOperationException("Not supported yet.");
                                            }

                                            public String getPath(String selectorName) throws RepositoryException {
                                                throw new UnsupportedOperationException("Not supported yet.");
                                            }

                                            public double getScore() throws RepositoryException {
                                                throw new UnsupportedOperationException("Not supported yet.");
                                            }

                                            public double getScore(String selectorName) throws RepositoryException {
                                                throw new UnsupportedOperationException("Not supported yet.");
                                            }

                                        };
                                    }

                                    public long getPosition() {
                                        return position;
                                    }

                                    public long getSize() {
                                        return nodes.size();
                                    }

                                    public void skip(long skipNum) {
                                        position+=skipNum;
                                    }

                                    public boolean hasNext() {
                                        return upstream.hasNext();
                                    }

                                    public Object next() {
                                        return nextRow();
                                    }

                                    public void remove() {
                                        throw new UnsupportedOperationException("move not supported");
                                    }
                                    
                                };
                            }

                    public String[] getSelectorNames() throws RepositoryException {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                        };
                    }
                }));
        SearchDocumentsProvider provider = new SearchDocumentsProvider(bsrm, new HashMap());
        assertEquals(6, provider.size());

        Iterator<Node> iter = provider.iterator(0, provider.size());
        assertEquals("/test/content/folder", iter.next().getPath());
        assertEquals("/test/content/otherfolder", iter.next().getPath());
        assertEquals("/test/content/document4", iter.next().getPath());
        assertEquals("/test/content/document3", iter.next().getPath());
        assertEquals("/test/content/document2", iter.next().getPath());
        assertEquals("/test/content/document1", iter.next().getPath());
    }
}
