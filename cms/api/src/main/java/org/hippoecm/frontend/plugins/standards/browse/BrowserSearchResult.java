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
package org.hippoecm.frontend.plugins.standards.browse;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.query.QueryResult;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IChangeListener;

public final class BrowserSearchResult implements IDetachable {

    private static final long serialVersionUID = 1L;

    private List<IChangeListener> listeners = new ArrayList<IChangeListener>();

    private String queryName;
    private IModel<QueryResult> nodes;

    public BrowserSearchResult(String name, IModel<QueryResult> nodes) {
        this.queryName = name;
        this.nodes = nodes;
    }

    public String getQueryName() {
        return queryName;
    }

    public QueryResult getQueryResult() {
        return nodes.getObject();
    }

    public void addChangeListener(IChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeChangeListener(IChangeListener listener) {
        listeners.remove(listener);
    }
    
    public void detach() {
        nodes.detach();
    }

}
