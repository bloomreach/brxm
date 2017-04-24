/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnpublishedReferenceNodeProvider implements ISortableDataProvider<Node, String> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UnpublishedReferenceNodeProvider.class);
    
    private Map<String, Node> referenced;
    private ISortState state = new SortState();

    public UnpublishedReferenceNodeProvider(Map<String, Node> referenced) {
        this.referenced = referenced;
    }

    @Override
    public Iterator<? extends Node> iterator(long first, long count) {
        if (first < referenced.size()) {
            if ((first + count) <= referenced.size()) {
                return new ArrayList<>(referenced.values()).subList((int) first, (int) (first + count)).iterator();
            } else {
                return new ArrayList<>(referenced.values()).subList((int) first, referenced.size()).iterator();
            }
        }
        return Collections.<Node>emptyList().iterator();
    }

    @Override
    public IModel<Node> model(Node object) {
        return new JcrNodeModel(object);
    }

    @Override
    public long size() {
        return referenced.size();
    }

    public void detach() {
    }

    public ISortState getSortState() {
        return state;
    }

    public void setSortState(ISortState state) {
        this.state = state;
    }
}
