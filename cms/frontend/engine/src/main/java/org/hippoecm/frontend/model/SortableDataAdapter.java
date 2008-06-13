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
package org.hippoecm.frontend.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortableDataAdapter<T extends IModel> extends SortableDataProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SortableDataAdapter.class);

    private IDataProvider provider;
    private Map<String, Comparator<? super T>> comparators;
    private transient List<T> resources;

    public SortableDataAdapter(IDataProvider provider, Map<String, Comparator<? super T>> comparators) {
        this.provider = provider;
        this.comparators = comparators;
        resources = null;
    }

    public IDataProvider getDataProvider() {
        return provider;
    }

    public Iterator<T> iterator(int first, int count) {
        sortResources();
        return Collections.unmodifiableList(resources.subList(first, first + count)).iterator();
    }

    public T model(Object object) {
        return (T) object;
    }

    public int size() {
        return provider.size();
    }

    private void sortResources() {
        resources = new ArrayList<T>(provider.size());
        Iterator<T> iter = provider.iterator(0, provider.size());
        while (iter.hasNext()) {
            resources.add(iter.next());
        }

        Comparator<? super T> comparator = comparators.get(getSort().getProperty());
        Collections.sort(resources, comparator);
        if (getSort().isAscending() == false) {
            Collections.reverse(resources);
        }
    }

}
