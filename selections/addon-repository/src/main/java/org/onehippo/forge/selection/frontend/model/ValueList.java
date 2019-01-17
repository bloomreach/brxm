/*
 * Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.selection.frontend.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Value list object, i.e. an array list with ListItem objects, reflecting a value list document.
 *
 * @author Jeroen Hoffman
 * @author Dennis Dam
 */
public class ValueList extends ArrayList<ListItem> implements Serializable {

    /**
     * Constructor.
     */
    public ValueList() {
        super();
    }

    /**
     * The index of the specified object in this list.
     * <p>
     * The object can be either a listitem or a string. If it is a string, then it represents the key of a list item.
     *
     * @param element a listitem or a string
     * @return index of a list item in the list
     */
    @Override
    public int indexOf(final Object element) {
        if (element instanceof ListItem) {
            return super.indexOf(element);
        }

        // string value is considered as the key
        if (element instanceof String) {
            for (int i = 0; i < size(); i++) {
                if (get(i).getKey().equals(element)) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Returns the key of the list item associated with the provided object. The object can be either a listitem or a
     * string. If it is a string, then it represents the key of a list item.
     *
     * @param object a listitem or a string
     * @return the key associated with the listitem/string
     */
    public String getKey(final Object object) {

        if (object instanceof ListItem) {
            return ((ListItem) object).getKey();
        }

        // string value is considered as the key itself
        if (object instanceof String) {
            return (String) object;
        }

        return null;
    }

    /**
     * Returns the label of the list item associated with the provided object. The object can be either a listitem or a
     * string. If it is a string, then it represents the key of a list item.
     *
     * @param object a listitem or a string
     * @return the label associated with the listitem/string
     */
    public String getLabel(final Object object) {

        if (object instanceof ListItem) {
            return ((ListItem) object).getLabel();
        }

        // string value is considered as the key
        if (object instanceof String) {
            for (ListItem listItem : this) {
                if (listItem.getKey().equals(object)) {
                    return listItem.getLabel();
                }
            }
        }

        return null;
    }

    /**
     * Looks up a listitem by matching the given key.
     *
     * @param key the key of a listitem
     * @return the listitem of which the key is equal to the given key
     */
    public ListItem getListItemByKey(final String key) {
        for (ListItem valueListItem : this) {
            if (valueListItem.getKey().equals(key)) {
                return valueListItem;
            }
        }
        return null;
    }

}
