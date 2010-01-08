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
package org.hippoecm.frontend.plugins.standards.browse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;

public class BrowseServiceTest extends PluginTest {

    private final class TestResultProvider implements IModel<QueryResult> {
        private static final long serialVersionUID = 1L;

        private List<Node> nodes;

        public TestResultProvider(List<Node> nodes) {
            this.nodes = nodes;
        }

        public Iterator<? extends Node> iterator(int first, int count) {
            return nodes.subList(first, first + count).iterator();
        }

        public IModel<Node> model(Node object) {
            return new JcrNodeModel(object);
        }

        public int size() {
            return nodes.size();
        }

        public void detach() {
        }

        public QueryResult getObject() {
            return new QueryResult() {

                public String[] getColumnNames() throws RepositoryException {
                    return new String[0];
                }

                public NodeIterator getNodes() throws RepositoryException {
                    return new NodeIterator() {

                        public Node nextNode() {
                            // TODO Auto-generated method stub
                            return null;
                        }

                        public long getPosition() {
                            // TODO Auto-generated method stub
                            return 0;
                        }

                        public long getSize() {
                            // TODO Auto-generated method stub
                            return 0;
                        }

                        public void skip(long skipNum) {
                            // TODO Auto-generated method stub
                            
                        }

                        public boolean hasNext() {
                            // TODO Auto-generated method stub
                            return false;
                        }

                        public Object next() {
                            // TODO Auto-generated method stub
                            return null;
                        }

                        public void remove() {
                            // TODO Auto-generated method stub
                            
                        }};
                }

                public RowIterator getRows() throws RepositoryException {
                    // TODO Auto-generated method stub
                    return null;
                }
                
            };
        }

        public void setObject(QueryResult object) {
            // TODO Auto-generated method stub
            
        }
    }

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final String BROWSE_SERVICE = "service.browser";
    static final String DOCUMENT_SERVICE = "service.document";
    static final String FOLDER_SERVICE = "service.folder";
    static final String SEARCH_MODEL_SERVICE = "service.search";

    String[] content = new String[] {
            "/test", "nt:unstructured",
                "/test/content", "hippostd:folder",
                    "/test/content/document", "hippo:handle",
                        "/test/content/document/document", "hippo:document",
                    "/test/content/folder", "hippostd:folder",
            "/test/config", "frontend:pluginconfig",
                BrowseService.BROWSER_ID, BROWSE_SERVICE,
                "model.search", SEARCH_MODEL_SERVICE,
                "model.folder", FOLDER_SERVICE,
                "model.document", DOCUMENT_SERVICE,
    };

    IModelReference<Node> getFolderService() {
        return context.getService(FOLDER_SERVICE, IModelReference.class);
    }

    IModelReference<Node> getDocumentService() {
        return context.getService(DOCUMENT_SERVICE, IModelReference.class);
    }

    IModelReference<BrowserSearchResult> getSearchModelService() {
        return context.getService(SEARCH_MODEL_SERVICE, IModelReference.class);
    }

    @Test
    public void updateModelsOnBrowse() throws Exception {
        build(session, content);

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test"));
        service.browse(new JcrNodeModel("/test/content/document"));

        assertEquals(new JcrNodeModel("/test/content"), getFolderService().getModel());
        assertEquals(new JcrNodeModel("/test/content/document"), getDocumentService().getModel());
    }

    @Test
    public void selectFolderWhenDocumentIsFolder() throws Exception {
        build(session, content);

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test"));
        service.browse(new JcrNodeModel("/test/content/folder"));

        assertEquals(new JcrNodeModel("/test/content/folder"), getFolderService().getModel());
        assertEquals(new JcrNodeModel((Node) null), getDocumentService().getModel());
    }

    @Test
    public void deselectDocumentMaintainsFolder() throws Exception {
        build(session, content);

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content/document"));

        IModelReference<Node> docService = getDocumentService();
        docService.setModel(new JcrNodeModel((Node) null));
        assertEquals(new JcrNodeModel("/test/content"), getFolderService().getModel());
    }

