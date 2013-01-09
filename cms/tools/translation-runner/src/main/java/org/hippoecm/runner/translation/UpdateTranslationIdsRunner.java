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

public class UpdateTranslationIdsRunner extends LoggingPlugin {

    public static final int DEFAULT_BATCH_SIZE = 500;

    private int maxdo;

    private int counter = 0;

    @Override
    public void init() {
        super.init();
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
        getLogger().info("This running will create new translation ids for documents and folders.");
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
            path = node.getPath();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                NodeIterator siblings = node.getNodes(node.getName());
                String id = UUID.randomUUID().toString();
                while (siblings.hasNext()) {
                    Node sibling = siblings.nextNode();
                    if (sibling.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                        counter++;

                        if (!sibling.isCheckedOut()) {
                            sibling.checkout();
                        }
                        sibling.setProperty(HippoTranslationNodeType.ID, id);
                    }
                }
                node.save();
                node.getSession().refresh(false);

                getLogger().info("Translated document {}", node.getPath());
            }

            if (node.isNodeType(HippoNodeType.NT_DOCUMENT) && node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                Node parent = node.getParent();
                if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                    return;
                }

                counter++;
                if (!node.isCheckedOut()) {
                    node.checkout();
                }

                String id = UUID.randomUUID().toString();
                node.setProperty(HippoTranslationNodeType.ID, id);
                node.save();
                node.getSession().refresh(false);

                getLogger().info("Translated folder {}", node.getPath());
            }
        } catch (RepositoryException ex) {
            getLogger().error("Error setting language to node {}", path);
        }
        if (counter >= maxdo) {
//            JcrHelper.save();
//            JcrHelper.refresh(false);
            getLogger().info("Saving");
            counter = 0;
        }
    }

}
