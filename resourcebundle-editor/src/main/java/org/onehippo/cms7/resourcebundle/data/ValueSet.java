/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.resourcebundle.data;

import java.io.Serializable;

/**
 * A value set represents a (often language dependent) set of resource bundle values. The
 * values themselves are not stored here (other than being available through the property),
 * but in the resource's valueMap.
 */
public class ValueSet implements Comparable<ValueSet>, Serializable {

    private Bundle bundle;
    private String name;
    private String displayName;

    ValueSet(Bundle bundle, String name, String defaultDisplayName) {
        this.bundle = bundle;
        this.name = name;

        if (name.equals(Bundle.PROP_VALUES_PREFIX)) {
            displayName = defaultDisplayName;
        } else if (name.startsWith(Bundle.PROP_VALUES_PREFIX)) {
            displayName = name.substring(Bundle.PROP_VALUES_PREFIX.length() + 1);
        } else {
            displayName = name; // unrecognized pattern, don't invent display name.
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String makeName() {
        return Bundle.PROP_VALUES_PREFIX + "_" + displayName;
    }

    public int compareTo(ValueSet other) {
        return displayName.compareTo(other.getDisplayName());
    }
}
