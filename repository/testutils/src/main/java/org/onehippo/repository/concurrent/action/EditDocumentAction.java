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
package org.onehippo.repository.concurrent.action;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class EditDocumentAction extends AbstractDocumentWorkflowAction {

    public EditDocumentAction(final ActionContext context) {
        super(context);
    }

    @Override
    protected String getWorkflowMethodName() {
        return "obtainEditableInstance";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Node handle = node.getParent();
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        Document document = documentWorkflow.obtainEditableInstance();
        Node draft = document.getNode(node.getSession());
        String value = draft.getProperty("testcontent:introduction").getString();
        value += "x";
        draft.setProperty("testcontent:introduction", value);
        draft.getSession().save();
        document = documentWorkflow.commitEditableInstance();
        String variantValue = null;
        String variantUUID = "null";
        for(NodeIterator iter = handle.getNodes(handle.getName()); iter.hasNext(); ) {
            Node variant = iter.nextNode();
            if(variant.getProperty("hippostd:state").getString().equals("unpublished")) {
                variantValue = variant.getProperty("testcontent:introduction").getString();
                variantUUID = variant.getIdentifier();
                break;
            }
        }
        if(variantValue == null || variantValue.length() < value.length()) {
            throw new RepositoryException("edit action failed "+Thread.currentThread().getName()+" document "+handle.getPath()+" "+variantUUID+" is "+(variantValue!=null?variantValue.length():-1)+" expected "+value.length());
        }

        node = document.getNode(node.getSession());
        node = node.getParent();
        if (node.isNodeType("hippo:handle")) {
            node = node.getParent();
        }
        return node;
    }
}
