/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.model.content;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Document extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Document.class);

    public Document(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public String getName() {
        try {
            return nodeModel.getNode().getDisplayName();
        } catch (RepositoryException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    public List<DocumentVariant> getVariants() {
        List<DocumentVariant> list = new ArrayList<DocumentVariant>();
        try {
            for (NodeIterator iter = nodeModel.getNode().getNodes(); iter.hasNext();) {
                HippoNode docNode = (HippoNode) iter.next();
                if (docNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    list.add(new DocumentVariant(new JcrNodeModel(docNode)));
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return list;
    }
}
