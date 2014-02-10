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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnpublishedReferenceProvider implements ISortableDataProvider<String, String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnpublishedReferenceProvider.class);

    private ReferenceProvider wrapped;
    private ISortState state = new SortState();
    private transient List<String> entries;

    public UnpublishedReferenceProvider(ReferenceProvider provider) {
        this.wrapped = provider;
    }

    public String getDocumentPath() {
        return wrapped.getNodeModel().getItemModel().getPath();
    }

    @Override
    public Iterator<String> iterator(long first, long count) {
        load();
        if (first < entries.size()) {
            if ((first + count) <= entries.size()) {
                return entries.subList((int) first, (int) (first + count)).iterator();
            } else {
                return entries.subList((int) first, entries.size()).iterator();
            }
        }
        return Collections.<String>emptyList().iterator();
    }

    @Override
    public IModel<String> model(String object) {
        return new Model<String>(object);
    }

    @Override
    public long size() {
        load();
        return entries.size();
    }

    @Override
    public void detach() {
        wrapped.detach();
    }

    public ISortState getSortState() {
        return state;
    }

    public void setSortState(ISortState state) {
        this.state = state;
    }

    protected void load() {
        if (entries == null) {
            entries = new ArrayList<>();
            Session session = UserSession.get().getJcrSession();
            Iterator<String> upstream = wrapped.iterator(0, wrapped.size());
            try {
                while (upstream.hasNext()) {
                    String uuid = upstream.next();
                    try {
                        Node node = session.getNodeByIdentifier(uuid);
                        boolean valid = true;
                        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                            valid = false;
                            for (Node document : new NodeIterable(node.getNodes(node.getName()))) {
                                if (isLive(document)) {
                                    valid = true;
                                    break;
                                }
                            }
                        }
                        if (!valid) {
                            entries.add(uuid);
                        }
                    } catch (ItemNotFoundException ex) {
                        log.debug("Reference to UUID " + uuid + " could not be dereferenced.");
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private boolean isLive(Node document) throws RepositoryException {
        final Property property = JcrUtils.getPropertyIfExists(document, HippoNodeType.HIPPO_AVAILABILITY);
        if (property != null) {
            for (Value value : property.getValues()) {
                if ("live".equals(value.getString())) {
                    return true;
                }
            }
        }
        return false;
    }

}
