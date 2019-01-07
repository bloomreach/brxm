/*
 * Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.selection.frontend.plugin.sorting;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.plugin.Config;

/**
 * List item comparator that sorts alphanumerically based of config settings.
 */
public class DefaultListItemComparator extends AbstractListItemComparator {

    private IFieldProvider fieldProvider;
    private Comparator<Comparable> fieldComparator;

    @Override
    public void setConfig(IPluginConfig config) {

        super.setConfig(config);

        createFieldComparator();
        createFieldProvider();
    }

    /**
     * Create a field provider
     */
    protected void createFieldProvider() {
        final String by = getConfig().getString(Config.SORT_BY);
        final SortBy sortBy = StringUtils.isNotBlank(by) ? SortBy.valueOf(by) : null;
        if (SortBy.key == sortBy) {
            fieldProvider = new KeyFieldProvider();
        }
        else{
            fieldProvider = new LabelFieldProvider();
        }
    }

    /**
     * Create a field comparator.
     */
    protected void createFieldComparator() {
        final String order = getConfig().getString(Config.SORT_ORDER);
        final SortOrder sortOrder = StringUtils.isNotBlank(order) ? SortOrder.valueOf(order) : null;
        if (SortOrder.descending == sortOrder) {
            fieldComparator = new DescendingComparator();
        }
        else {
            fieldComparator = new AscendingComparator();
        }
    }

    protected void setFieldProvider(IFieldProvider fieldProvider){
        this.fieldProvider = fieldProvider;
    }

    protected void setFieldComparator(Comparator<Comparable> fieldComparator){
        this.fieldComparator = fieldComparator;
    }

    // javadoc from interface
    public int compare(final ListItem listItem1, final ListItem listItem2) {
        return fieldComparator.compare(fieldProvider.getField(listItem1), fieldProvider.getField(listItem2));

    }

    public class AscendingComparator implements Comparator<Comparable>, Serializable {
        public int compare(final Comparable o1, final Comparable o2) {
            return o1.compareTo(o2);
        }
    }

    public class DescendingComparator implements Comparator<Comparable>, Serializable {
        public int compare(final Comparable o1, final Comparable o2) {
            return o2.compareTo(o1);
        }
    }

    public class KeyFieldProvider implements IFieldProvider {
        public String getField(final ListItem item) {
            return item.getKey();
        }
    }

    public class LabelFieldProvider implements IFieldProvider {
        public String getField(final ListItem item) {
            return item.getLabel();
        }
    }
}
