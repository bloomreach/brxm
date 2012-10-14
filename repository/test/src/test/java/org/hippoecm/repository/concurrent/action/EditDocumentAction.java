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
package org.hippoecm.repository.concurrent.action;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;

public class EditDocumentAction extends AbstractFullReviewedActionsWorkflowAction {

    @Override
    protected String getWorkflowMethodName() {
        return "obtainEditableInstance";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Document document = getFullReviewedActionsWorkflow(node).obtainEditableInstance();
        node.getSession().refresh(false);
        Node draft = node.getSession().getNodeByUUID(document.getIdentity());
        String value = draft.getProperty("defaultcontent:introduction").getString();
        value += "x";
        draft.setProperty("defaultcontent:introduction", value);
        draft.getSession().save();
        Node handle = draft.getParent();
        document = getFullReviewedActionsWorkflow(draft).commitEditableInstance();
        node.getSession().refresh(false);
        String variantValue = null;
        String variantUUID = "null";
        for(NodeIterator iter = handle.getNodes(handle.getName()); iter.hasNext(); ) {
            Node variant = iter.nextNode();
            if(variant.getProperty("hippostd:state").getString().equals("unpublished")) {
                variantValue = variant.getProperty("defaultcontent:introduction").getString();
                variantUUID = variant.getUUID();
            }
        }
        if(variantValue != null && variantValue.length() < value.length()) {
            throw new RepositoryException("edit action failed "+Thread.currentThread().getName()+" document "+handle.getPath()+" "+variantUUID+" is "+(variantValue!=null?variantValue.length():-1)+" expected "+value.length());
        }

        node = node.getSession().getNodeByUUID(document.getIdentity());
        node = node.getParent();
        if (node.isNodeType("hippo:handle")) {
            node = node.getParent();
        }
        return node;
    }
}
