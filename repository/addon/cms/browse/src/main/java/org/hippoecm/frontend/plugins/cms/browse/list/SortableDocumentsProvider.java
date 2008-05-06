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
package org.hippoecm.frontend.plugins.cms.browse.list;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortableDocumentsProvider extends SortableDataProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SortableDocumentsProvider.class);

    private List<JcrNodeModel> resources;

    public SortableDocumentsProvider(List<JcrNodeModel> resources) {
        setSort("name", true);
        this.resources = resources;
    }

    public Iterator<JcrNodeModel> iterator(int first, int count) {
        sortResources();
        List<JcrNodeModel> list = Collections.unmodifiableList(resources.subList(first, first + count));
        return list.iterator();
    }

    public IModel model(Object object) {
        return (JcrNodeModel) object;
    }

    public int size() {
        return resources.size();
    }

    private void sortResources() {
        
        JcrNodeModelComparator jcrNodeModelComparator = new JcrNodeModelComparator(getSort().getProperty());
        Collections.sort(resources, jcrNodeModelComparator);       
        if (getSort().isAscending() == false) {
            Collections.reverse(resources);
        }
    }

}