    @Test
    public void selectCurrentFolderForVersionedNode() throws Exception {
        build(session, content);

        root.getNode("test").accept(new ItemVisitor() {
            public void visit(Property property) throws RepositoryException {
            }

            public void visit(Node node) throws RepositoryException {
                if (node.isNodeType("hippo:document")) {
                    node.addMixin("hippo:harddocument");
                } else if (node.isNodeType("hippo:handle")) {
                    node.addMixin("hippo:hardhandle");
                }

                NodeIterator nodeIter = node.getNodes();
                while (nodeIter.hasNext()) {
                    nodeIter.nextNode().accept(this);
                }
            }
        });
        session.save();

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test"));

        Node handle = root.getNode("test/content/document");
        handle.checkin();
        Version base = handle.getBaseVersion();
        IModelReference<Node> docService = getDocumentService();
        docService.setModel(new JcrNodeModel(base));
        assertEquals(new JcrNodeModel("/test/content"), getFolderService().getModel());
    }

    // select folder A
    // set search model
    // select different folder B
    // assert search model is reset
    // select original folder A
    // assert search model is back to original

    // A search result is coupled to a folder.  When a different folder is selected, the search result should
    // no longer be available in the model.
    @Test
    public void searchModelFollowsFolder() throws Exception {
        build(session, content);

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content"));

        final List<Node> nodes = new LinkedList<Node>();
        nodes.add(session.getRootNode().getNode("test/content/document/document"));
        BrowserSearchResult searchResult = new BrowserSearchResult("query", new TestResultProvider(nodes));
        getSearchModelService().setModel(new Model(searchResult));
        assertEquals(searchResult, getSearchModelService().getModel().getObject());

        service.browse(new JcrNodeModel("/test/content/folder"));
        assertEquals(null, getSearchModelService().getModel().getObject());

        service.browse(new JcrNodeModel("/test/content"));
        assertEquals(searchResult, getSearchModelService().getModel().getObject());
    }

    @Test
    public void folderHistoryFollowsDocument() throws Exception {
        // select folder A
        // set search model
        // select document D from search result
        // select different folder B
        // select document D
        // assert folder model is back A
        // assert search model is back
        build(session, content);

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content"));

        final List<Node> nodes = new LinkedList<Node>();
        nodes.add(session.getRootNode().getNode("test/content/document/document"));
        getSearchModelService().setModel(new Model(new BrowserSearchResult("query", new TestResultProvider(nodes))));

        BrowserSearchResult searchResult = new BrowserSearchResult("query", new TestResultProvider(nodes));
        searchResult.setSelectedNode(new JcrNodeModel("/test/content/document/document").getNode());
        getSearchModelService().setModel(new Model(searchResult));
        assertEquals(new JcrNodeModel("/test/content/document"), getDocumentService().getModel());

        getDocumentService().setModel(new JcrNodeModel("/test/content/folder"));
        assertTrue(getSearchModelService().getModel().getObject() == null);

        getDocumentService().setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(new JcrNodeModel("/test/content"), getFolderService().getModel());
        assertFalse(getSearchModelService().getModel().getObject() == null);
    }

    @Test
    public void folderHistoryCanContainMultipleDocuments() throws Exception {
        // select folder A
        // set search model
        // select document D from search result
        // select document E from search result
        // browse to document D
        // assert search result is still available
        build(session, content);

        Node content = session.getRootNode().getNode("test/content");
        Node handle = content.addNode("docB", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node docB = handle.addNode("docB", "hippo:document");
        docB.addMixin("hippo:harddocument");

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content"));

        final List<Node> nodes = new LinkedList<Node>();
        nodes.add(session.getRootNode().getNode("test/content/document/document"));
        nodes.add(docB);
        BrowserSearchResult searchResult = new BrowserSearchResult("query", new TestResultProvider(nodes));
        getSearchModelService().setModel(new BrowserSearchResultModel(searchResult));

        searchResult.setSelectedNode(new JcrNodeModel("/test/content/document/document").getNode());
        assertEquals(new JcrNodeModel("/test/content/document"), getDocumentService().getModel());
        
        searchResult.setSelectedNode(docB);
        assertEquals(new JcrNodeModel("/test/content/docB"), getDocumentService().getModel());

        getDocumentService().setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(searchResult, getSearchModelService().getModel().getObject());
    }
    
    @Test
    public void clearSearchDeselectsDocument() throws Exception {
        // select folder
        // set search with document selected
        // clear search
        // assert that no document is selected
    }

}
