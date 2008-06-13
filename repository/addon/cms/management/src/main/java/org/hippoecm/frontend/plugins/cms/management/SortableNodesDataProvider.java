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
package org.hippoecm.frontend.plugins.cms.management;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrNodeModelComparator;

public abstract class SortableNodesDataProvider extends SortableDataProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private List<JcrNodeModel> nodes;

    public SortableNodesDataProvider(String defaultSort) {
        setSort(defaultSort, true);
    }

    public Iterator<JcrNodeModel> iterator(int first, int count) {
        List<JcrNodeModel> list = Collections.unmodifiableList(getNodes().subList(first, first + count));
        return list.iterator();
    }

    public IModel model(Object object) {
        return (JcrNodeModel) object;
    }

    public int size() {
        return getNodes().size();
    }

    protected List<JcrNodeModel> getNodes() {
        if (nodes == null) {
            nodes = createNodes();
            sortNodes();
        }
        return nodes;
    }

    private void sortNodes() {
        JcrNodeModelComparator jcrNodeModelComparator = new JcrNodeModelComparator(getSort().getProperty());
        Collections.sort(nodes, jcrNodeModelComparator);
        if (getSort().isAscending() == false) {
            Collections.reverse(nodes);
        }
    }

    public void detach() {
        nodes = null;
    }

    protected abstract List<JcrNodeModel> createNodes();

}
