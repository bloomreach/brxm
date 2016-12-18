/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

public enum EsvMerge {

    APPEND(false, true),
    COMBINE(true, false),
    INSERT(true, true),
    OVERLAY(true, false),
    OVERRIDE(false, true),
    SKIP(true, true);

    private final boolean forNode;
    private final boolean forProperty;
    private final String  name;

    public static EsvMerge lookup(String name) {
        for (EsvMerge merge : values()) {
            if (merge.name.equals(name)) {
                return merge;
            }
        }
        return null;
    }

    EsvMerge(final boolean forNode, final boolean forProperty) {
        this.forNode = forNode;
        this.forProperty = forProperty;
        this.name = name().toLowerCase();
    }

    public boolean isForNode() {
        return forNode;
    }

    public boolean isForProperty() {
        return forProperty;
    }

    public String toString() {
        return name;
    }
}
