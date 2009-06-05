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

import javax.jcr.Node;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;

public class BrowseServiceTest extends PluginTest {

    static final String BROWSE_SERVICE = "service.browser";
    static final String DOCUMENT_SERVICE = "service.document";
    static final String FOLDER_SERVICE = "service.folder";

    String[] content = new String[] {
            "/test", "nt:unstructured",
                "/test/content", "hippostd:folder",
                    "/test/content/document", "hippo:handle",
                        "/test/content/document/document", "hippo:document",
                    "/test/content/folder", "hippostd:folder",
            "/test/config", "frontend:pluginconfig",
                BrowseService.BROWSER_ID, BROWSE_SERVICE,
                "model.folder", FOLDER_SERVICE,
                "model.document", DOCUMENT_SERVICE,
    };

    IModelReference<JcrNodeModel> getFolderService() {
        return context.getService(FOLDER_SERVICE, IModelReference.class);
    }

    IModelReference<JcrNodeModel> getDocumentService() {
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

        IModelReference<JcrNodeModel> docService = getDocumentService();
        docService.setModel(new JcrNodeModel((Node) null));
        assertEquals(new JcrNodeModel("/test/content"), getFolderService().getModel());
    }

}
