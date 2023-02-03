/*
 * Copyright 2012-2023 Bloomreach
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.concurrent.action;

import java.util.Random;

import javax.jcr.Node;

public class RenameDocumentAction extends AbstractDocumentWorkflowAction {

    private final Random random = new Random(System.currentTimeMillis());

    public RenameDocumentAction(final ActionContext context) {
        super(context);
    }

    @Override
    protected String getWorkflowMethodName() {
        return "rename";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Node handle = node.getParent();
        Node folder = handle.getParent();
        String newName = node.getName();
        do {
            newName += "." + random.nextInt(10);
        } while (folder.hasNode(newName));
        getDocumentWorkflow(handle).rename(newName);
        return folder.getNode(newName).getNode(newName);
    }
}
