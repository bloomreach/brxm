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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnpublishedReferenceNodeProvider implements ISortableDataProvider<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UnpublishedReferenceNodeProvider.class);
    
    private final UnpublishedReferenceProvider referenced;

    public UnpublishedReferenceNodeProvider(UnpublishedReferenceProvider referenced) {
        this.referenced = referenced;
    }

    public Iterator<? extends Node> iterator(int first, int count) {
        final Iterator<String> upstream = referenced.iterator(first, count);
        return new Iterator<Node>() {

            public boolean hasNext() {
                return upstream.hasNext();
            }

            public Node next() {
                String uuid = upstream.next();
                javax.jcr.Session session = UserSession.get().getJcrSession();
                try {
                    return session.getNodeByIdentifier(uuid);
                } catch (ItemNotFoundException e) {
                    log.info("Document {} has a reference to non-existing UUID {}", referenced.getDocumentPath(), uuid);
                } catch (RepositoryException e) {
                    log.error(e.getMessage(), e);
                }
                return null;
            }

            public void remove() {
                upstream.remove();
            }
            
        };
    }

    public IModel<Node> model(Node object) {
        return new JcrNodeModel(object);
    }

    public int size() {
        return referenced.size();
    }

    public void detach() {
        referenced.detach();
    }

    public ISortState getSortState() {
        return referenced.getSortState();
    }

    public void setSortState(ISortState state) {
        referenced.setSortState(state);
    }
}
