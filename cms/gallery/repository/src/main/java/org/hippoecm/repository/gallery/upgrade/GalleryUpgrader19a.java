/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.gallery.upgrade;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryUpgrader19a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(GalleryUpgrader19a.class);

    @Override
    public void register(UpdaterContext updaterContext) {
        updaterContext.registerName("v19a-cms-gallery-updater");
        updaterContext.registerStartTag("v19-cms-gallery");
        updaterContext.registerEndTag("v19a-cms-gallery");

        registerNamespaceVisitor(updaterContext);
    }


    private void registerNamespaceVisitor(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (String initName : new String[]{
                        "hippogallery-editor", "hippogallery-editor-fr",
                }) {
                    if (node.hasNode(initName)) {
                        node.getNode(initName).remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("hippogallery")) {
                    node.getNode("hippogallery").remove();
                }
            }

        });
    }

}

