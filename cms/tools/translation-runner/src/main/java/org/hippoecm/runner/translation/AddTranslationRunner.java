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
package org.hippoecm.runner.translation;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.forge.jcrrunner.plugins.LoggingPlugin;

public class AddTranslationRunner extends LoggingPlugin {

    public static final int DEFAULT_BATCH_SIZE = 500;

    private int maxdo;
    private String language;

    private int counter = 0;

    @Override
    public void init() {
        super.init();
        language = getConfigValue("language");
        String maxdoStr = getConfigValue("maxdo");
        if (maxdoStr != null && !"".equals(maxdoStr.trim())) {
            maxdo = Integer.valueOf(maxdoStr.trim());
            if (maxdo <= 0) {
                getLogger().warn("Invalid batch size '{}', falling back to default size {}", maxdoStr,
                        DEFAULT_BATCH_SIZE);
                maxdo = DEFAULT_BATCH_SIZE;
            }
        } else {
            maxdo = DEFAULT_BATCH_SIZE;
        }
        getLogger().info("This running will convert documents and folders to language {}.", language);
    }

    @Override
    public void destroy() {
//        JcrHelper.save();
        counter = 0;
        super.destroy();
    }

    @Override
    public void visit(Node node) {
        super.visit(node);

        String path = "";
        try {
            if (node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                counter++;

                path = node.getPath();
                if (!node.isCheckedOut()) {
                    node.checkout();
                }
                node.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
                node.setProperty(HippoTranslationNodeType.LOCALE, language);

                Node parent = node.getParent();
                String id = null;
                if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    NodeIterator siblings = parent.getNodes(parent.getName());
                    while (siblings.hasNext()) {
                        Node sibling = siblings.nextNode();
                        if (sibling.isNodeType(HippoTranslationNodeType.NT_TRANSLATED) && !sibling.isSame(node)) {
                            id = sibling.getProperty(HippoTranslationNodeType.ID).getString();
                            break;
                        }
                    }
                }
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                node.setProperty(HippoTranslationNodeType.ID, id);
                node.save();

                getLogger().info("Translated document {}", node.getPath());
            }
        } catch (RepositoryException ex) {
            getLogger().error("Error setting language to node {}", path);
        }
        if (counter >= maxdo) {
//            JcrHelper.save();
            getLogger().info("Saving");
            counter = 0;
        }
    }

}
