/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.upgrade;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class CmsUpgrader18b implements UpdaterModule{

static final Logger log = LoggerFactory.getLogger(CmsUpgrader18b.class);

    @Override
    public void register(UpdaterContext updaterContext) {
        updaterContext.registerName("v18b-cms-updater");
        updaterContext.registerStartTag("v18-cms");
        updaterContext.registerEndTag("v18b-cms");

        registerConsoleVisitors(updaterContext);
        updateCmsViewsDefaultColumns(updaterContext);
    }

    private void registerConsoleVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("frontend-console")) {
                    node.getNode("frontend-console").remove();
                }
            }

        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("console")) {
                    node.getNode("console").remove();
                }
            }

        });


    }

    private void updateCmsViewsDefaultColumns(UpdaterContext context){
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-folder-views"){
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node facetSearchDefaultColumns = node.getNode("hippo:facetsearch").getNode("defaultColumns");
                facetSearchDefaultColumns.setProperty("documentTypeIconRenderer", "resourceIconRenderer");

                Node directoryDefaultColumns = node.getNode("hippostd:directory").getNode("defaultColumns");
                directoryDefaultColumns.setProperty("documentTypeIconRenderer", "resourceIconRenderer");

                Node folderDefaultColumns = node.getNode("hippostd:folder").getNode("defaultColumns");
                folderDefaultColumns.setProperty("documentTypeIconRenderer", "resourceIconRenderer");
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-search-views"){
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node textDefaultColumns = node.getNode("text").getNode("defaultColumns");
                textDefaultColumns.setProperty("documentTypeIconRenderer", "resourceIconRenderer");
            }
        });
    }
}
