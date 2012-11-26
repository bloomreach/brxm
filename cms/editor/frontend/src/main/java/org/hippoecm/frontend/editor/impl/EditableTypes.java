/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.impl;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.hippoecm.editor.EditorNodeType;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

class EditableTypes extends AbstractList implements Serializable, IObservable {

    private static final long serialVersionUID = 1L;

    private IObservationContext obContext;
    private JcrEventListener listener;
    private List<String> entries;

    EditableTypes() {
        entries = load();
    }

    List<String> load() {
        List<String> editableTypes = new ArrayList<String>();
        javax.jcr.Session session = UserSession.get().getJcrSession();
        try {
            QueryManager qMgr = session.getWorkspace().getQueryManager();
            Query query = qMgr.createQuery("//element(*, " + EditorNodeType.NT_EDITABLE + ")", Query.XPATH);
            NodeIterator iter = query.execute().getNodes();
            Set<String> types = new TreeSet<String>();
            while (iter.hasNext()) {
                Node ttNode = iter.nextNode();
                TemplateEngine.log.debug("search result: {}", ttNode.getPath());

                // verify that parent is of correct type
                Node nsNode = ttNode.getParent();
                if (!nsNode.isNodeType(HippoNodeType.NT_NAMESPACE)) {
                    continue;
                }

                String name;
                if ("system".equals(nsNode.getName())) {
                    name = ttNode.getName();
                } else if ("hippo".equals(nsNode.getName())) {
                    name = "hippo:" + ttNode.getName();
                } else {
                    name = nsNode.getName() + ":" + ttNode.getName();

                    String namespace;
                    try {
                        namespace = nsNode.getSession().getNamespaceURI(nsNode.getName());
                    } catch (NamespaceException ex) {
                        continue;
                    }
                    Node ntNode = null;
                    Node ntVersions = ttNode.getNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE);
                    NodeIterator ntVersionIter = ntVersions.getNodes();
                    while (ntVersionIter.hasNext()) {
                        Node ntVersion = ntVersionIter.nextNode();
                        if (ntVersion.isNodeType(HippoNodeType.NT_REMODEL)) {
                            if (ntVersion.getProperty(HippoNodeType.HIPPO_URI).getString().equals(namespace)) {
                                ntNode = ntVersion;
                            }
                        }
                    }
                    if (ntNode == null) {
                        continue;
                    }
                }

                types.add(name);
            }
            editableTypes.addAll(types);
        } catch (RepositoryException ex) {
            TemplateEngine.log.error("Unable to enumerate editable types", ex);
        }
        return editableTypes;
    }

    @Override
    public Object get(int index) {
        return entries.get(index);
    }

    @Override
    public int size() {
        return entries.size();
    }

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    public void startObservation() {
        listener = new JcrEventListener(obContext, Event.NODE_ADDED | Event.NODE_REMOVED, "/", true, null,
                new String[] { HippoNodeType.NT_NAMESPACE }) {
            @Override
            public void onEvent(EventIterator events) {
                EventCollection<IEvent<IObservable>> collection = new EventCollection<IEvent<IObservable>>();
                collection.add(new IEvent<IObservable>() {

                    public IObservable getSource() {
                        return EditableTypes.this;
                    }
                });
                List<String> oldEntries = entries;
                entries = load();
                for (String newEntry : entries) {
                    if (!oldEntries.contains(newEntry)) {
                        obContext.notifyObservers(collection);
                        return;
                    }
                }
                for (String oldEntry : oldEntries) {
                    if (!entries.contains(oldEntry)) {
                        obContext.notifyObservers(collection);
                        return;
                    }
                }
            }
        };
        listener.start();
    }

    public void stopObservation() {
        listener.stop();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof EditableTypes);
    }

    @Override
    public int hashCode() {
        return 345997;
    }

}
