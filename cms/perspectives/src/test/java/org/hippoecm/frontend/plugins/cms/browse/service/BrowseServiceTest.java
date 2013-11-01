/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.service;

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
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.cms.browse.section.BrowsingSectionPlugin;
import org.hippoecm.frontend.plugins.cms.browse.section.SearchingSectionPlugin;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResultModel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
                    final Iterator<Node> upstream = nodes.iterator();
                    return new NodeIterator() {

                        public Node nextNode() {
                            return upstream.next();
                        }

                        public long getPosition() {
                            return -1;
                        }

                        public long getSize() {
                            return nodes.size();
                        }

                        public void skip(long skipNum) {
                            while (skipNum-- > 0)
                                upstream.next();
                        }

                        public boolean hasNext() {
                            return upstream.hasNext();
                        }

                        public Object next() {
                            return upstream.next();
                        }

                        public void remove() {
                            upstream.remove();
                        }};
                }

                public RowIterator getRows() throws RepositoryException {
                    // TODO Auto-generated method stub
                    return null;
                }

                public String[] getSelectorNames() throws RepositoryException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

            };
        }

        public void setObject(QueryResult object) {
            // TODO Auto-generated method stub

        }
    }


    static final String BROWSE_SERVICE = "service.browser";
    static final String DOCUMENT_SERVICE = "service.document";
    static final String FOLDER_SERVICE = "service.folder";

    static final String BROWSE_SECTION = "section.browse";
    static final String SEARCH_SECTION = "section.search";

    String[] content = new String[] {
            "/test", "nt:unstructured",
                "/test/content", "hippostd:folder",
                    "/test/content/document", "hippo:handle",
                        "jcr:mixinTypes", "mix:versionable",
                        "/test/content/document/document", "frontendtest:document",
                            "jcr:mixinTypes", "mix:referenceable",
                            "a", "xxx",
                    "/test/content/folder", "hippostd:folder",
            "/test/config", "frontend:pluginconfig",
                BrowseService.BROWSER_ID, BROWSE_SERVICE,
                "sections", "section.browse",
                "section.browse", BROWSE_SECTION,
                "section.search", SEARCH_SECTION,
                "model.document", DOCUMENT_SERVICE,
                "model.folder", FOLDER_SERVICE,
            "/test/browseplugin", "frontend:plugin",
                "plugin.class", BrowsingSectionPlugin.class.getName(),
                "wicket.id", BROWSE_SECTION,
                "model.folder", "section.browse.folder",
                "model.folder.root", "/test/content",
            "/test/searchplugin", "frontend:plugin",
                "plugin.class", SearchingSectionPlugin.class.getName(),
                "wicket.id", SEARCH_SECTION,
                "model.folder", "section.search.folder",
                "model.folder.root", "/test/content/folder",
    };

    IModelReference<Node> getDocumentService() {
        return context.getService(DOCUMENT_SERVICE, IModelReference.class);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);
        session.save();

        start(new JcrPluginConfig(new JcrNodeModel("/test/browseplugin")));
    }

    @Test
    public void updateModelsOnBrowse() throws Exception {
        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test"));
        service.browse(new JcrNodeModel("/test/content/document"));

        assertEquals(new JcrNodeModel("/test/content"), service.getCollectionModel().getObject().getFolder());
        assertEquals(new JcrNodeModel("/test/content/document"), getDocumentService().getModel());
    }

    @Test
    public void selectHandleForDocument() throws Exception {
        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                                                  new JcrNodeModel("/test"));
        service.browse(new JcrNodeModel("/test/content/document/document"));

        assertEquals(new JcrNodeModel("/test/content"), service.getCollectionModel().getObject().getFolder());
        assertEquals(new JcrNodeModel("/test/content/document"), getDocumentService().getModel());
    }

    @Test
    public void selectFolderWhenDocumentIsFolder() throws Exception {
        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test"));
        service.browse(new JcrNodeModel("/test/content/folder"));

        assertEquals(new JcrNodeModel("/test/content/folder"), service.getCollectionModel().getObject().getFolder());
    }

    @Test
    public void deselectDocumentMaintainsFolder() throws Exception {
        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content/document"));

        IModelReference<Node> docService = getDocumentService();
        docService.setModel(new JcrNodeModel((Node) null));
        assertEquals(new JcrNodeModel("/test/content"), service.getCollectionModel().getObject().getFolder());
        assertEquals(null, docService.getModel().getObject());
    }

    @Test
    public void selectCurrentFolderForVersionedNode() throws Exception {
        root.getNode("test").accept(new ItemVisitor() {
            public void visit(Property property) throws RepositoryException {
            }

            public void visit(Node node) throws RepositoryException {
                if (node.isNodeType("hippo:document")) {
                    node.addMixin("mix:referenceable");
                } else if (node.isNodeType("hippo:docNode")) {
                    node.addMixin("mix:referenceable");
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

        Node docNode = root.getNode("test/content/document/document");
        docNode.checkin();
        Version base = docNode.getBaseVersion();
        service.browse(new JcrNodeModel(base));
        assertEquals(new JcrNodeModel("/test/content"), service.getCollectionModel().getObject().getFolder());
    }

    @Test
    public void switchSectionUpdatesDoclisting() throws Exception {
        root.getNode("test/config").getProperty("sections").remove();
        root.getNode("test/config").setProperty("sections", new String[] { "section.browse", "section.search" });

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content/document"));

        start(new JcrPluginConfig(new JcrNodeModel("/test/searchplugin")));

        service.getSections().setActiveSection("section.search");
        assertEquals(new JcrNodeModel("/test/content/folder"), service.getCollectionModel().getObject().getFolder());
    }

    @Test
    public void enterSearchUpdatesDoclisting() throws Exception {
        BrowseService service = startServiceWithSearchSection(null);

        assertEquals(DocumentCollectionType.SEARCHRESULT, service.getCollectionModel().getObject().getType());
    }

    @Test
    public void folderFollowsDocumentInBrowseMode() throws Exception {
        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content/folder"));

        assertNull(getDocumentService().getModel().getObject());

        getDocumentService().setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(service.getCollectionModel().getObject().getFolder(), new JcrNodeModel("/test/content"));
    }

    @Test
    public void searchDoesNotFollowDocument() throws Exception {
        BrowseService service = startServiceWithSearchSection(null);

        IModel<BrowserSearchResult> searchResultModel = service.getCollectionModel().getObject().getSearchResult();
        getDocumentService().setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(searchResultModel, service.getCollectionModel().getObject().getSearchResult());
    }

    @Test
    public void folderDoesNotFollowDocumentWhenSearching() throws Exception {
        Node otherDoc = root.getNode("test/content").addNode("doc", "hippo:document");
        otherDoc.addMixin("mix:referenceable");
        session.save();

        BrowseService service = startServiceWithSearchSection(null);
        getDocumentService().setModel(new JcrNodeModel(otherDoc));
        assertEquals(new JcrNodeModel("/test/content/folder"), service.getCollectionModel().getObject().getFolder());
    }

    @Test
    public void sectionFollowsFolder() throws Exception {
        root.getNode("test/config").getProperty("sections").remove();
        root.getNode("test/config").setProperty("sections", new String[] { "section.browse", "section.more" });
        root.getNode("test/config").setProperty("section.more", "section.more");

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test/content/document"));

        root.getNode("test").addNode("other", "hippostd:folder");

        JavaPluginConfig browseSectionClone = new JavaPluginConfig(new JcrPluginConfig(new JcrNodeModel("/test/browseplugin")));
        browseSectionClone.put("wicket.id", "section.more");
        browseSectionClone.put("model.folder", "section.more.folder");
        browseSectionClone.put("model.folder.root", "/test/other");
        start(browseSectionClone);

        getDocumentService().setModel(new JcrNodeModel("/test/other"));
        assertEquals("section.more", service.getSections().getActiveSection());
    }

    /*
    @Test
    public void searchIsResetOnSectionSwitch() throws Exception {
        BrowseService service = startServiceWithSearchSection();

        IModel<BrowserSearchResult> searchResultModel = service.getCollectionModel().getObject().getSearchResult();
        getDocumentService().setModel(new JcrNodeModel("/test/content/document"));
        assertEquals(searchResultModel, service.getCollectionModel().getObject().getSearchResult());
    }
    */

    @Test
    public void switchToBrowseWhenSelectingFolder() throws Exception {
        Node child = root.getNode("test/content/folder").addNode("kind", "hippostd:folder");
        child.addMixin("mix:referenceable");
        session.save();

        BrowseService service = startServiceWithSearchSection(null);

        getDocumentService().setModel(new JcrNodeModel(child));
        assertEquals(DocumentCollectionType.FOLDER, service.getCollectionModel().getObject().getType());
    }

    @Test
    public void switchToBrowseWhenDocumentIsNotInSearchResults() throws Exception {
        Node otherHandle = root.getNode("test/content/folder").addNode("doc", "hippo:handle");
        otherHandle.addMixin("mix:referenceable");
        Node otherDoc = otherHandle.addNode("doc", "hippo:document");
        otherDoc.addMixin("mix:referenceable");
        session.save();

        BrowseService service = startServiceWithSearchSection(null);

        getDocumentService().setModel(new JcrNodeModel(otherDoc));
        assertEquals(DocumentCollectionType.FOLDER, service.getCollectionModel().getObject().getType());
    }

    @Test
    public void keepSearchingWhenDocumentIsInSearchResults() throws Exception {
        Node otherHandle = root.getNode("test/content/folder").addNode("doc", "hippo:handle");
        otherHandle.addMixin("mix:referenceable");
        Node otherDoc = otherHandle.addNode("doc", "hippo:document");
        otherDoc.addMixin("mix:referenceable");
        session.save();

        List<Node> nodes = new LinkedList<Node>();
        nodes.add(root.getNode("test/content/document/document"));
        nodes.add(otherDoc);
        BrowseService service = startServiceWithSearchSection(nodes);

        getDocumentService().setModel(new JcrNodeModel(otherHandle));
        assertEquals(DocumentCollectionType.SEARCHRESULT, service.getCollectionModel().getObject().getType());
    }

    /**
     * starts a search section.  The search has a single result, /test/content/document/document.
     */
    private BrowseService startServiceWithSearchSection(List<Node> nodes) throws Exception {
        IPluginConfig serviceConfig = new JcrPluginConfig(new JcrNodeModel("/test/config"));
        serviceConfig.put("sections", "section.search");

        BrowseService service = new BrowseService(context, serviceConfig, new JcrNodeModel("/test/content/document"));

        start(new JcrPluginConfig(new JcrNodeModel("/test/searchplugin")));

        if (nodes == null) {
            nodes = new LinkedList<Node>();
            nodes.add(root.getNode("test/content/document/document"));
        }
        service.getCollectionModel().getObject().setSearchResult(
                new BrowserSearchResultModel(new BrowserSearchResult("test", new TestResultProvider(nodes))));

        return service;
    }

}
