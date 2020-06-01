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
package org.hippoecm.frontend.plugins.standards.browse;

import javax.jcr.Node;
import javax.jcr.version.Version;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowseServiceTest extends PluginTest {

    static final String BROWSE_SERVICE = "service.browser";
    static final String DOCUMENT_SERVICE = "service.document";
    static final String FOLDER_SERVICE = "service.folder";

    String[] content = new String[] {
            "/test", "nt:unstructured",
                "/test/content", "hippostd:folder",
                    "jcr:mixinTypes", "mix:referenceable",
                    "/test/content/document", "hippo:handle",
                        "jcr:mixinTypes", "mix:referenceable",
                        "/test/content/document/document", "hippo:document",
                            "jcr:mixinTypes", "mix:versionable",
                    "/test/content/folder", "hippostd:folder",
                        "jcr:mixinTypes", "mix:referenceable",
            "/test/config", "frontend:pluginconfig",
                BrowseService.BROWSER_ID, BROWSE_SERVICE,
                "model.folder", FOLDER_SERVICE,
                "model.document", DOCUMENT_SERVICE,
    };

    IModelReference<Node> getFolderService() {
        return context.getService(FOLDER_SERVICE, IModelReference.class);
    }

    IModelReference<Node> getDocumentService() {
        return context.getService(DOCUMENT_SERVICE, IModelReference.class);
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
        session.save();

        BrowseService service = new BrowseService(context, new JcrPluginConfig(new JcrNodeModel("/test/config")),
                new JcrNodeModel("/test"));

        Node document = root.getNode("test/content/document/document");
        document.checkin();
        Version base = document.getBaseVersion();
        IModelReference<Node> docService = getDocumentService();
        docService.setModel(new JcrNodeModel(base));
        assertEquals(new JcrNodeModel("/test/content"), getFolderService().getModel());
    }

}
