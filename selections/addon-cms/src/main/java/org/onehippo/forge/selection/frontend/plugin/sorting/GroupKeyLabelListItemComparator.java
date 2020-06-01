/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Comparator;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.plugin.Config;

/*
 * @version $Id$
 * ListItemComparator that sort alphanumerically based on config settings. First the group is used to sort, if
 * the group is identical, the keys are used and finally if the keys might be the same, the labels are used.
 */
public class GroupKeyLabelListItemComparator extends AbstractListItemComparator {


    private SortOrder sortOrder;




    @Override
    public void setConfig(IPluginConfig config) {
        super.setConfig(config);
        final String order = getConfig().getString(Config.SORT_ORDER);
        sortOrder = (order != null) ? SortOrder.valueOf(order) : null;
    }


    public int compare(final ListItem listItem1, final ListItem listItem2) {
        String key1 = listItem1.getKey();
        String key2 = listItem2.getKey();
        String label1 = listItem1.getLabel();
        String label2 = listItem2.getLabel();
        String group1 = listItem1.getGroup();
        String group2 = listItem2.getGroup();

        int compare;

        // compare on group if groups aren't equal, then on keys if keys
        // aren't equal and finally on label.
        if ((group1 != null) && (!group1.equals(group2)) && (group2 != null)) {
            compare = group1.compareTo(group2);
        } else if ((key1 != null) && (!key1.equals(key2)) && (key2 != null)) {
            compare = key1.compareTo(key2);
        } else {
            compare = label1.compareTo(label2);
        }

        if ((sortOrder != null) && (sortOrder == SortOrder.descending)) {
            return -1 * compare;
        } else {
            return compare;
        }
    }


}
