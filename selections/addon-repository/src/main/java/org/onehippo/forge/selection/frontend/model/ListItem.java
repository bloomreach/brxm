/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

/**
 * List item object reflecting the list item in a value list document.
 *
 * @author Jeroen Hoffman
 * @author Dennis Dam
 */
public class ListItem implements Serializable {

    private static final long serialVersionUID = -7549187735023108422L;

    private final String key;
    private final String label;
    private final String group;

    /**
     * Constructor for this class
     * @param key the key of the list item
     * @param label the label of the list item
     */
    public ListItem(final String key, final String label) {
        this.key = key;
        this.label = label;
        this.group=null;
    }

    /**
     * Constructor for this class
     * @param key the key of the list item
     * @param label the label of the list item
     * @param group the group to which this item belongs
     */
    public ListItem(final String key, final String label, final String group) {
        this.key = key;
        this.label = label;
        this.group=group;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getGroup() {
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj){ return true;}
        if (obj == null){ return false;}
        if (getClass() != obj.getClass()){ return false;}
        ListItem other = (ListItem) obj;
        if (key == null) {
            if (other.key != null){ return false;}
        } else if (!key.equals(other.key)){ return false;}
        if (group == null){
            if (other.group != null){ return false;}
        } else if (!group.equals(other.group)){ return false; }
        return true;
    }

}
