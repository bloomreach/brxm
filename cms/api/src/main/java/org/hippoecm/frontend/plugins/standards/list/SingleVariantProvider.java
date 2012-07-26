/*
 *  Copyright 2010 Hippo.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load a single representative for each set of variants of a document.
 * Different documents can share a name, so the comparison is based on the
 * handle that contains the variants.  The first document (lowest sns index
 * in resultset) is used. 
 */
public class SingleVariantProvider extends LoadableDetachableModel<List<Node>> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SingleVariantProvider.class);

    private IModel<Iterator> documents;

    public SingleVariantProvider(IModel<Iterator> documents) {
        this.documents = documents;
    }

    @Override
    protected List<Node> load() {
        Map<String, Node> primaryNodes = new HashMap<String, Node>();
        Iterator subNodes = documents.getObject();
        // workaround: node may disappear without notification
        if (subNodes != null) {
            try {
                while (subNodes.hasNext()) {
                    Node subNode = (Node) subNodes.next();
                    if (subNode == null || !(subNode instanceof HippoNode)) {
                        continue;
                    }
                    try {
                        Node canonicalNode = ((HippoNode) subNode).getCanonicalNode();
                        if (canonicalNode == null) {
                            // no physical equivalent exists
                            continue;
                        }
                        if (canonicalNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            Node parentNode = canonicalNode.getParent();
                            if (parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                                if (parentNode.isNodeType("mix:referenceable")) {
                                    if (primaryNodes.containsKey(parentNode.getUUID())) {
                                        Node currentNode = primaryNodes.get(parentNode.getUUID());
                                        if (subNode.getIndex() < currentNode.getIndex()) {
                                            primaryNodes.put(parentNode.getUUID(), subNode);
                                        }
                                    } else {
                                        primaryNodes.put(parentNode.getUUID(), subNode);
                                    }
                                } else {
                                    log.info("Skipping unreferenceable canonical handle " + parentNode.getPath());
                                }
                            }
                        }
                    } catch (ItemNotFoundException ex) {
                        // physical item no longer exists
                        continue;
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return new ArrayList<Node>(primaryNodes.values());
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        documents.detach();
    }

}
