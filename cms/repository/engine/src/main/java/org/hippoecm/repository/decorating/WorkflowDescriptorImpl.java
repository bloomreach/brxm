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
package org.hippoecm.repository.decorating;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowDescriptor;

final class WorkflowDescriptorImpl implements WorkflowDescriptor {

    String nodeAbsPath;
    String category;
    protected String displayName;
    protected String rendererName;
    protected String serviceName;

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, Node node, Node item) throws RepositoryException {
        this.category = category;
        nodeAbsPath = item.getPath();
        try {
            try {
                serviceName = node.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                displayName = node.getProperty(HippoNodeType.HIPPO_DISPLAY).getString();
            } catch (PathNotFoundException ex) {
                manager.log.error("Workflow specification corrupt on node " + nodeAbsPath);
                throw new RepositoryException("workflow specification corrupt", ex);
            }
            try {
                rendererName = node.getProperty(HippoNodeType.HIPPO_RENDERER).getString();
            } catch (PathNotFoundException ex) {
            }
        } catch (ValueFormatException ex) {
            manager.log.error("Workflow specification corrupt on node " + nodeAbsPath);
            throw new RepositoryException("workflow specification corrupt", ex);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRendererName() {
        return rendererName;
    }

    public String toString() {
        return getClass().getName() + "[node=" + nodeAbsPath + ",category=" + category + ",service=" + serviceName
                + ",renderer=" + rendererName + "]";
    }
}
