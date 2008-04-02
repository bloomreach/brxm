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
package org.hippoecm.frontend.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowsModel extends NodeModelWrapper implements IDataProvider {
    private static final long serialVersionUID = 1L;

    transient private Map<String,Vector<WorkflowDescriptor>> workflows;

    public WorkflowsModel(JcrNodeModel model, List<String> categories) throws RepositoryException {
        super(model);
        workflows = new TreeMap<String,Vector<WorkflowDescriptor>>();

        Node handle = model.getNode();
        WorkflowManager manager = ((HippoWorkspace) handle.getSession().getWorkspace()).getWorkflowManager();

        for(String category : categories) {
            if(handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                for(NodeIterator iter = handle.getNodes(); iter.hasNext(); ) {
                    Node child = iter.nextNode();
                    if(child.isNodeType(HippoNodeType.NT_DOCUMENT) || child.isNodeType(HippoNodeType.NT_REQUEST)) {
                        WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, child);
                        if(workflowDescriptor != null) {
                            if(!workflows.containsKey(workflowDescriptor.getRendererName()))
                                workflows.put(workflowDescriptor.getRendererName(), new Vector());
                            workflows.get(workflowDescriptor.getRendererName()).add(workflowDescriptor);
                        }
                    }
                }
            } else if(handle.isNodeType("hippo:prototyped") || handle.isNodeType("hippo:templatetype") ||
                      handle.isNodeType(HippoNodeType.NT_DOCUMENT) || handle.isNodeType(HippoNodeType.NT_REQUEST)) {
                WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, handle);
                if(workflowDescriptor != null) {
                    if(!workflows.containsKey(workflowDescriptor.getRendererName()))
                        workflows.put(workflowDescriptor.getRendererName(), new Vector());
                    workflows.get(workflowDescriptor.getRendererName()).add(workflowDescriptor);
                }
            }
        }
    }

    public WorkflowsModel(WorkflowsModel model, String renderer) {
        super(model.getNodeModel());
        workflows = new TreeMap<String,Vector<WorkflowDescriptor>>();

        workflows.put(renderer, model.workflows.get(renderer));
    }

    @Override
    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = super.getMapRepresentation();
        map.put("workflows", workflows);
        return map;
    }

    public String getWorkflowName() {
        Iterator iter = workflows.keySet().iterator();
        if(iter.hasNext()) {
            return (String) iter.next();
        }
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor() {
        Iterator<Vector<WorkflowDescriptor>> iter = workflows.values().iterator();
        if(iter.hasNext()) {
            Vector<WorkflowDescriptor> descriptors = iter.next();
            if(descriptors.size() > 0)
                return descriptors.get(0);
        }
        return null;
    }

    public Iterator iterator(int first, final int count) {
        final Iterator<String> renderers = workflows.keySet().iterator();
        while(first > 0 && renderers.hasNext()) {
            --first;
            renderers.next();
        }
        return new Iterator() {
                int remaining = count;
                public boolean hasNext() {
                    if(remaining == 0)
                        return false;
                    return renderers.hasNext();
                }
                public WorkflowsModel next() {
                    if(remaining == 0)
                        throw new NoSuchElementException();
                    --remaining;
                    return new WorkflowsModel(WorkflowsModel.this, renderers.next());
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
        };
    }

    public int size() {
        return workflows.keySet().size();
    }

    public IModel model(Object object) {
        return (IModel) object;
    }
}
