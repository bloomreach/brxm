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

import java.util.ArrayList;
import java.util.List;

public class EsvNode {

    private String name;
    private String type;
    private List<String> mixins = new ArrayList<>();
    private String uuid;
    private int index;
    private EsvMerge merge;
    private String mergeLocation;
    private List<EsvProperty> properties = new ArrayList<>();
    private List<EsvNode> children = new ArrayList<>();

    public EsvNode(final String name, final int index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public List<String> getMixins() {
        return mixins;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public int getIndex() {
        return index;
    }

    public EsvMerge getMerge() {
        return merge;
    }

    public void setMerge(final EsvMerge merge) {
        this.merge = merge;
    }

    public boolean isDeltaMerge() {
        return merge != null && (EsvMerge.COMBINE == merge || EsvMerge.OVERLAY == merge);
    }

    public boolean isDeltaSkip() {
        return merge != null && (EsvMerge.SKIP == merge);
    }

    public String getMergeLocation() {
        return mergeLocation;
    }

    public void setMergeLocation(final String mergeLocation) {
        this.mergeLocation = mergeLocation;
    }

    public List<EsvProperty> getProperties() {
        return properties;
    }

    public EsvProperty getProperty(final String name) {
        for (EsvProperty prop : properties) {
            if (prop.getName().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    public List<EsvNode> getChildren() {
        return children;
    }
}
