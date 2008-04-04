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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.TreeSet;
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

    private class Entry {
        String name;
        int order;
        Entry(String name, int order) {
            this.name = name;
            this.order = order;
        }
        Entry(String name) {
            this.name = name;
            order = -1;
        }
    }

    List<String> categories;

    transient private Map<Entry,Vector<WorkflowDescriptor>> workflows;

    public void initialize() throws RepositoryException {
        workflows = new TreeMap<Entry,Vector<WorkflowDescriptor>>(new Comparator<Entry>() {
            public int compare(Entry o1, Entry o2) {
                return o1.name.compareTo(o2.name);
            }
            public boolean equals(Entry o1, Entry o2) {
                return o1.name.equals(o2.name);
            }
        });

        Node handle = getNodeModel().getNode();
        WorkflowManager manager = ((HippoWorkspace) handle.getSession().getWorkspace()).getWorkflowManager();

        int sequence = 0;

        for(String category : categories) {
            if(handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                for(NodeIterator iter = handle.getNodes(); iter.hasNext(); ) {
                    Node child = iter.nextNode();
                    if(child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, child);
                        if(workflowDescriptor != null) {
                            if(!workflows.containsKey(new Entry(workflowDescriptor.getRendererName())))
                                workflows.put(new Entry(workflowDescriptor.getRendererName(), sequence++), new Vector());
                            workflows.get(new Entry(workflowDescriptor.getRendererName())).add(workflowDescriptor);
                        }
                    }
                }
            } else if(handle.isNodeType("hippo:prototyped") || handle.isNodeType("hippo:templatetype") ||
                      handle.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, handle);
                if(workflowDescriptor != null) {
                    if(!workflows.containsKey(new Entry(workflowDescriptor.getRendererName())))
                        workflows.put(new Entry(workflowDescriptor.getRendererName(),sequence++), new Vector());
                    workflows.get(new Entry(workflowDescriptor.getRendererName())).add(workflowDescriptor);
                }
            }
        }
        for(String category : categories) {
            if(handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                for(NodeIterator iter = handle.getNodes(); iter.hasNext(); ) {
                    Node child = iter.nextNode();
                    if(child.isNodeType(HippoNodeType.NT_REQUEST)) {
                        WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, child);
                        if(workflowDescriptor != null) {
                            if(!workflows.containsKey(new Entry(workflowDescriptor.getRendererName())))
                                workflows.put(new Entry(workflowDescriptor.getRendererName(), sequence++), new Vector());
                            workflows.get(new Entry(workflowDescriptor.getRendererName())).add(workflowDescriptor);
                        }
                    }
                }
            } else if(handle.isNodeType(HippoNodeType.NT_REQUEST)) {
                WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, handle);
                if(workflowDescriptor != null) {
                    if(!workflows.containsKey(new Entry(workflowDescriptor.getRendererName())))
                        workflows.put(new Entry(workflowDescriptor.getRendererName(),sequence++), new Vector());
                    workflows.get(new Entry(workflowDescriptor.getRendererName())).add(workflowDescriptor);
                }
            }
        }
    }

    public WorkflowsModel(JcrNodeModel model, List<String> categories) throws RepositoryException {
        super(model);
        this.categories = new LinkedList<String>();
        this.categories.addAll(categories);
        initialize();
    }

    private WorkflowsModel(WorkflowsModel model, List<String> categories, String renderer) throws RepositoryException {
        super(model.getNodeModel());
        this.categories = new LinkedList<String>();
        // deliberate before adding categories
        initialize();
        this.categories.addAll(categories);
        workflows.put(new Entry(renderer,0), model.workflows.get(new Entry(renderer)));
    }

    @Override
    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = super.getMapRepresentation();
        map.put("workflows", workflows);
        return map;
    }

    public String getWorkflowName() {
        try {
            if(workflows == null)
                initialize();
            Iterator iter = workflows.keySet().iterator();
            if(iter.hasNext()) {
                return ((Entry)iter.next()).name;
            }
        } catch(RepositoryException ex) {
            // FIXME
        }
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor() {
        try {
            if(workflows == null)
                initialize();
            Iterator<Vector<WorkflowDescriptor>> iter = workflows.values().iterator();
            if(iter.hasNext()) {
                Vector<WorkflowDescriptor> descriptors = iter.next();
                if(descriptors.size() > 0)
                    return descriptors.get(0);
            }
        } catch(RepositoryException ex) {
            // FIXME
        }
        return null;
    }

    public Iterator iterator(int first, final int count) {
        try {
            if(workflows == null)
                initialize();
        } catch(RepositoryException ex) {
            // FIXME
            return null;
        }

        TreeSet sortedWorkflows = new TreeSet<Entry>(new Comparator<Entry>() {
            public int compare(Entry o1, Entry o2) {
                return o1.order - o2.order;
            }
            public boolean equals(Entry o1, Entry o2) {
                return o1.order == o2.order;
            }
        });
        sortedWorkflows.addAll(workflows.keySet());
        final Iterator<Entry> renderers = sortedWorkflows.iterator();

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
                    try {
                        return new WorkflowsModel(WorkflowsModel.this, categories, renderers.next().name);
                    } catch(RepositoryException ex) {
                        // FIXME
                        throw new NoSuchElementException();
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
        };
    }

    public int size() {
        try {
            if(workflows == null)
                initialize();
            return workflows.keySet().size();
        } catch(RepositoryException ex) {
            // FIXME
            return 0;
        }
    }

    public IModel model(Object object) {
        return (IModel) object;
    }
}
